package client;

import server.HiPacket;
import server.Packet;
import server.ByePacket;
import server.WelcomePacket;
import server.ErrorPacket;

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
                    System.out.println("✅ [ClientConnection] Пакет успешно отправлен: " + packet.getType());
                }
            } catch (InterruptedException e) {
                System.out.println("🛑 [ClientConnection] Поток записи прерван.");
            } catch (Exception ex) {
                System.out.println("❌ [ClientConnection] Ошибка при отправке пакета: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        writerThread.start();
    }

    public void sendPacket(Packet packet) {
        if (packet instanceof HiPacket hiPacket) {
            System.out.println("📤 [ClientConnection] Отправка HiPacket: login = " + hiPacket.login + ", password = " + hiPacket.password);
        }

        if (socket.isClosed()) {
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
            while (true) {
                System.out.println("🔎 [ClientConnection] Ожидание пакета от сервера...");

                Packet packet = Packet.readPacket(reader);

                if (packet == null) {
                    System.out.println("❗️ [ClientConnection] Пакет пустой, повторная попытка...");
                    Thread.sleep(300);  // Задержка для предотвращения преждевременного отключения
                    continue;
                }


                if (packet instanceof WelcomePacket) {
                    System.out.println("✅ [ClientConnection] Успешная авторизация. Показ списка пользователей.");
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
            writerThread.interrupt();
            socket.close();
        } catch (Exception ex) {
            System.out.println("❌ [ClientConnection] Ошибка при закрытии соединения: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
