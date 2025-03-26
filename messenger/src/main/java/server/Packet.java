package server;

import server.packets.RequestUserListPacket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class Packet {

    public abstract String getType();

    public abstract void writeBody(PrintWriter writer) throws Exception;

    public abstract void readBody(BufferedReader reader) throws Exception;

    // ✅ Исправленный фабричный метод для чтения пакетов
    public static Packet readPacket(BufferedReader reader) throws IOException {
        String packetType;
        // Пропускаем пустые строки (например, завершающую пустую строку предыдущего пакета)
        do {
            packetType = reader.readLine();
        } while (packetType != null && packetType.isEmpty());

        if (packetType == null || packetType.isEmpty()) {
            System.out.println("❗️ [Packet] Пакет не распознан или пустой.");
            return null;
        }

        Packet packet;
        switch (packetType) {
            case HiPacket.TYPE -> packet = new HiPacket();
            case ListPacket.TYPE -> packet = new ListPacket();
            case WelcomePacket.TYPE -> packet = new WelcomePacket();
            case ErrorPacket.TYPE -> packet = new ErrorPacket();
            case RequestUserListPacket.TYPE -> packet = new RequestUserListPacket();
            case MessagePacket.TYPE -> packet = new MessagePacket();
            default -> {
                System.out.println("❗️ [Packet] Неизвестный тип пакета: " + packetType);
                return null;
            }
        }

        try {
            packet.readBody(reader);
        } catch (Exception e) {
            System.out.println("❌ [Packet] Ошибка при чтении данных пакета: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        return packet;
    }


    // ✅ Исправленный метод для корректной записи пакетов в поток
    public void writePacket(PrintWriter writer) throws Exception {
        if (getType() == null || getType().isEmpty()) {
            System.out.println("❗️ [Packet] Попытка записи пакета с пустым типом.");
            return;
        }

        writer.println(getType());
        try {
            writeBody(writer);
            writer.flush();
            System.out.println("✅ [Packet] Пакет успешно записан: " + getType());
        } catch (Exception e) {
            System.out.println("❌ [Packet] Ошибка при записи пакета: " + e.getMessage());
            throw e;
        }
    }
}
