package server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Correspondent {
    public final int id;
    public final String login;
    public final String password; // Добавляем поле для пароля

    public Session activeSession;

    // Конструктор с паролем
    public Correspondent(int id, String login, String password) {
        this.id = id;
        this.login = login;
        this.password = password; // Инициализация пароля
    }

    private static final Map<Integer, Correspondent> correspondentById = new HashMap<>();
    private static final Map<String, Correspondent> correspondentByLogin = new HashMap<>();

    // Регистрация пользователя с паролем
    public static void registerCorrespondent(Correspondent c) {
        correspondentById.put(c.id, c);
        correspondentByLogin.put(c.login, c);
    }

    // Поиск по ID
    public static Correspondent findCorrespondent(int id) {
        return correspondentById.get(id);
    }

    // Поиск по логину
    public static Correspondent findCorrespondent(String login) {
        return correspondentByLogin.get(login);
    }

    // Список всех пользователей
    public static Collection<Correspondent> listAll() {
        return correspondentById.values();
    }

    public static boolean validateUser(String login, String password) {
        Correspondent correspondent = correspondentByLogin.get(login);
        return correspondent != null && correspondent.password.equals(password);  // Проверка пароля
    }
}
