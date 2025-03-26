package server;

import java.util.*;

public class Correspondent {
    private final int id;
    private final String login;
    private final String password;

    public Session activeSession;
    private final List<MessagePacket> offlineMessages = new ArrayList<>();
    private final List<MessagePacket> sessionMessages = new ArrayList<>(); // 🆕 история сообщений текущей сессии


    // Конструктор с паролем
    public Correspondent(int id, String login, String password) {
        this.id = id;
        this.login = login;
        this.password = password;
    }

    // Геттеры
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

    // Регистрация пользователей (встроенные учетные записи)
    static {
        registerCorrespondent(new Correspondent(1, "User1", "password1"));
        registerCorrespondent(new Correspondent(2, "User2", "password2"));
        registerCorrespondent(new Correspondent(3, "User3", "password3"));
        System.out.println("✅ [Correspondent] Базовые пользователи зарегистрированы.");
    }

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
        if (correspondentByLogin.containsKey(login)) {
            return correspondentByLogin.get(login);
        }
        System.out.println("❗️ [Correspondent] Пользователь " + login + " не найден.");
        return null;
    }

    // Список всех пользователей
    public static Collection<Correspondent> getAllCorrespondents() {
        return Collections.unmodifiableCollection(correspondentById.values());
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

    // ==============================
    // 🔄 Оффлайн-сообщения
    // ==============================
    public void storeOfflineMessage(MessagePacket msg) {
        offlineMessages.add(msg);
        System.out.println("⚠️ [Correspondent] Сообщение сохранено для оффлайн пользователя: " + login);
    }

    public List<MessagePacket> getOfflineMessages() {
        return new ArrayList<>(offlineMessages); // Возвращаем копию для безопасности
    }

    public void clearOfflineMessages() {
        offlineMessages.clear();
    }

    // Автоматическая отправка оффлайн-сообщений при подключении
    public void deliverOfflineMessages() {
        if (!offlineMessages.isEmpty() && activeSession != null) {
            for (MessagePacket msg : offlineMessages) {
                activeSession.sendPacket(msg);
                System.out.println("📤 [Correspondent] Доставлено оффлайн сообщение: " + msg.text);
            }
            clearOfflineMessages();
        }
    }

    public void addToSessionHistory(MessagePacket msg) {
        sessionMessages.add(msg);
        System.out.println("🗂 [Correspondent] Сообщение добавлено в историю сессии пользователя: " + login);
    }

    public List<MessagePacket> getSessionMessages() {
        return new ArrayList<>(sessionMessages); // безопасная копия
    }

}
