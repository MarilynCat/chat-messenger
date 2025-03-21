package server;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessengerServer {

	// Список активных сессий (подключённых клиентов)
	private static final ArrayList<Session> sessions = new ArrayList<>();

	public static void main(String[] args) {
		// Регистрируем пользователей с паролями
		Correspondent.registerCorrespondent(new Correspondent(1, "User1", "password1"));
		Correspondent.registerCorrespondent(new Correspondent(2, "User2", "password2"));
		Correspondent.registerCorrespondent(new Correspondent(3, "User3", "password3"));

		try (ServerSocket serverSocket = new ServerSocket(10001)) {
			// Запуск потока диспетчера
			new Thread(new Dispatcher()).start();

			System.out.println("✅ Сервер запущен. Ожидание входящих подключений...");

			// Ожидание входящих подключений
			while (true) {
				Socket socket = serverSocket.accept();
				Session newSession = new Session(socket);

				if (newSession != null) {
					System.out.println("✅ Установлено новое подключение");

					// Новый вызов метода onClientConnected()
					onClientConnected(newSession);

					// Запускаем сессию
					newSession.start();
				} else {
					System.out.println("❌ [MessengerServer] Не удалось создать новую сессию.");
				}
			}

		} catch (BindException e) {
			System.out.println("❌ Ошибка: Порт уже используется. Пожалуйста, убедитесь, что сервер не запущен дважды.");
		} catch (Exception ex) {
			System.out.println("❌ Ошибка при запуске сервера: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	// Метод для обработки нового подключения
	private static void onClientConnected(Session session) {
		sessions.add(session);  // Добавляем нового клиента в список
		System.out.println("✅ Подключён новый клиент. ID: " + session.getCorrespondentId());
		sendUserList();  // Уведомляем всех клиентов о новом пользователе
	}

	// Метод для отправки списка пользователей всем клиентам
	private static void sendUserList() {
		ListPacket listPacket = new ListPacket();

		// Удаляем неактивные сессии перед отправкой
		cleanInactiveSessions();

		// Добавляем всех авторизованных пользователей в список
		System.out.println("🔍 [MessengerServer] Список активных сессий: " + sessions.size());

		for (Session session : sessions) {
			int correspondentId = session.getCorrespondentId();
			if (correspondentId == -1) {
				System.out.println("⚠️ [MessengerServer] Неавторизованная сессия пропущена при отправке списка.");
				continue;
			}

			Correspondent correspondent = Correspondent.getCorrespondent(correspondentId);
			if (correspondent != null) {
				listPacket.addItem(correspondent.getId(), correspondent.getLogin());
				System.out.println("🟢 [MessengerServer] Добавлен пользователь в список: " + correspondent.getLogin());
			} else {
				System.out.println("❌ [MessengerServer] Не удалось найти пользователя с ID: " + correspondentId);
			}
		}

		System.out.println("✅ [MessengerServer] Итоговый список пользователей: " + listPacket.items.size());

		// Рассылаем пакет всем клиентам
		for (Session session : sessions) {
			try {
				session.sendPacket(listPacket);
				System.out.println("📤 [MessengerServer] Пакет отправлен клиенту ID: " + session.getCorrespondentId());
			} catch (Exception e) {
				System.out.println("❌ [MessengerServer] Ошибка при отправке списка пользователю " +
						session.getCorrespondentId() + ": " + e.getMessage());
			}
		}
	}

	// Метод для удаления отключившегося клиента и обновления списка пользователей
	public static void removeSession(Session session) {
		sessions.remove(session);
		sendUserList();  // Отправляем обновлённый список
		System.out.println("🛑 [MessengerServer] Клиент отключился. ID: " + session.getCorrespondentId());
	}

	// Метод для очистки списка от неактивных сессий
	private static void cleanInactiveSessions() {
		sessions.removeIf(session -> !session.isAlive());
	}

	// ✅ Добавленный метод getActiveSessions() для корректной работы Dispatcher
	public static List<Session> getActiveSessions() {
		return Collections.unmodifiableList(sessions); // Возвращаем безопасную копию списка
	}
}