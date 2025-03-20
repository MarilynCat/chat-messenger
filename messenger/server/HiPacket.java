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
        writer.println(login);      // Отправляем логин
        writer.println(password);   // Отправляем пароль
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        login = reader.readLine();       // Читаем логин
        password = reader.readLine();    // Читаем пароль
    }
}
