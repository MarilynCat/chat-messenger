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
import java.awt.geom.RoundRectangle2D;

public class ChatWindow extends JFrame {
    private static ChatWindow instance;
    private JTextField messageField;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private ClientConnection connection;
    private String username;
    private String selectedUser;
    private final Map<String, Integer> userIdMap = new HashMap<>();
    private JPanel chatMessagesPanel;
    private JLabel chatTitle;

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
        connection.sendPacket(new RequestUserListPacket());

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selected = userList.getSelectedValue();
                if (selected != null && !selected.startsWith("Вы: ")) {
                    selectedUser = selected;
                    chatTitle.setText(selectedUser); // ✅ заголовок обновляется
                    addMessageBubble("💬 Начат диалог с " + selectedUser, false);


                    if (!userIdMap.containsKey(selectedUser)) {
                        addMessageBubble("❌ Ошибка: Собеседник не найден в системе.", false);
                    }
                }
            }
        });
    }

    private void initUI() {
        setLayout(new BorderLayout());

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

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(new Color(20, 20, 20));

        chatTitle = new JLabel("Выберите собеседника", JLabel.CENTER);
        chatTitle.setForeground(Color.WHITE);
        chatTitle.setBackground(new Color(30, 30, 30));
        chatTitle.setOpaque(true);
        chatTitle.setBorder(new EmptyBorder(10, 0, 10, 0));

        chatMessagesPanel = new JPanel();
        chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
        chatMessagesPanel.setBackground(new Color(25, 25, 25));

        JScrollPane chatScrollPane = new JScrollPane(chatMessagesPanel);
        chatScrollPane.setBorder(null);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        chatPanel.add(chatTitle, BorderLayout.NORTH);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(30, 30, 30));

        messageField = new JTextField();
        JButton sendButton = new JButton("➤");

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        add(contactsPanel, BorderLayout.WEST);
        add(chatPanel, BorderLayout.CENTER);

        messageField.addActionListener(e -> sendMessage());
        sendButton.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        if (selectedUser == null || selectedUser.equals("Вы: " + username) || !userIdMap.containsKey(selectedUser)) {
            addMessageBubble("⚠️ Пожалуйста, выберите корректного собеседника из списка.", false);
            return;
        }

        Integer correspondentId = userIdMap.get(selectedUser);
        if (correspondentId == null || correspondentId == -1) {
            addMessageBubble("❌ Ошибка: Собеседник не найден или оффлайн.", false);
            return;
        }

        MessagePacket msgPacket = new MessagePacket(connection.getCurrentUserId(), correspondentId, text);
        connection.sendPacket(msgPacket);

        displayOutgoingMessage(text);
        messageField.setText("");
    }

    public void displayIncomingPacket(Packet packet) {
        System.out.println("📩 [ChatWindow] Пакет получен: " + packet.getType());

        if (packet instanceof ListPacket listPacket) {
            updateUserList(listPacket);
        }

        if (packet instanceof MessagePacket msg) {
            displayIncomingMessage(msg.text);
        }
    }

    private void updateUserList(ListPacket listPacket) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            userListModel.addElement("Вы: " + username);
            userIdMap.clear();

            if (listPacket == null || listPacket.items.isEmpty()) {
                addMessageBubble("❗️Нет доступных пользователей для диалога.", false);
                return;
            }

            for (ListPacket.CorrespondentItem item : listPacket.items) {
                if (!item.login.equals(username)) {
                    userListModel.addElement(item.login);
                    userIdMap.put(item.login, item.id);
                }
            }

            addMessageBubble("✅ Список пользователей обновлён.", false);
        });
    }

    public void displayIncomingMessage(String message) {
        SwingUtilities.invokeLater(() -> addMessageBubble(message, false));
    }

    public void displayOutgoingMessage(String message) {
        SwingUtilities.invokeLater(() -> addMessageBubble(message, true));
    }

    private void addMessageBubble(String text, boolean outgoing) {
        JPanel bubbleWrapper = new JPanel();
        bubbleWrapper.setLayout(new BoxLayout(bubbleWrapper, BoxLayout.X_AXIS));
        bubbleWrapper.setOpaque(false);
        bubbleWrapper.setBorder(new EmptyBorder(5, 10, 5, 10));

        ChatBubbleArea bubble = new ChatBubbleArea(text, outgoing);
        bubble.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));

        if (outgoing) {
            bubbleWrapper.add(Box.createHorizontalGlue());
            bubbleWrapper.add(bubble);
        } else {
            bubbleWrapper.add(bubble);
            bubbleWrapper.add(Box.createHorizontalGlue());
        }

        chatMessagesPanel.add(bubbleWrapper);
        chatMessagesPanel.revalidate();
        chatMessagesPanel.repaint();

        JScrollBar vertical = ((JScrollPane) chatMessagesPanel.getParent().getParent()).getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    public static ChatWindow getInstance() {
        return instance;
    }
}

// 👇 Добавь после класса ChatWindow (в этом же файле):

// Было:
// - setBorder(...) → 10, 15, 10, 15 (лишний padding справа у входящих)
// - setMaximumSize(..., Integer.MAX_VALUE) → баблы растягиваются по высоте
// - outgoing хвостик кривой: не выровнен по нижнему краю

// Стало:
class ChatBubbleArea extends JTextArea {
    private final boolean outgoing;

    public ChatBubbleArea(String text, boolean outgoing) {
        super(text);
        this.outgoing = outgoing;
        setLineWrap(true);
        setWrapStyleWord(true);
        setEditable(false);
        setFont(new Font("Arial", Font.PLAIN, 14));
        setBackground(outgoing ? new Color(0x25D366) : new Color(0x2A2A2A));
        setForeground(outgoing ? Color.BLACK : Color.WHITE);
        if (outgoing) {
            setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 20));
        } else {
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 15));
        }
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        // ✅ предотвращаем растягивание по высоте
        Dimension preferred = super.getPreferredSize();
        preferred.width = Math.min(preferred.width, 400);
        return preferred;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());

        int arc = 20;
        int w = getWidth();
        int h = getHeight();
        int tailSize = 10;

        RoundRectangle2D.Float bubble = new RoundRectangle2D.Float(
                outgoing ? 0 : tailSize,
                0,
                w - tailSize,
                h,
                arc, arc
        );
        g2.fill(bubble);

        Polygon tail = new Polygon();
        if (outgoing) {
            int x = w - 1;
            int y = h - 15;
            tail.addPoint(x - tailSize, y);
            tail.addPoint(x, y + 5);
            tail.addPoint(x - tailSize, y + 10);
        } else {
            tail.addPoint(0, 10);
            tail.addPoint(tailSize, 5);
            tail.addPoint(tailSize, 20);
        }
        g2.fillPolygon(tail);

        g2.dispose();
        super.paintComponent(g);
    }
}





