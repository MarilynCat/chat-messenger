package server;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class ByePacket extends Packet {
    public static final String TYPE = "BYE";  // Исправлено: константа в верхнем регистре по стандарту

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void writeBody(PrintWriter writer) throws Exception {
        // Нет данных для записи в теле пакета
    }

    @Override
    public void readBody(BufferedReader reader) throws Exception {
        // Нет данных для чтения в теле пакета
    }
}
