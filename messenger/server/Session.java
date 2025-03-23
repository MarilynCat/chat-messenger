package server;

import java.io.*;
import java.net.*;
import java.util.Map;
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

        writerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    var p = toClientQueue.take();
                    if (socket.isClosed()) {
                        System.out.println("❗️ [Session] Попытка отправить пакет через закрытое соединение.");
                        continue;
                    }

                    System.out.println("📤 [Session] Пакет перед отправкой: " + p.getType());
                    p.writePacket(writer);
                    flush();  // 🔹 Явный вызов flush() для немедленной отправки
                    if (writer.checkError()) {
                        System.out.println("❗️ [Session] Ошибка при отправке данных.");
                    } else {
                        System.out.println("✅ [Session] Пакет успешно отправлен: " + p.getType());
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
    }

    public int getCorrespondentId() {
        return (correspondent != null) ? correspondent.getId() : -1;
    }

    public void sendPacket(Packet p) {
        if (p != null) {
            toClientQueue.add(p);
            flush();  // 🔹 Добавлен flush() для немедленной отправки данных
            System.out.println("✅ [Session] Пакет добавлен в очередь отправки: " + p.getType());
        } else {
            System.out.println("❌ [Session] Попытка отправки null-пакета. Пропущено.");
        }
    }

    // 🔹 Улучшенный метод flush()
    public void flush() {
        if (writer != null) {
            try {
                writer.flush();

                // 🔹 Дополнительная проверка на активность сокета
                if (!socket.isClosed() && socket.isConnected()) {
                    System.out.println("✅ [Session] Поток данных успешно отправлен (flush).");
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

    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            socket.setSoTimeout(10000);  // Увеличен тайм-аут до 10 секунд

            while (!socket.isClosed()) {
                if (!reader.ready()) {
                    System.out.println("❗️ [Session] Поток данных не готов к чтению.");
                    Thread.sleep(200); // Задержка для предотвращения ненужных итераций
                    continue;
                }

                String rawData = reader.readLine();
                System.out.println("📥 [Session] Получены сырые данные: " + rawData);

                if (rawData == null || rawData.isEmpty()) {
                    System.out.println("❗️ [Session] Пустая строка получена. Возможно, временный разрыв соединения.");
                    Thread.sleep(200); // Задержка для повторной попытки
                    continue;
                }

                var p = Packet.readPacket(reader);
                if (p == null || p.getType().equals(ByePacket.type)) {
                    System.out.println("🛑 [Session] ByePacket отправлен или соединение завершено.");
                    close();
                    return;
                }

                if (p instanceof HiPacket hiPacket) {
                    if (isValidUser(hiPacket.login, hiPacket.password)) {
                        System.out.println("✅ [Session] Успешная авторизация пользователя: " + hiPacket.login);

                        correspondent = Correspondent.getCorrespondent(hiPacket.login);
                        if (correspondent == null) {
                            System.out.println("❗️ [Session] Создаётся новый Correspondent для: " + hiPacket.login);
                            correspondent = new Correspondent(getCorrespondentId(), hiPacket.login, hiPacket.password);
                            Correspondent.registerCorrespondent(correspondent);
                        }

                        correspondent.activeSession = this;

                        if (writer != null) {
                            System.out.println("📤 [Session] Отправляем WelcomePacket.");
                            sendPacket(new WelcomePacket());
                            flush();
                            sendUserListToClient();
                            System.out.println("✅ [Session] WelcomePacket успешно отправлен.");
                        } else {
                            System.out.println("❌ [Session] Ошибка: writer = null. Пакет не отправлен.");
                        }
                    } else {
                        System.out.println("❌ [Session] Неверный логин или пароль.");
                        sendPacket(new ErrorPacket("Invalid credentials"));
                        flush();
                        close();
                    }
                }

                var e = new Event(this, p);
                Dispatcher.event(e);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("❗️ [Session] Тайм-аут при ожидании данных. Закрытие соединения.");
            close();
        } catch (IOException | InterruptedException ex) {
            System.out.println("❌ [Session] Ошибка в session.Session: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private boolean isValidUser(String login, String password) {
        Map<String, String> validUsers = Map.of(
                "User1", "password1",
                "User2", "password2",
                "User3", "password3"
        );
        return validUsers.containsKey(login) && validUsers.get(login).equals(password);
    }

    private void sendUserListToClient() {
        ListPacket userListPacket = new ListPacket();
        userListPacket.addItem(1, "User1");
        userListPacket.addItem(2, "User2");
        userListPacket.addItem(3, "User3");

        sendPacket(userListPacket);
        flush();
        System.out.println("📤 [Session] Список пользователей отправлен клиенту.");
    }

    public void close() {
        try {
            if (correspondent != null) {
                correspondent.activeSession = null;
            }

            if (!socket.isClosed()) {
                socket.shutdownOutput(); // 🔹 Безопасное закрытие потока данных
                socket.close();
            }

            if (writerThread != null) {
                writerThread.interrupt();
            }

            System.out.println("🛑 [Session] Соединение закрыто.");
        } catch (Exception ex) {
            System.out.println("❌ [Session] Ошибка при закрытии session.Session: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
