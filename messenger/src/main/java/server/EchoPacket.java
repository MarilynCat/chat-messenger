package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class EchoPacket extends Packet {
    public static final String TYPE = "ECHO"; // Исправлена константа TYPE

    public String text;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        writer.println(text);
        writer.println();
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        text = reader.readLine();
    }
}
