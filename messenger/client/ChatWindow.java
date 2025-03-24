package client;

import client.ClientConnection;
import server.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class ChatWindow extends JFrame {
    private static ChatWindow instance;
    private JTextArea chatArea;
    private JTextField messageField;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private ClientConnection connection;
    private String username;
    private String selectedUser;
    private final Map<String, Integer> userIdMap = new HashMap<>();

    public ChatWindow(ClientConnection connection, String username) {
        this.connection = connection;
        this.username = username;
        instance = this;

        setTitle("Chat - " + username);
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();

        userListModel.addElement("Вы: " + username);

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selected = userList.getSelectedValue();
                if (selected != null && !selected.startsWith("Вы: ")) {
                    selectedUser = selected;
                    chatArea.append("💬 Начат диалог с " + selectedUser + "\n");
                }
            }
        });
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

        if (selectedUser == null) {
            chatArea.append("⚠️ Пожалуйста, выберите собеседника из списка.\n");
            return;
        }

        if (!userIdMap.containsKey(selectedUser)) {
            chatArea.append("❌ Ошибка: Собеседник не найден в системе.\n");
            return;
        }

        Integer correspondentId = userIdMap.get(selectedUser);

        if (correspondentId == null || correspondentId == -1) {
            chatArea.append("❌ Ошибка: Собеседник не найден или оффлайн.\n");
            return;
        }

        MessagePacket msgPacket = new MessagePacket();
        msgPacket.senderId = connection.getCurrentUserId();
        msgPacket.correspondentId = correspondentId;
        msgPacket.text = text;

        connection.sendPacket(msgPacket);

        displayOutgoingMessage("Me to " + selectedUser + ": " + text);
        messageField.setText("");
    }

    public void displayIncomingPacket(Packet packet) {
        System.out.println("📩 [ChatWindow] Пакет получен: " + packet.getType());

        if (packet instanceof ListPacket) {
            ListPacket listPacket = (ListPacket) packet;
            System.out.println("✅ [ChatWindow] Получен ListPacket с количеством пользователей: " + listPacket.items.size());
            updateUserList(listPacket);
        }

        if (packet instanceof MessagePacket) {
            MessagePacket msg = (MessagePacket) packet;
            displayIncomingMessage("📩 Сообщение от пользователя ID " + msg.senderId + ": " + msg.text);
        }
    }

    private void updateUserList(ListPacket listPacket) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            userListModel.addElement("Вы: " + username);
            userIdMap.clear();

            if (listPacket == null || listPacket.items.isEmpty()) {
                chatArea.append("❗️Нет доступных пользователей для диалога.\n");
                System.out.println("❗️ [ChatWindow] Получен пустой список пользователей!");
                return;
            }

            for (ListPacket.CorrespondentItem item : listPacket.items) {
                userListModel.addElement(item.login);
                userIdMap.put(item.login, item.id);
                System.out.println("➕ [ChatWindow] Добавлен пользователь в список: " + item.login);
            }

            chatArea.append("✅ Список пользователей обновлён.\n");
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