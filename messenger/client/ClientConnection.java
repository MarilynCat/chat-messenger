package client;

import server.*;
import server.packets.RequestUserListPacket;

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

    private final String username; // ✅ Добавлены поля username и password
    private final String password;

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

    public ClientConnection(String host, int port, String username, String password, MessageListener listener) throws IOException {
        this.username = username;  // ✅ Инициализация логина и пароля
        this.password = password;
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

        // ✅ Исправлено: Автоматически отправляем HiPacket с явной передачей данных
        sendHiPacket(this.username, this.password); // ✅ Исправлено
    }

    // ✅ Исправленный метод отправки HiPacket
    public void sendHiPacket(String login, String password) {
        try {
            HiPacket hiPacket = new HiPacket(login, password);
            sendPacket(hiPacket); // Отправляем через стандартный метод
            System.out.println("✅ [ClientConnection] Пакет успешно отправлен: HI");
        } catch (Exception e) {
            System.out.println("❌ [ClientConnection] Ошибка при отправке HiPacket: " + e.getMessage());
            e.printStackTrace();
        }
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
            final int RETRY_DELAY_MS = 100;
            while (!socket.isClosed()) {
                System.out.println("🔎 [ClientConnection] Ожидание пакета от сервера...");

                if (!reader.ready()) {
                    System.out.println("⏳ [ClientConnection] Данных пока нет, ожидание...");
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }

                Packet packet = Packet.readPacket(reader);
                if (packet == null) {
//                    System.out.println("❗️ [ClientConnection] Получен пустой или некорректный пакет, ожидание...");
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }

                if (packet instanceof WelcomePacket wp) {
                    System.out.println("✅ [ClientConnection] Успешная авторизация. Показ списка пользователей.");
                    // Устанавливаем текущего пользователя, используя userId из пакета
                    this.correspondent = new Correspondent(wp.userId, this.username, this.password);
                    requestUserList();  // Запрос списка пользователей после установки correspondent
                } else if (packet instanceof ListPacket listPacket) {
                    System.out.println("✅ [ClientConnection] Получен ListPacket с количеством пользователей: " + listPacket.items.size());
                    if (messageListener != null) {
                        messageListener.onPacketReceived(listPacket);
                    } else {
                        System.out.println("❗️ [ClientConnection] MessageListener не установлен! Список пользователей может не отобразиться.");
                    }
                } else if (packet instanceof ErrorPacket) {
                    System.out.println("❌ [ClientConnection] Ошибка авторизации: " + ((ErrorPacket) packet).getMessage());
                    close();
                    break;
                } else {
                    if (messageListener != null) {
                        messageListener.onPacketReceived(packet);
                    }
                }
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
