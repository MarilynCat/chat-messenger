package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class ByePacket extends Packet {
    public static final String type = "BYE";
    
    public String getType() {
        return type;
    }

    public void writeBody(PrintWriter writer) throws Exception {}

    public void readBody(BufferedReader reader) throws Exception {}
}