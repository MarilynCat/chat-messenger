package server;

import server.packets.RequestUserListPacket;

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

    public boolean isAuthorized() {
        return correspondent != null;
    }

    public Correspondent getCorrespondent() {
        return correspondent;
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

    public void processPacket(Packet packet) {
        if (packet instanceof RequestUserListPacket) {
            if (!isAuthorized()) {
                System.out.println("❗️ [Session] Попытка запроса списка пользователей от неавторизованного клиента.");
                sendPacket(new ErrorPacket("Вы не авторизованы."));
                return;
            }
            System.out.println("📥 [Session] Получен запрос на список пользователей.");
            MessengerServer.getInstance().sendUserList();
        }

        if (packet instanceof HiPacket hiPacket) {
            System.out.println("📥 [Session] Получен HiPacket с логином: " + hiPacket.login);

            if (!hiPacket.getType().equals(HiPacket.TYPE)) {
                System.out.println("❌ [Session] Некорректный тип пакета для HiPacket.");
                sendPacket(new ErrorPacket("Ошибка авторизации: Некорректный тип пакета."));
                close();
                return;
            }

            Correspondent correspondent = Correspondent.getCorrespondent(hiPacket.login);

            if (correspondent != null && Correspondent.validateUser(hiPacket.login, hiPacket.password)) {
                this.correspondent = correspondent;
                correspondent.activeSession = this;

                System.out.println("✅ Успешная авторизация: " + hiPacket.login);
                sendPacket(new WelcomePacket());
                MessengerServer.getInstance().sendUserList();
            } else {
                System.out.println("❌ Ошибка авторизации: неверные данные для логина " + hiPacket.login);
                sendPacket(new ErrorPacket("Ошибка авторизации"));
                close();
            }
        }


        if (packet instanceof MessagePacket msg) {
            System.out.println("📩 [Session] Получено сообщение от ID " + msg.senderId + ": " + msg.text);

            Correspondent receiver = Correspondent.getCorrespondent(msg.correspondentId);
            if (receiver != null && receiver.activeSession != null) {
                receiver.activeSession.sendPacket(msg);
                System.out.println("✅ [Session] Сообщение доставлено пользователю ID: " + msg.correspondentId);
            } else if (receiver != null) {
                receiver.storeOfflineMessage(msg);
                System.out.println("⚠️ [Session] Получатель оффлайн, сообщение сохранено.");
            } else {
                System.out.println("❗️ [Session] Ошибка: Получатель не найден.");
                sendPacket(new ErrorPacket("Получатель не найден."));
            }
        }
    }

    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            socket.setSoTimeout(20000);

            // Поток отправки данных клиенту
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

            // Чтение данных от клиента
            while (!socket.isClosed()) {
                try {
                    if (reader.ready()) {
                        String rawData = reader.readLine();
                        if (rawData == null || rawData.isEmpty()) {
                            continue;
                        }

                        System.out.println("📥 [Session] Получены сырые данные: " + rawData);

                        Packet p = Packet.readPacket(reader);
                        if (p == null) {
                            System.out.println("❗️ [Session] Ошибка чтения пакета: Пакет не распознан.");
                            sendPacket(new ErrorPacket("Ошибка авторизации: Пакет не распознан."));
                            close();
                            return;
                        }
                        processPacket(p);

                        if (p != null) {
                            processPacket(p);
                        }
                    } else {
                        System.out.println("⏳ [Session] Ожидание данных...");
                        Thread.sleep(200);
                    }
                } catch (IOException e) {
                    System.out.println("❗️ [Session] Ошибка чтения данных: " + e.getMessage());
                }
            }
        } catch (Exception ex) {
            System.out.println("❌ [Session] Неизвестная ошибка: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
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
