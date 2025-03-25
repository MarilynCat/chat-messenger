package server;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class ListPacket extends Packet {
    public static final String TYPE = "LIST";

    public int currentUserId = -1; // 🆕 Добавлено поле
    public ArrayList<CorrespondentItem> items = new ArrayList<>();

    public static class CorrespondentItem {
        public int id;
        public String login;

        public CorrespondentItem(int id, String login) {
            this.id = id;
            this.login = login;
        }
    }

    public void addItem(int id, String login) {
        items.add(new CorrespondentItem(id, login));
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        writer.println(currentUserId); // 🆕 сначала передаём ID текущего пользователя

        for (CorrespondentItem item : items) {
            writer.println(item.id);
            writer.println(item.login);
        }

        writer.println(); // окончание пакета
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        items.clear();

        String userIdLine = reader.readLine(); // 🆕 сначала читаем ID текущего пользователя
        if (userIdLine == null) return;
        currentUserId = Integer.parseInt(userIdLine);

        while (true) {
            String idLine = reader.readLine();
            if (idLine == null || idLine.isEmpty()) break;

            String login = reader.readLine();
            addItem(Integer.parseInt(idLine), login);
        }
    }
}
