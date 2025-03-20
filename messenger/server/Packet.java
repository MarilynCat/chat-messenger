package server;

import java.io.*;
import java.util.*;
import java.util.function.*;

public abstract class Packet {
    private static Map<String, Supplier<Packet>> typeMap = Map.of(
        EchoPacket.type, () -> { return new EchoPacket(); },
        HiPacket.type, () -> { return new HiPacket(); },
        ByePacket.type, () -> { return new ByePacket(); },
        MessagePacket.type, () -> { return new MessagePacket(); },
        ListPacket.type, () -> { return new ListPacket(); }
    );

    public abstract String getType();

    public abstract void writeBody(PrintWriter writer) throws Exception;

    public abstract void readBody(BufferedReader reader) throws Exception;

    public void writePacket(PrintWriter writer) {
        try {
            writer.println( getType() );
            writeBody(writer);
        } 
        catch(Exception x) { throw new RuntimeException(x); }
    }

    public static Packet readPacket(BufferedReader reader) {
        try {
            var type = reader.readLine();
            if(type == null) type = "";
            var packetSupplier = typeMap.get(type);
            if(packetSupplier == null) {
                System.out.println("Unrecognized message type '" + type + "'");
                return null;
            }
            
            var packet = packetSupplier.get();
            packet.readBody(reader);
            return packet;
        } 
        catch(Exception x) { throw new RuntimeException(x); }
    }

    public String readText(BufferedReader reader) throws Exception {
        String text = "";
        for(;;) {
            var s = reader.readLine();
            if(s.isEmpty()) break;
            
            if(!text.isEmpty()) text += "\n";
            text += s;
        }
        return text;
    }
}