package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class WelcomePacket extends Packet {
    public static final String TYPE = "WELCOME"; // Исправлена константа TYPE

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        writer.println("Добро пожаловать в чат!");
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        // Нет дополнительных данных для чтения
    }
}
