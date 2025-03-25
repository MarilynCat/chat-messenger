package server;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class ListPacket extends Packet {
    public static final String TYPE = "LIST"; // Исправлена константа TYPE

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
        for (CorrespondentItem item : items) {
            writer.println(item.id);
            writer.println(item.login);
        }
        writer.println();
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        items.clear();
        while (true) {
            String idLine = reader.readLine();
            if (idLine == null || idLine.isEmpty()) break;

            String login = reader.readLine();
            addItem(Integer.parseInt(idLine), login);
        }
    }
}
