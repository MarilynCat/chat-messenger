package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class EchoPacket extends Packet {
    public static final String type = "ECHO";
    
    public String text;

    public String getType() {
        return type;
    }

    public void writeBody(PrintWriter writer) throws Exception {
        writer.println(text);
        writer.println();
    }

    public void readBody(BufferedReader reader) throws Exception {
        text = readText(reader);
    }
}