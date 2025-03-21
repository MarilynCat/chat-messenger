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
        var item = new CorrespondentItem(id, login);
        items.add(item);
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
        writer.println();  // Пустая строка для обозначения конца списка
    }

    // Метод для чтения данных из потока
    @Override
    public void readBody(BufferedReader reader) throws Exception {
        items.clear();  // Очищаем список перед добавлением данных
        while (true) {
            var firstLine = reader.readLine();
            if (firstLine == null || firstLine.isEmpty()) {
                break;  // Конец данных
            }

            var secondLine = reader.readLine();
            if (secondLine == null) {
                break;  // Предотвращение ошибок при некорректных данных
            }

            try {
                int id = Integer.parseInt(firstLine);
                addItem(id, secondLine); // Добавляем пользователя в список
            } catch (NumberFormatException e) {
                System.err.println("Ошибка при чтении ID пользователя: " + e.getMessage());
            }
        }
    }
}
