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
    private String selectedUser;  // Собеседник для отправки сообщений
    private final Map<String, Integer> userIdMap = new HashMap<>();  // Хранение ID пользователей

    public ChatWindow(ClientConnection connection, String username) {
        this.connection = connection;
        this.username = username;
        instance = this;

        setTitle("Chat - " + username);
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();

        // Добавляем текущего пользователя в список
        userListModel.addElement("Вы: " + username);

        // Выбор собеседника по клику
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selected = userList.getSelectedValue();
                if (selected != null && !selected.startsWith("Вы: ")) {
                    selectedUser = selected;  // Устанавливаем выбранного собеседника
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

        int correspondentId = userIdMap.get(selectedUser);
        if (correspondentId == -1) {
            chatArea.append("❌ Ошибка: Собеседник не авторизован.\n");
            return;
        }

        MessagePacket msgPacket = new MessagePacket();
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

            if (listPacket.items.isEmpty()) {
                chatArea.append("❗️Нет доступных пользователей для диалога.\n");
            } else {
                for (ListPacket.CorrespondentItem item : listPacket.items) {
                    System.out.println("🟢 [ChatWindow] Пользователь в списке: " + item.login);
                }
            }

        } else if (packet instanceof EchoPacket) {
            EchoPacket echo = (EchoPacket) packet;
            displayIncomingMessage("Echo: " + echo.text);

        } else if (packet instanceof MessagePacket) {
            MessagePacket msg = (MessagePacket) packet;
            Correspondent sender = Correspondent.getCorrespondent(msg.correspondentId);

            if (sender != null) {
                displayIncomingMessage("📩 " + sender.getLogin() + ": " + msg.text);
            } else {
                displayIncomingMessage("❓ Сообщение от неизвестного пользователя: " + msg.text);
            }

        } else {
            displayIncomingMessage("Received: " + packet.getType());
            System.out.println("❗️ [ChatWindow] Неизвестный тип пакета: " + packet.getType());
        }
    }

    private void updateUserList(ListPacket listPacket) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            userListModel.addElement("Вы: " + username); // Добавляем текущего пользователя
            userIdMap.clear();  // Очищаем старые ID для корректного отображения

            for (ListPacket.CorrespondentItem item : listPacket.items) {
                if (!item.login.equals(username)) {
                    userListModel.addElement(item.login);
                    userIdMap.put(item.login, item.id); // Сохраняем ID пользователей
                    System.out.println("🟢 Пользователь добавлен в список: " + item.login);
                }
            }

            if (userIdMap.isEmpty()) {
                chatArea.append("❗️Нет доступных пользователей для диалога.\n");
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
