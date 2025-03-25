package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class WelcomePacket extends Packet {
    public static final String TYPE = "WELCOME"; // Исправлена константа TYPE

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        writer.println("Добро пожаловать в чат!");
        writer.println(); // Завершающая пустая строка для корректного разделения пакетов
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        StringBuilder body = new StringBuilder();
        String line;
        // Читаем все строки до пустой строки, которая означает конец пакета
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            body.append(line);
        }
        System.out.println("📩 [WelcomePacket] Получено сообщение: " + body.toString());
    }

}
