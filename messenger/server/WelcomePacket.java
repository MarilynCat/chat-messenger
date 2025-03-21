package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class WelcomePacket extends Packet {
    public static final String type = "WELCOME";

    @Override
    public String getType() {
        return type;
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
