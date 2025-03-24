package server;

import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Session extends Thread {
    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final LinkedBlockingQueue<Packet> toClientQueue = new LinkedBlockingQueue<>();
    private Thread writerThread;

    public Correspondent correspondent;

    public Session(Socket socket) {
        this.socket = socket;
    }

    public int getCorrespondentId() {
        return (correspondent != null) ? correspondent.getId() : -1;
    }

    public Socket getSocket() {
        return socket;
    }

    public void sendPacket(Packet p) {
        if (p != null && writer != null && socket.isConnected() && !socket.isClosed()) {
            toClientQueue.add(p);
            flush();
            System.out.println("✅ [Session] Пакет добавлен в очередь отправки: " + p.getType());
        } else {
            System.out.println("❌ [Session] Попытка отправки null-пакета или через закрытый сокет. Пропущено.");
        }
    }

    public void flush() {
        if (writer != null) {
            try {
                writer.flush();
                if (!socket.isClosed() && socket.isConnected()) {
                    System.out.println("✅ [Session] Поток данных успешно отправлен (flush). ");
                } else {
                    System.out.println("❗️ [Session] Flush вызван на закрытом сокете.");
                }
            } catch (Exception e) {
                System.out.println("❌ [Session] Ошибка при flush(): " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("❗️ [Session] Поток записи (writer) не существует.");
        }
    }

    public void requestUserList() {
        System.out.println("📋 [Session] Запрос списка пользователей...");
        MessengerServer.getInstance().sendUserList();
    }

    public void processPacket(Packet packet) {
        if (packet instanceof HiPacket) {
            HiPacket hiPacket = (HiPacket) packet;
            System.out.println("📥 [Session] Получен HiPacket с логином: " + hiPacket.login);

            Correspondent correspondent = Correspondent.getCorrespondent(hiPacket.login);

            if (correspondent != null && Correspondent.validateUser(hiPacket.login, hiPacket.password)) {
                this.correspondent = correspondent;
                correspondent.activeSession = this;

                if (MessengerServer.getInstance() != null) {
                    System.out.println("📢 [Session] Вызываем sendUserList() после успешной авторизации.");
                    MessengerServer.getInstance().sendUserList();
                }

                System.out.println("✅ Успешная авторизация: " + hiPacket.login);
            } else {
                System.out.println("❌ Ошибка авторизации: неверные данные для логина " + hiPacket.login);
                sendPacket(new ErrorPacket("Ошибка авторизации"));
                close();
            }
        }
    }

    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            socket.setSoTimeout(20000);

            writerThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                    try {
                        Packet p = toClientQueue.take();
                        if (socket.isClosed()) {
                            System.out.println("❗️ [Session] Попытка отправить пакет через закрытое соединение.");
                            continue;
                        }

                        if (writer != null) {
                            System.out.println("📤 [Session] Пакет перед отправкой: " + p.getType());
                            p.writePacket(writer);
                            flush();

                            if (writer.checkError()) {
                                System.out.println("❗️ [Session] Ошибка при отправке данных.");
                            } else {
                                System.out.println("✅ [Session] Пакет успешно отправлен: " + p.getType());
                            }
                        } else {
                            System.out.println("❌ [Session] Ошибка: writer == null. Пакет не отправлен.");
                        }
                    } catch (InterruptedException x) {
                        System.out.println("🛑 [Session] Поток записи остановлен.");
                        break;
                    } catch (Exception e) {
                        System.out.println("❌ [Session] Ошибка при отправке пакета: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            writerThread.start();

            while (!socket.isClosed()) {
                if (!reader.ready()) {
                    Thread.sleep(200);
                    continue;
                }

                String rawData = reader.readLine();
                System.out.println("📥 [Session] Получены сырые данные: " + rawData);

                if (rawData == null || rawData.isEmpty()) {
                    Thread.sleep(200);
                    continue;
                }

                Packet p = Packet.readPacket(reader);
                if (p instanceof MessagePacket msg) {
                    Correspondent receiver = Correspondent.getCorrespondent(msg.correspondentId);
                    if (receiver != null && receiver.activeSession != null) {
                        receiver.activeSession.sendPacket(msg);
                    } else if (receiver != null) {
                        receiver.storeOfflineMessage(msg);
                        System.out.println("⚠️ [Session] Получатель оффлайн, сообщение сохранено.");
                    } else {
                        System.out.println("❗️ [Session] Ошибка: Получатель не найден.");
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("❌ [Session] Неизвестная ошибка: " + ex.getMessage());
            ex.printStackTrace();
            close();
        }
    }

    public void close() {
        try {
            if (correspondent != null) {
                correspondent.activeSession = null;
            }

            if (!socket.isClosed()) {
                socket.shutdownOutput();
                socket.close();
            }

            if (writerThread != null && writerThread.isAlive()) {
                writerThread.interrupt();
                try {
                    writerThread.join(1000);
                } catch (InterruptedException e) {
                    System.out.println("❗️ [Session] Поток записи не завершился корректно.");
                }
            }

            System.out.println("🛑 [Session] Соединение закрыто.");
        } catch (Exception ex) {
            System.out.println("❌ [Session] Ошибка при закрытии session.Session: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}