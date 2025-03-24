package server;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class ListPacket extends Packet {
    public static final String type = "LIST";

    public static class CorrespondentItem {
        public int id;
        public String login;

        public CorrespondentItem(int id, String login) {
            this.id = id;
            this.login = login;
        }
    }

    public ArrayList<CorrespondentItem> items = new ArrayList<>();

    // Добавление пользователя в список
    public void addItem(int id, String login) {
        if (login != null && !login.trim().isEmpty()) {
            var item = new CorrespondentItem(id, login);
            items.add(item);
        } else {
            System.err.println("❌ [ListPacket] Попытка добавить пользователя с пустым логином.");
        }
    }

    @Override
    public String getType() {
        return type;
    }

    // Метод для записи данных в поток
    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        for (var ci : items) {
            writer.println(ci.id);      // Пишем ID пользователя
            writer.println(ci.login);   // Пишем имя пользователя
        }
        writer.println();  // ✅ Пустая строка для обозначения конца списка
    }

    // Метод для чтения данных из потока
    @Override
    public void readBody(BufferedReader reader) throws Exception {
        items.clear();  // Очищаем список перед добавлением данных
        System.out.println("🔎 [ListPacket] Начато чтение данных о пользователях...");

        while (true) {
            var firstLine = reader.readLine();
            if (firstLine == null || firstLine.isEmpty()) {  // ✅ Корректное завершение чтения
                System.out.println("✅ [ListPacket] Прочитано " + items.size() + " пользователей.");
                break;
            }

            var secondLine = reader.readLine();
            if (secondLine == null || secondLine.isEmpty()) {
                System.out.println("✅ [ListPacket] Прочитано " + items.size() + " пользователей.");
                break;
            }

            try {
                int id = Integer.parseInt(firstLine);
                addItem(id, secondLine);
                System.out.println("👤 [ListPacket] Добавлен пользователь: " + secondLine);
            } catch (NumberFormatException e) {
                System.err.println("❌ [ListPacket] Ошибка при чтении ID пользователя: " + e.getMessage());
            }
        }
    }
}