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
                    sendPacketWithFlush(session, new ErrorPacket("Empty login or password."));
                    session.close();
                    return;
                }

                var correspondent = Correspondent.getCorrespondent(hiP.login);

                if (correspondent == null || !Correspondent.validateUser(hiP.login, hiP.password)) {
                    System.out.println("❌ [Dispatcher] Ошибка: Неверный логин или пароль.");
                    sendPacketWithFlush(session, new ErrorPacket("Invalid credentials."));
                    session.close();
                    return;
                }

                session.correspondent = correspondent;
                correspondent.activeSession = session;

                System.out.println("📤 [Dispatcher] Отправляем WelcomePacket пользователю: " + hiP.login);
                sendPacketWithFlush(session, new WelcomePacket());
                for (MessagePacket msg : correspondent.getSessionMessages()) {
                    if (msg.correspondentId == correspondent.getId() || msg.senderId == correspondent.getId()) {
                        sendPacketWithFlush(session, msg);
                    }
                }
                System.out.println("📤 [Dispatcher] История сообщений отправлена пользователю: " + correspondent.getLogin());
                System.out.println("✅ [Dispatcher] WelcomePacket успешно отправлен.");
            }


            case ListPacket listP -> {
                System.out.println("✅ [Dispatcher] Получен ListPacket с пользователями: " + listP.items.size());
                for (ListPacket.CorrespondentItem item : listP.items) {
                    System.out.println("🟢 [Dispatcher] Пользователь в списке: " + item.login);
                }
            }

            case MessagePacket msgP -> {
                System.out.println("💬 [Dispatcher] Сообщение от ID: " + session.getCorrespondentId());

                if (msgP.text == null || msgP.text.trim().isEmpty()) {
                    System.out.println("❌ [Dispatcher] Пустое сообщение не отправлено.");
                    sendPacketWithFlush(session, new ErrorPacket("Empty message."));
                    return;
                }

                var recipientSession = findSessionById(msgP.correspondentId);
                if (recipientSession != null && recipientSession.isAlive()) {
                    sendPacketWithFlush(recipientSession, msgP);
                    // Добавляем в историю и для отправителя, и для получателя
                    session.getCorrespondent().addToSessionHistory(msgP);
                    recipientSession.getCorrespondent().addToSessionHistory(msgP);
                } else {
                    Correspondent recipient = Correspondent.getCorrespondent(msgP.correspondentId);
                    if (recipient != null) {
                        recipient.storeOfflineMessage(msgP);
                        session.getCorrespondent().addToSessionHistory(msgP);
                        recipient.addToSessionHistory(msgP);
                    } else {
                        sendPacketWithFlush(session, new ErrorPacket("Recipient not found or inactive."));
                    }
                }

            }

            default -> {
                System.out.println("❗️ [Dispatcher] Неизвестный пакет: " + p.getType());
                sendPacketWithFlush(session, new ErrorPacket("Unknown packet type."));
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

    // ✅ Добавленный метод для корректной отправки с flush()
    private void sendPacketWithFlush(Session session, Packet packet) {
        if (session != null && packet != null) {
            session.sendPacket(packet);
            session.flush();
            System.out.println("✅ [Dispatcher] Пакет успешно отправлен с flush(): " + packet.getType());
        } else {
            System.out.println("❗️ [Dispatcher] Не удалось отправить пакет. Параметры null.");
        }
    }
}
