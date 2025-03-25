package server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessengerServer {

	// Список активных сессий (подключённых клиентов)
	private static final ArrayList<Session> sessions = new ArrayList<>();

	// Singleton экземпляр сервера
	private static MessengerServer instance;

	public MessengerServer() {
		instance = this;
	}

	public static MessengerServer getInstance() {
		if (instance == null) {
			instance = new MessengerServer();
		}
		return instance;
	}

	public static void main(String[] args) {
		int port = 20000;
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.out.println("❗️ Неверный формат порта. Используется порт по умолчанию: " + port);
			}
		}

		// Регистрация пользователей
		Correspondent.registerCorrespondent(new Correspondent(1, "User1", "password1"));
		Correspondent.registerCorrespondent(new Correspondent(2, "User2", "password2"));
		Correspondent.registerCorrespondent(new Correspondent(3, "User3", "password3"));

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			Thread dispatcherThread = new Thread(new Dispatcher());
			if (!dispatcherThread.isAlive()) {
				dispatcherThread.start();
			}

			System.out.println("✅ Сервер запущен на порту " + port + ". Ожидание входящих подключений...");

			while (true) {
				Socket socket = serverSocket.accept();
				Session newSession = new Session(socket);

				System.out.println("✅ Установлено новое подключение");

				newSession.start(); // Поток стартует только один раз

				// ✅ Ожидание успешной авторизации без жесткой задержки
				int attempts = 0;
				while (!newSession.isAuthorized() && attempts < 15) {
					Thread.sleep(200);
					attempts++;
				}

				synchronized (sessions) {
					sessions.removeIf(session -> !session.isAlive() || session.getCorrespondent() == null);
				}

			}

		} catch (BindException e) {
			System.out.println("❌ Ошибка: Порт уже используется. Пожалуйста, убедитесь, что сервер не запущен дважды.");
		} catch (Exception ex) {
			System.out.println("❌ Ошибка при запуске сервера на порту " + port + ": " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	// Отправка списка пользователей клиентам
	// Исправлено: Удален ошибочный вызов sendUserList внутри самого метода
	public void sendUserList() {
		ListPacket listPacket = new ListPacket();

		for (Correspondent correspondent : Correspondent.getAllCorrespondents()) {
			if (correspondent != null) {
				listPacket.addItem(correspondent.getId(), correspondent.getLogin());
			}
		}

		for (Session session : sessions) {
			if (session.isAuthorized()) {
				session.sendPacket(listPacket);
				System.out.println("✅ [MessengerServer] Список пользователей отправлен сессии: " + session.getCorrespondent().getLogin());
			}
		}
	}


	// Удаление сессии из списка активных при отключении клиента
	public static void removeSession(Session session) {
		System.out.println("🔄 [MessengerServer] Удаление сессии клиента ID: " + session.getCorrespondentId());
		sessions.remove(session);
		cleanInactiveSessions();
		MessengerServer.getInstance().sendUserList();
		System.out.println("🛑 [MessengerServer] Клиент отключился. ID: " + session.getCorrespondentId());
	}

	// Очистка неактивных сессий
	private static void cleanInactiveSessions() {
		System.out.println("🧹 [MessengerServer] Очистка неактивных сессий...");
		sessions.removeIf(session -> !session.isAlive());
	}

	// Получение активных сессий
	public static List<Session> getActiveSessions() {
		System.out.println("🔎 [MessengerServer] Запрошен список активных сессий. Количество активных сессий: " + sessions.size());
		return Collections.unmodifiableList(sessions);
	}
}
