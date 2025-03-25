package client;

import client.ClientConnection;
import server.*;
import server.packets.RequestUserListPacket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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

        // Добавление текущего пользователя
        userListModel.addElement("Вы: " + username);

        // Автоматический запрос списка пользователей после запуска
        connection.sendPacket(new RequestUserListPacket());

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selected = userList.getSelectedValue();
                if (selected != null && !selected.startsWith("Вы: ")) {
                    selectedUser = selected;
                    chatArea.append("💬 Начат диалог с " + selectedUser + "\n");

                    if (!userIdMap.containsKey(selectedUser)) {
                        chatArea.append("❌ Ошибка: Собеседник не найден в системе.\n");
                    }
                }
            }
        });
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // ---------------------- Список контактов (левая панель) ----------------------
        JPanel contactsPanel = new JPanel();
        contactsPanel.setLayout(new BoxLayout(contactsPanel, BoxLayout.Y_AXIS));
        contactsPanel.setBackground(new Color(30, 30, 30));
        contactsPanel.setPreferredSize(new Dimension(250, 600));

        JLabel profileLabel = new JLabel("Вы: " + username);
        profileLabel.setForeground(Color.WHITE);
        profileLabel.setBorder(new EmptyBorder(10, 10, 10, 10));

        contactsPanel.add(profileLabel);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setBackground(new Color(40, 40, 40));
        userList.setForeground(Color.WHITE);
        userList.setSelectionBackground(new Color(50, 200, 100));

        JScrollPane userScrollPane = new JScrollPane(userList);
        contactsPanel.add(userScrollPane);

        // ---------------------- Панель сообщений (правая панель) ----------------------
        JPanel chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());
        chatPanel.setBackground(new Color(20, 20, 20));

        JLabel chatTitle = new JLabel("Выберите собеседника", JLabel.CENTER);
        chatTitle.setForeground(Color.WHITE);
        chatTitle.setBackground(new Color(30, 30, 30));
        chatTitle.setOpaque(true);
        chatTitle.setBorder(new EmptyBorder(10, 0, 10, 0));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(25, 25, 25));
        chatArea.setForeground(Color.WHITE);
        chatArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        chatPanel.add(chatTitle, BorderLayout.NORTH);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        // ---------------------- Панель ввода сообщений ----------------------
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(30, 30, 30));

        messageField = new JTextField();
        JButton sendButton = new JButton("➤");

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        // ---------------------- Добавление панелей в общий макет ----------------------
        add(contactsPanel, BorderLayout.WEST);
        add(chatPanel, BorderLayout.CENTER);

        // Добавление событий
        messageField.addActionListener(e -> sendMessage());
        sendButton.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        if (selectedUser == null || selectedUser.equals("Вы: " + username)) {
            chatArea.append("⚠️ Пожалуйста, выберите корректного собеседника из списка.\n");
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

        MessagePacket msgPacket = new MessagePacket(connection.getCurrentUserId(), correspondentId, text);
        connection.sendPacket(msgPacket);

        displayOutgoingMessage("Me to " + selectedUser + ": " + text);
        messageField.setText("");
    }

    public void displayIncomingPacket(Packet packet) {
        System.out.println("📩 [ChatWindow] Пакет получен: " + packet.getType());

        if (packet instanceof ListPacket listPacket) {
            updateUserList(listPacket);
        }

        if (packet instanceof MessagePacket) {
            MessagePacket msg = (MessagePacket) packet;
            displayIncomingMessage("📩 Сообщение от пользователя ID " + msg.senderId + ": " + msg.text);
        }

        if (packet instanceof WelcomePacket) {
            chatArea.append("✅ Авторизация успешна. Список пользователей обновляется...\n");
            connection.sendPacket(new RequestUserListPacket());  // ✅ Повторный запрос списка пользователей
        }

    }

    private void updateUserList(ListPacket listPacket) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            userListModel.addElement("Вы: " + username); // Добавляем себя в список
            userIdMap.clear();

            if (listPacket == null || listPacket.items.isEmpty()) {
                chatArea.append("❗️Нет доступных пользователей для диалога.\n");
                System.out.println("❗️ [ChatWindow] Получен пустой список пользователей!");
                return;
            }

            for (ListPacket.CorrespondentItem item : listPacket.items) {
                if (!item.login.equals(username)) {  // Исключаем текущего пользователя
                    userListModel.addElement(item.login);
                    userIdMap.put(item.login, item.id);
                    System.out.println("➕ [ChatWindow] Добавлен пользователь в список: " + item.login);
                }
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
