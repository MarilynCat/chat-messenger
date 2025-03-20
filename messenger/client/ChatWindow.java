package client;

import client.ClientConnection;
import server.EchoPacket;
import server.ListPacket;
import server.MessagePacket;
import server.Packet;

import javax.swing.*;
import java.awt.*;

public class ChatWindow extends JFrame {
    private static ChatWindow instance;
    private JTextArea chatArea;
    private JTextField messageField;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private ClientConnection connection;
    private String username;

    public ChatWindow(ClientConnection connection, String username) {
        this.connection = connection;
        this.username = username;
        instance = this;

        setTitle("Chat - " + username);
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0));

        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        add(userScrollPane, BorderLayout.WEST);
        add(chatScrollPane, BorderLayout.CENTER);
        add(messagePanel, BorderLayout.SOUTH);
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        MessagePacket msgPacket = new MessagePacket();
        msgPacket.correspondentId = 0;
        msgPacket.text = text;
        connection.sendPacket(msgPacket);

        displayOutgoingMessage("Me: " + text);
        messageField.setText("");
    }

    public void displayIncomingPacket(Packet packet) {
        if (packet instanceof EchoPacket) {
            EchoPacket echo = (EchoPacket) packet;
            displayIncomingMessage("Echo: " + echo.text);
        } else if (packet instanceof MessagePacket) {
            MessagePacket msg = (MessagePacket) packet;
            displayIncomingMessage("From: " + msg.text);
        } else if (packet instanceof ListPacket) {
            updateUserList((ListPacket) packet);
        } else {
            displayIncomingMessage("Received: " + packet.getType());
        }
    }

    private void updateUserList(ListPacket listPacket) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (ListPacket.CorrespondentItem item : listPacket.items) {
                userListModel.addElement(item.login);
            }
        });
    }

    public void displayIncomingMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    public void displayOutgoingMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    public static ChatWindow getInstance() {
        return instance;
    }
}
