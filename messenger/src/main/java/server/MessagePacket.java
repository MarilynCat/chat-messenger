package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class MessagePacket extends Packet {
    public static final String TYPE = "MSG";  // Исправлена константа TYPE

    public int senderId;
    public int correspondentId;
    public String text;

    // ✅ Конструктор для инициализации данных
    public MessagePacket(int senderId, int correspondentId, String text) {
        this.senderId = senderId;
        this.correspondentId = correspondentId;
        this.text = (text != null) ? text : "";

    }

    // ✅ Конструктор по умолчанию для корректной десериализации
    public MessagePacket() {}

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        if (senderId <= 0 || correspondentId <= 0 || text == null || text.isEmpty()) {
            System.out.println("❌ [MessagePacket] Неверные данные для отправки.");
            return;
        }

        writer.println(senderId);
        writer.println(correspondentId);
        writer.println(text);
        writer.println();  // ✅ Добавлен перенос строки для корректной передачи
        System.out.println("✅ [MessagePacket] Пакет успешно записан: " + text);
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        try {
            senderId = Integer.parseInt(reader.readLine());
            correspondentId = Integer.parseInt(reader.readLine());
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                sb.append(line).append("\n");
            }
            text = sb.toString().trim();

            if (text.isEmpty()) {
                System.out.println("❗️ [MessagePacket] Получено пустое сообщение.");
            }


            System.out.println("📩 [MessagePacket] Получено сообщение: " + text);
        } catch (NumberFormatException e) {
            System.out.println("❌ [MessagePacket] Ошибка при чтении ID отправителя или собеседника: " + e.getMessage());
            senderId = -1;
            correspondentId = -1;
        } catch (Exception e) {
            System.out.println("❌ [MessagePacket] Ошибка при чтении данных пакета: " + e.getMessage());
            throw e;
        }
    }
}
