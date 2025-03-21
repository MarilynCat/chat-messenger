package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class ErrorPacket extends Packet {
    public static final String type = "ERROR";

    private String message;

    public ErrorPacket(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        writer.println(message);
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        message = reader.readLine();
    }
}
