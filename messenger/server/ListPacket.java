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

    public void addItem(int id, String login) {
        var item = new CorrespondentItem(id, login);
        items.add(item);
    }

    public String getType() {
        return type;
    }

    public void writeBody(PrintWriter writer) throws Exception {
        for(var ci : items) {
            writer.println(ci.id);
            writer.println(ci.login);
        }
        writer.println();
    }

    public void readBody(BufferedReader reader) throws Exception {
        for(;;) {
            var firstLine = reader.readLine();
            if(firstLine.isEmpty())
                break;
            var secondLine = reader.readLine();
            
            addItem(Integer.parseInt(firstLine), secondLine);
        }
    }
}