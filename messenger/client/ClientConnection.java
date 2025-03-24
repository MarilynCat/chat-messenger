package client;

import server.*;
import server.packets.RequestUserListPacket; // ✅ Добавлен импорт для RequestUserListPacket

import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientConnection extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final LinkedBlockingQueue<Packet> toServerQueue = new LinkedBlockingQueue<>();
    private Thread writerThread;
    private MessageListener messageListener;

    private Correspondent correspondent;

    public int getCurrentUserId() {
        if (correspondent != null) {
            return correspondent.getId();
        } else {
            System.out.println("❗️ [ClientConnection] Ошибка: correspondent не инициализирован.");
            return -1;
        }
    }

    public void setCorrespondent(Correspondent correspondent) {
        this.correspondent = correspondent;
    }

    public interface MessageListener {
        void onPacketReceived(Packet packet);
    }

    public ClientConnection(String host, int port, MessageListener listener) throws IOException {
        this.socket = new Socket(host, port);
        this.messageListener = listener;

        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

        writerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Packet packet = toServerQueue.take();

                    if (socket.isClosed()) {
                        System.out.println("❗️ [ClientConnection] Попытка отправить пакет через закрытое соединение.");
                        continue;
                    }

                    System.out.println("📤 [ClientConnection] Пакет перед отправкой: " + packet.getType());
                    packet.writePacket(writer);
                    writer.flush();
                    if (writer.checkError()) {
                        System.out.println("❗️ [ClientConnection] Ошибка при отправке данных. Возможно, сервер закрыл соединение.");
                    } else {
                        System.out.println("✅ [ClientConnection] Пакет успешно отправлен: " + packet.getType());
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("🛑 [ClientConnection] Поток записи прерван.");
            } catch (Exception ex) {
                System.out.println("❌ [ClientConnection] Ошибка при отправке пакета: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                close();
            }
        });

        writerThread.start();
    }

    public void sendPacket(Packet packet) {
        if (socket.isClosed() || !socket.isConnected()) {
            System.out.println("❗️ [ClientConnection] Пакет не отправлен, соединение закрыто.");
            return;
        }

        try {
            toServerQueue.add(packet);
        } catch (Exception e) {
            System.out.println("❌ [ClientConnection] Ошибка при добавлении пакета в очередь: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            int retryCount = 0;
            final int MAX_RETRIES = 50;
            final int RETRY_DELAY_MS = 100;

            while (!socket.isClosed() && retryCount < MAX_RETRIES) {
                System.out.println("🔎 [ClientConnection] Ожидание пакета от сервера...");

                if (!reader.ready()) {
                    retryCount++;
                    System.out.println("⏳ [ClientConnection] Поток данных не готов к чтению. Попытка " + retryCount);
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }

                Packet packet = Packet.readPacket(reader);

                if (packet == null) {
                    System.out.println("❗️ [ClientConnection] Пакет пустой или некорректный, попытка " + retryCount);
                    retryCount++;
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }

                retryCount = 0;

                if (packet instanceof WelcomePacket) {
                    System.out.println("✅ [ClientConnection] Успешная авторизация. Показ списка пользователей.");
                    requestUserList();
                }

                if (packet instanceof ListPacket listPacket) {
                    System.out.println("✅ [ClientConnection] Получен ListPacket с количеством пользователей: " + listPacket.items.size());
                    if (messageListener != null) {
                        messageListener.onPacketReceived(listPacket);
                    } else {
                        System.out.println("❗️ [ClientConnection] MessageListener не установлен! Список пользователей может не отобразиться.");
                    }
                }

                if (packet instanceof ErrorPacket) {
                    System.out.println("❌ [ClientConnection] Ошибка авторизации: " + ((ErrorPacket) packet).getMessage());
                    close();
                    break;
                }

                if (messageListener != null) {
                    messageListener.onPacketReceived(packet);
                }
            }

            if (retryCount >= MAX_RETRIES) {
                System.out.println("🛑 [ClientConnection] Превышено количество попыток. Закрываем соединение.");
                close();
            }

        } catch (Exception e) {
            System.out.println("❌ [ClientConnection] Ошибка при чтении пакета: " + e.getMessage());
            e.printStackTrace();
        } finally {
            close();
        }
    }

    private void requestUserList() {
        System.out.println("📋 [ClientConnection] Запрос списка пользователей...");
        RequestUserListPacket requestUserListPacket = new RequestUserListPacket();
        sendPacket(requestUserListPacket);
    }

    public void close() {
        try {
            System.out.println("🛑 [ClientConnection] Закрытие подключения...");

            if (writerThread != null) {
                writerThread.interrupt();
                try {
                    writerThread.join(1000);
                } catch (InterruptedException e) {
                    System.out.println("❗️ [ClientConnection] Поток был прерван во время ожидания завершения.");
                    Thread.currentThread().interrupt();
                }
            }

            if (socket != null && !socket.isClosed()) {
                socket.shutdownOutput();
                socket.close();
            }
        } catch (Exception ex) {
            System.out.println("❌ [ClientConnection] Ошибка при закрытии соединения: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
