package client;

import server.Packet;
import server.ByePacket;

import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientConnection extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final LinkedBlockingQueue<Packet> toServerQueue = new LinkedBlockingQueue<>();
    private Thread writerThread;
    private MessageListener messageListener;

    public interface MessageListener {
        void onPacketReceived(Packet packet);
    }

    public ClientConnection(String host, int port, MessageListener listener) throws IOException {
        this.socket = new Socket(host, port);
        this.messageListener = listener;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);

        writerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Packet packet = toServerQueue.take();
                    packet.writePacket(writer);
                }
            } catch (InterruptedException e) {
                // поток прерван, завершаем работу
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        writerThread.start();
    }

    public void sendPacket(Packet packet) {
        toServerQueue.add(packet);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Packet packet = Packet.readPacket(reader);
                if (packet == null || packet.getType().equals(ByePacket.type)) {
                    close();
                    break;
                }
                if (messageListener != null) {
                    messageListener.onPacketReceived(packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public void close() {
        try {
            writerThread.interrupt();
            socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
