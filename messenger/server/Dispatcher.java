package server;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class Dispatcher implements Runnable {
    private static final LinkedBlockingQueue<Event> packetQueue = new LinkedBlockingQueue<>();

    public static void event(Event e) {
        packetQueue.add(e);
    }

    public void run() {
        System.out.println("✅ [Dispatcher] Поток диспетчера запущен.");
        while (true) {
            try {
                var e = packetQueue.take();
                processPacket(e.session, e.packet);
            } catch (InterruptedException x) {
                System.out.println("🛑 [Dispatcher] Поток прерван.");
                break;
            } catch (Exception e) {
                System.out.println("❌ [Dispatcher] Ошибка при обработке пакета: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void processPacket(Session session, Packet p) {
        if (session == null) {
            System.out.println("❌ [Dispatcher] Ошибка: Сессия не определена.");
            return;
        }

        if (p == null) {
            System.out.println("❌ [Dispatcher] Ошибка: Пакет не определён.");
            return;
        }

        System.out.println("📩 [Dispatcher] Пакет для обработки: " + p.getType());

        switch (p) {
            case HiPacket hiP -> {
                System.out.println("✅ [Dispatcher] Получен HiPacket от: " + hiP.login);

                if (hiP.login == null || hiP.password == null) {
                    System.out.println("❌ [Dispatcher] Ошибка: Поля логина или пароля пустые.");
                    session.sendPacket(new ErrorPacket("Empty login or password."));
                    session.close();
                    return;
                }

                var correspondent = Correspondent.getCorrespondent(hiP.login);

                if (correspondent == null || !Correspondent.validateUser(hiP.login, hiP.password)) {
                    System.out.println("❌ [Dispatcher] Ошибка: Неверный логин или пароль.");
                    session.sendPacket(new ErrorPacket("Invalid credentials."));
                    session.close();
                    return;
                }

                session.correspondent = correspondent;
                correspondent.activeSession = session;

                System.out.println("✅ [Dispatcher] Пользователь успешно авторизован: " + hiP.login);
                session.sendPacket(new WelcomePacket());
            }

            case ListPacket listP -> {
                System.out.println("✅ [Dispatcher] Получен ListPacket с пользователями: " + listP.items.size());
                for (ListPacket.CorrespondentItem item : listP.items) {
                    System.out.println("🟢 [Dispatcher] Пользователь в списке: " + item.login);
                }
            }

            case MessagePacket msgP -> {
                System.out.println("💬 [Dispatcher] Сообщение от ID: " + session.getCorrespondentId());

                var recipientSession = findSessionById(msgP.correspondentId);
                if (recipientSession != null) {
                    System.out.println("📨 [Dispatcher] Сообщение отправлено пользователю ID: " + msgP.correspondentId);
                    recipientSession.sendPacket(msgP);
                } else {
                    System.out.println("❌ [Dispatcher] Получатель ID " + msgP.correspondentId + " не найден.");
                    session.sendPacket(new ErrorPacket("Recipient not found."));
                }
            }

            default -> {
                System.out.println("❗️ [Dispatcher] Неизвестный пакет: " + p.getType());
                session.sendPacket(new ErrorPacket("Unknown packet type."));
            }
        }
    }

    // Исправленный метод для поиска сессии по ID собеседника
    private Session findSessionById(int correspondentId) {
        List<Session> activeSessions = MessengerServer.getActiveSessions();
        if (activeSessions == null || activeSessions.isEmpty()) {
            System.out.println("❌ [Dispatcher] Ошибка: Список сессий пуст.");
            return null;
        }

        for (Session session : activeSessions) {
            if (session.getCorrespondentId() == correspondentId) {
                return session;
            }
        }

        System.out.println("❌ [Dispatcher] Ошибка: Сессия с ID " + correspondentId + " не найдена.");
        return null;
    }
}
