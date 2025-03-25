package server.packets;

import server.Packet;
import java.io.BufferedReader;
import java.io.PrintWriter;

public class RequestUserListPacket extends Packet {
    public static final String TYPE = "REQUEST_USER_LIST"; // Исправлена константа TYPE

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        // Нет данных для отправки
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        // Нет данных для чтения
    }
}
