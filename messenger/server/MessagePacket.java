package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class MessagePacket extends Packet {
    public static final String type = "MSG";
    
    public int correspondentId;
    public String text;

    public String getType() {
        return type;
    }

    public void writeBody(PrintWriter writer) throws Exception {
        if (correspondentId <= 0 || text == null || text.trim().isEmpty()) {
            System.out.println("❌ [MessagePacket] Неверные данные для отправки.");
            return;
        }

        writer.println(correspondentId);
        writer.println(text);
        writer.println();
    }


    public void readBody(BufferedReader reader) throws Exception {
        var correspondentIdText = reader.readLine();
        correspondentId = Integer.parseInt(correspondentIdText);
        
        text = readText(reader);
    }
}