package server;

import java.io.*;
import java.util.*;
import java.util.function.*;

public abstract class Packet {
    private static final String END_MARKER = "###END###";

    private static final Map<String, Function<String, Packet>> typeMap = Map.of(
            EchoPacket.type, s -> new EchoPacket(),
            HiPacket.type, s -> new HiPacket(),
            ByePacket.type, s -> new ByePacket(),
            MessagePacket.type, s -> new MessagePacket(),
            ListPacket.type, s -> new ListPacket(),
            WelcomePacket.type, s -> new WelcomePacket(),
            ErrorPacket.type, ErrorPacket::new  // ✅ Исправление: добавлена поддержка конструктора с параметром
    );

    public abstract String getType();

    public abstract void writeBody(PrintWriter writer) throws Exception;

    public abstract void readBody(BufferedReader reader) throws Exception;

    // ✅ Улучшенная диагностика в методе отправки пакетов
    public void writePacket(PrintWriter writer) {
        if (this instanceof MessagePacket msg) {
            writer.println("MESSAGE");
            writer.println("From: " + msg.senderId);
            writer.println("To: " + msg.correspondentId);
            writer.println("Text: " + msg.text);
            writer.println("###END###");
        }
    }


    public static Packet readPacket(BufferedReader reader) {
        try {
            if (reader == null) {
                System.out.println("❌ [Packet] Ошибка: Поток чтения = null.");
                return null;
            }

            // ✅ Проверяем, готов ли поток данных
            if (!reader.ready()) {
                System.out.println("❗️ [Packet] Поток данных не готов к чтению.");
                return null;
            }

            var type = reader.readLine();
            System.out.println("🔍 [Packet] Прочитано из потока: '" + type + "'");

            if (type == null || type.isEmpty()) {
                System.out.println("❌ [Packet] Пустая строка или конец потока. Пакет не получен.");
                return null;
            }

            var packetSupplier = typeMap.get(type.trim());
            if (packetSupplier == null) {
                System.out.println("❌ [Packet] Нераспознанный тип пакета: '" + type + "'");
                return null;
            }

            Packet packet = packetSupplier.apply(type.equals(ErrorPacket.type) ? reader.readLine() : "");
            packet.readBody(reader);

            // 🔎 Чтение маркера завершения пакета
            String endSignal = reader.readLine();
            if (endSignal == null) {
                System.out.println("❌ [Packet] Пакет оборван или маркер завершения не получен.");
                return null;
            }

            System.out.println("🔎 [Packet] Прочитан маркер завершения: '" + endSignal + "'");

            if (!END_MARKER.equals(endSignal.trim())) {
                System.out.println("❌ [Packet] Пакет не завершён корректно. Ожидался маркер '###END###'.");
                return null;
            }

            System.out.println("✅ [Packet] Пакет успешно прочитан: " + packet.getType());
            return packet;
        } catch (Exception x) {
            System.out.println("❌ [Packet] Ошибка при чтении пакета: " + x.getMessage());
            x.printStackTrace();
            return null;
        }
    }

    public String readText(BufferedReader reader) throws Exception {
        StringBuilder text = new StringBuilder();
        while (true) {
            var s = reader.readLine();
            if (s == null || s.trim().equals(END_MARKER)) break; // Поддержка пустых строк и длинных сообщений

            if (text.length() > 0) {
                text.append("\n");
            }
            text.append(s);
        }

        System.out.println("📩 [Packet] Прочитан текст: " + text.toString());
        return text.toString();
    }
}
