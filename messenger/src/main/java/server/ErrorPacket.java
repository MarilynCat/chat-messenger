package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class ErrorPacket extends Packet {
    public static final String TYPE = "ERROR";

    private String message;

    public ErrorPacket() {
        // нужен для Packet.readPacket()
    }

    public ErrorPacket(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String getType() {
        return TYPE;
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
