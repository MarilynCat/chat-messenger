package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class HiPacket extends Packet {
    public static final String type = "HI";

    public String login;
    public String password;  // Добавлено поле для пароля

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        if (login == null || password == null) {
            System.out.println("❌ [HiPacket] Попытка отправки данных с null-полями.");
            return;
        }

        System.out.println("📤 [HiPacket] Отправка данных: login = " + login + ", password = " + password);
        writer.println(login);      // Отправляем логин
        writer.println(password);   // Отправляем пароль
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        try {
            login = reader.readLine();
            password = reader.readLine();

            if (login == null || password == null) {
                System.out.println("❌ [HiPacket] Некорректные данные в пакете. Пропуск.");
                login = "";
                password = "";
            } else {
                System.out.println("📩 [HiPacket] Получены данные: login = " + login + ", password = " + password);
            }
        } catch (Exception e) {
            System.out.println("❌ [HiPacket] Ошибка при чтении данных: " + e.getMessage());
            throw e;
        }
    }

}
