package server.packets;

import server.Packet;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;

public class RequestUserListPacket extends Packet {

    @Override
    public String getType() {
        return "REQUEST_USER_LIST";
    }

    @Override
    public void writePacket(PrintWriter writer) {
        super.writePacket(writer);  // Убедимся, что базовая логика вызвана
        writer.println(getType());
    }

    @Override
    public void readBody(BufferedReader reader) throws IOException {
        // Пакет не требует данных в теле
    }

    @Override
    public void writeBody(PrintWriter writer) {
        // Пакет не требует данных в теле
    }
}
