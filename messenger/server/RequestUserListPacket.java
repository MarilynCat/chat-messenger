package server.packets;

import server.Packet;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;

public class RequestUserListPacket extends Packet {
    public static final String type = "REQUEST_USER_LIST";

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void writePacket(PrintWriter writer) {
        writer.println(type);
    }

    @Override
    public void readBody(BufferedReader reader) throws IOException {
        // Пустая реализация, так как тело пакета не требуется
    }

    @Override
    public void writeBody(PrintWriter writer) {
        // Пустая реализация, так как тело пакета не требуется
    }
}
