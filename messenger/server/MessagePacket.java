package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class MessagePacket extends Packet {
    public static final String type = "MSG";

    public int senderId;         // ✅ Добавлено поле senderId
    public int correspondentId;  // ID получателя
    public String text;

    public MessagePacket() {}

    public MessagePacket(int senderId, int correspondentId, String text) {
        this.senderId = senderId;
        this.correspondentId = correspondentId;
        this.text = text;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        if (senderId <= 0 || correspondentId <= 0 || text == null || text.trim().isEmpty()) {
            System.out.println("❌ [MessagePacket] Неверные данные для отправки.");
            return;
        }

        writer.println(senderId);         // ✅ Добавлено поле senderId
        writer.println(correspondentId);
        writer.println(text.trim());
        writer.println();
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        var senderIdText = reader.readLine();
        senderId = Integer.parseInt(senderIdText); // ✅ Чтение senderId

        var correspondentIdText = reader.readLine();
        correspondentId = Integer.parseInt(correspondentIdText);

        text = readText(reader).trim();
    }
}
