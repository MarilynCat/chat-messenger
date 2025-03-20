package server;

import java.net.*;

public class MessengerServer {

	public static void main(String[] args) {
		// Регистрируем пользователей с паролями
		Correspondent.registerCorrespondent(new Correspondent(1, "user1", "password1"));
		Correspondent.registerCorrespondent(new Correspondent(2, "user2", "password2"));
		Correspondent.registerCorrespondent(new Correspondent(3, "user3", "password3"));

		try (ServerSocket serverSocket = new ServerSocket(10001)) {
			// Запуск потока диспетчера
			new Thread(new Dispatcher()).start();

			System.out.println("Waiting for incoming connection");

			// Ожидание входящих подключений
			while (true) {
				Socket socket = serverSocket.accept();
				new Session(socket).start();
			}

		} catch (Exception ex) {
			System.out.println("Problem when starting server: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
}
