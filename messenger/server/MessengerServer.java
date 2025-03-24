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

		Correspondent.registerCorrespondent(new Correspondent(1, "User1", "password1"));
		Correspondent.registerCorrespondent(new Correspondent(2, "User2", "password2"));
		Correspondent.registerCorrespondent(new Correspondent(3, "User3", "password3"));

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			new Thread(new Dispatcher()).start();

			System.out.println("✅ Сервер запущен на порту " + port + ". Ожидание входящих подключений...");

			while (true) {
				Socket socket = serverSocket.accept();
				Session newSession = new Session(socket);

				System.out.println("✅ Установлено новое подключение");
				sessions.add(newSession); // Добавляем сессию в список активных сессий
				newSession.start();  // onClientConnected теперь вызывается внутри Session при успешной авторизации
			}

		} catch (BindException e) {
			System.out.println("❌ Ошибка: Порт уже используется. Пожалуйста, убедитесь, что сервер не запущен дважды.");
		} catch (Exception ex) {
			System.out.println("❌ Ошибка при запуске сервера на порту " + port + ": " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	public void sendUserList() {
		ListPacket listPacket = new ListPacket();
		System.out.println("📋 [MessengerServer] Формируем список пользователей...");

		for (Correspondent c : Correspondent.getAllCorrespondents()) {
			listPacket.addItem(c.getId(), c.getLogin());
			System.out.println("➕ Добавлен пользователь в список: " + c.getLogin());
		}

		if (listPacket.items.isEmpty()) {
			System.out.println("❗️ [MessengerServer] Список пользователей пуст!");
			return;
		}

		for (Session session : getActiveSessions()) {
			if (!session.getSocket().isClosed()) {
				session.sendPacket(listPacket);
				System.out.println("📤 [MessengerServer] Пакет с пользователями отправлен клиенту ID: " + session.getCorrespondentId());
			} else {
				System.out.println("❗️ [MessengerServer] Сокет закрыт. Пакет не отправлен клиенту ID: " + session.getCorrespondentId());
			}
		}
	}



	public static void removeSession(Session session) {
		System.out.println("🔄 [MessengerServer] Удаление сессии клиента ID: " + session.getCorrespondentId());
		sessions.remove(session);
		cleanInactiveSessions();
		MessengerServer.getInstance().sendUserList();
		System.out.println("🛑 [MessengerServer] Клиент отключился. ID: " + session.getCorrespondentId());
	}

	private static void cleanInactiveSessions() {
		System.out.println("🧹 [MessengerServer] Очистка неактивных сессий...");
		sessions.removeIf(session -> !session.isAlive());
	}

	public static List<Session> getActiveSessions() {
		System.out.println("🔎 [MessengerServer] Запрошен список активных сессий. Количество активных сессий: " + sessions.size());
		return Collections.unmodifiableList(sessions);
	}
}
