package server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Correspondent {
    private final int id;
    private final String login;
    private final String password; // Поле для пароля

    public Session activeSession;

    // Конструктор с паролем
    public Correspondent(int id, String login, String password) {
        this.id = id;
        this.login = login;
        this.password = password;
    }

    // Геттеры для получения ID и логина
    public int getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    // ==============================
    // 🔒 Система хранения пользователей
    // ==============================
    private static final Map<Integer, Correspondent> correspondentById = new HashMap<>();
    private static final Map<String, Correspondent> correspondentByLogin = new HashMap<>();

    // Регистрация нового пользователя
    public static void registerCorrespondent(Correspondent c) {
        correspondentById.put(c.id, c);
        correspondentByLogin.put(c.login, c);
    }

    // Поиск по ID
    public static Correspondent getCorrespondent(int id) {
        return correspondentById.get(id);
    }

    // Поиск по логину
    public static Correspondent getCorrespondent(String login) {
        return correspondentByLogin.get(login);
    }

    // Список всех пользователей
    public static Collection<Correspondent> listAll() {
        return correspondentById.values();
    }

    // Валидация логина и пароля
    public static boolean validateUser(String login, String password) {
        Correspondent correspondent = correspondentByLogin.get(login);
        if (correspondent != null && correspondent.password.equals(password)) {
            System.out.println("✅ [Correspondent] Пользователь авторизован: " + login);
            return true;
        } else {
            System.out.println("❌ [Correspondent] Неверный логин или пароль для: " + login);
            return false;
        }
    }

}
