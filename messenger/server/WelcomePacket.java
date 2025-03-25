package server;

import java.io.BufferedReader;
import java.io.PrintWriter;
public class WelcomePacket extends Packet {
    public static final String TYPE = "WELCOME";
    public int userId; // Новый параметр для передачи ID пользователя

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        writer.println(userId);
        writer.println();  // Завершающая пустая строка
        System.out.println("✅ [WelcomePacket] Пакет успешно записан с userId: " + userId);
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        String line = reader.readLine();
        if (line != null && !line.isEmpty()) {
            userId = Integer.parseInt(line);
        }
        // Читаем завершающую пустую строку
        reader.readLine();
    }
}
