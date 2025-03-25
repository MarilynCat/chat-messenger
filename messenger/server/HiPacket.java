package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class HiPacket extends Packet {
    public static final String TYPE = "HI";

    public String login;
    public String password;

    public HiPacket(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public HiPacket() {}

    @Override
    public String getType() {
        return TYPE;
    }

    // ✅ Исправление: Добавлен метод writeBody для корректной отправки данных
    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        writer.println(login);
        writer.println(password);
        System.out.println("📤 [HiPacket] Отправка данных: login = " + login + ", password = " + password);
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        login = reader.readLine();
        password = reader.readLine();

        if (login == null || password == null || login.isEmpty() || password.isEmpty()) {
            System.out.println("❌ [HiPacket] Некорректные данные в пакете. Пропуск.");
            login = "";
            password = "";
        } else {
            System.out.println("📩 [HiPacket] Получены данные: login = " + login + ", password = " + password);
        }
    }
}
