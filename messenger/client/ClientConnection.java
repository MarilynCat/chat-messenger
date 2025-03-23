package client;

import server.HiPacket;
import server.Packet;
import server.ByePacket;
import server.WelcomePacket;
import server.ErrorPacket;
import server.ListPacket;

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
            int retryCount = 0; // Счётчик неудачных попыток чтения
            while (!socket.isClosed()) {
                System.out.println("🔎 [ClientConnection] Ожидание пакета от сервера...");

                // ✅ Добавлена проверка на готовность потока
                if (!reader.ready()) {
                    System.out.println("⏳ [ClientConnection] Поток данных не готов к чтению. Попытка " + (retryCount + 1));
                    System.out.println("🟠 [ClientConnection] Проверяем состояние сокета: "
                            + (!socket.isClosed() ? "Открыт" : "Закрыт"));
                }


                Packet packet = Packet.readPacket(reader);

                if (packet == null) {
                    System.out.println("❗️ [ClientConnection] Пакет пустой или некорректный, попытка " + (retryCount + 1));
                    retryCount++;
                    continue;
                }

                retryCount = 0; // Сбрасываем счётчик при успешном чтении

                if (packet instanceof WelcomePacket) {
                    System.out.println("✅ [ClientConnection] Успешная авторизация. Показ списка пользователей.");
                }

                if (packet instanceof ListPacket listPacket) {
                    System.out.println("📋 [ClientConnection] Список пользователей получен:");
                    for (var item : listPacket.items) {
                        System.out.println("👤 Пользователь: " + item.login);
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
        } catch (Exception e) {
            System.out.println("❌ [ClientConnection] Ошибка при чтении пакета: " + e.getMessage());
            e.printStackTrace();
        } finally {
            close();
        }
    }


    public void close() {
        try {
            System.out.println("🛑 [ClientConnection] Закрытие подключения...");

            if (writerThread != null && writerThread.isAlive()) {
                writerThread.interrupt();
                writerThread.join(1000);  // 🔹 Ограничение ожидания до 1 секунды
            }

            if (socket != null && !socket.isClosed()) {
                socket.shutdownOutput();  // 🔹 Закрытие вывода для корректного завершения
                socket.close();
            }
        } catch (Exception ex) {
            System.out.println("❌ [ClientConnection] Ошибка при закрытии соединения: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
