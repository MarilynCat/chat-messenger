package client;

import client.ClientConnection;
import server.*;
import server.packets.RequestUserListPacket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.border.AbstractBorder;
import java.net.URL;

public class ChatWindow extends JFrame {
    private static ChatWindow instance;
    private JTextArea messageField;
    private JList<String> userList;
    private DefaultListModel<String> userListModel = new DefaultListModel<>();
    private ClientConnection connection;
    private String username;
    private String selectedUser;
    private final Map<String, Integer> userIdMap = new HashMap<>();
    private final Map<String, String> lastMessages = new HashMap<>();
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
        setVisible(true);

        userListModel.addElement("Вы: " + username);
        connection.sendPacket(new RequestUserListPacket());

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selected = userList.getSelectedValue();
                if (selected != null) {
                    selectedUser = selected.startsWith("Вы: ") ? selected.substring(4) : selected;
                    chatTitle.setText(selectedUser);
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

        userList = new JList<>(userListModel);
        userList.setCellRenderer(new ContactListRenderer());
        userList.setBackground(new Color(40, 40, 40));
        userList.setSelectionBackground(new Color(50, 200, 100));
        userList.setFixedCellHeight(60);

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
        chatMessagesPanel.setOpaque(false); // важный момент!
        chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
        chatMessagesPanel.setOpaque(false); // обязательно


        chatMessagesPanel.setBorder(new EmptyBorder(0, 0, 60, 0)); // отступ снизу, чтобы не заезжали под input


        // Загружаем фон
        Image bgImage = new ImageIcon(getClass().getResource("/icons/chat_background.png")).getImage();

// Создаём кастомный viewport
        JViewport customViewport = new JViewport() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    int iw = bgImage.getWidth(null);
                    int ih = bgImage.getHeight(null);
                    if (iw > 0 && ih > 0) {
                        for (int x = 0; x < getWidth(); x += iw) {
                            for (int y = 0; y < getHeight(); y += ih) {
                                g.drawImage(bgImage, x, y, this);
                            }
                        }
                    }
                }
            }
        };
        customViewport.setOpaque(false);

// ScrollPane с кастомным viewport
        JScrollPane chatScrollPane = new JScrollPane();
        chatScrollPane.setViewport(customViewport);               // <-- тут главное изменение
        chatScrollPane.setViewportView(chatMessagesPanel);
        chatScrollPane.setOpaque(false);
        chatScrollPane.getViewport().setOpaque(false);
        chatScrollPane.setBorder(null);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);


        chatPanel.add(chatTitle, BorderLayout.NORTH);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);


// 1) Нижняя панель с полностью прозрачным фоном и минимальными отступами
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setOpaque(false); // прозрачный фон
        inputPanel.setBorder(new EmptyBorder(5, 8, 5, 8)); // чуть-чуть отступов по бокам
        // inputPanel.setPreferredSize(new Dimension(0, 45)); // высота всей нижней панели

// 2) Иконка «прикрепить файл» слева
        URL attachIconUrl = getClass().getResource("/icons/attach_icon.png");
        Icon attachIcon = attachIconUrl != null ? new ImageIcon(attachIconUrl) : null;
        JLabel attachLabel = new JLabel(attachIcon != null ? attachIcon : new ImageIcon()); // или можно new JLabel("+")
        attachLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));


// 3) Иконка «отправить» справа
        URL sendIconUrl = getClass().getResource("/icons/send_icon.png");
        Icon sendIcon = sendIconUrl != null ? new ImageIcon(sendIconUrl) : null;
        JButton sendButton = new JButton(sendIcon != null ? sendIcon : new ImageIcon()); // или new JButton("→")
        sendButton.setBorder(null);
        sendButton.setContentAreaFilled(false);
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

/// 4) Поле ввода (многострочное, прозрачное, без фона и границ)
        JTextArea messageFieldArea = new JTextArea("Сообщение", 1, 20);
        messageFieldArea.setRows(1);
        messageFieldArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120)); // ограничим максимум
        this.messageField = messageFieldArea; // сохраняем в поле для обратной совместимости
        messageFieldArea.setFont(new Font("Arial", Font.PLAIN, 14));
        messageFieldArea.setForeground(new Color(255, 255, 255, 204));
        messageFieldArea.setCaretColor(Color.WHITE);
        messageFieldArea.setOpaque(false);
        messageFieldArea.setLineWrap(true);
        messageFieldArea.setWrapStyleWord(true);
        // Автоматическая подстройка высоты text area
        messageFieldArea.setRows(1);
        messageFieldArea.setLineWrap(true);
        messageFieldArea.setWrapStyleWord(true);

        messageFieldArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void updateSize() {
                int lineCount = messageFieldArea.getLineCount();
                int lineHeight = messageFieldArea.getFontMetrics(messageFieldArea.getFont()).getHeight();
                int newHeight = lineHeight * lineCount + 20; // с отступами

                // Применяем высоту
                messageFieldArea.setPreferredSize(new Dimension(messageFieldArea.getWidth(), newHeight));
                messageFieldArea.revalidate();
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSize(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSize(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSize(); }
        });

        // Автоматическое изменение высоты поля ввода
        messageFieldArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void updateSize() {
                messageFieldArea.setRows(Math.min(6, messageFieldArea.getLineCount()));
                messageFieldArea.revalidate();
                messageFieldArea.repaint();
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSize(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSize(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSize(); }
        });

        messageFieldArea.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));


// 5) Плейсхолдер
        messageFieldArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (messageFieldArea.getText().equals("Сообщение")) {
                    messageFieldArea.setText("");
                    messageFieldArea.setForeground(Color.WHITE);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (messageFieldArea.getText().trim().isEmpty()) {
                    messageFieldArea.setText("Сообщение");
                    messageFieldArea.setForeground(new Color(255, 255, 255, 204));
                }
            }
        });

// 6) Скруглённая обёртка
        JPanel roundedWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(60, 60, 60)); // фон
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); // скруглённый прямоугольник
                g2.dispose();
                super.paintComponent(g);
            }
        };
        roundedWrapper.setOpaque(false); // фон рисуем вручную
        roundedWrapper.add(messageFieldArea, BorderLayout.CENTER);


// 7) Добавляем в нижнюю панель
        // Создаём fieldPanel и добавляем в него иконки и обёртку с messageField
        JPanel fieldPanel = new JPanel(new BorderLayout(10, 0));
        fieldPanel.setOpaque(false);
        fieldPanel.add(attachLabel, BorderLayout.WEST);
        fieldPanel.add(roundedWrapper, BorderLayout.CENTER); // <-- тут именно roundedWrapper
        fieldPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(fieldPanel, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

// 8) Подключаем действия
        InputMap inputMap = messageFieldArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = messageFieldArea.getActionMap();

// Shift+Enter — вставка новой строки (дефолтное поведение, ничего не меняем)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "insert-break");
        actionMap.put("insert-break", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                messageFieldArea.append("\n");
            }
        });

// Enter без Shift — отправка
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send-message");
        actionMap.put("send-message", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });


        sendButton.addActionListener(e -> sendMessage());

        // Добавляем основные панели в окно
        add(contactsPanel, BorderLayout.WEST);
        add(chatPanel, BorderLayout.CENTER);
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
        SwingUtilities.invokeLater(() -> {
            addMessageBubble(message, false);
            updateLastMessagePreview(selectedUser, message);
        });
    }

    public void displayOutgoingMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            addMessageBubble(message, true);
            updateLastMessagePreview(selectedUser, message);
        });
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

    public void updateLastMessagePreview(String user, String message) {
        lastMessages.put(user, message);
        userList.repaint();
    }

    public String getLastMessagePreviewForUser(String user) {
        return lastMessages.getOrDefault(user, "");
    }

    public static ChatWindow getInstance() {
        return instance;
    }
}

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

        // Исправлено: бабл расширен на 1px в сторону хвоста
        RoundRectangle2D.Float bubble = new RoundRectangle2D.Float(
                outgoing ? 0 : tailSize - 1,
                0,
                w - tailSize + 1,
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
            int y = 10;
            tail.addPoint(0, y);
            tail.addPoint(tailSize, y - 5);
            tail.addPoint(tailSize, y + 10);
        }
        g2.fillPolygon(tail);

        g2.dispose();
        super.paintComponent(g);
    }

}

class ContactListRenderer extends JPanel implements ListCellRenderer<String> {
    private final JLabel avatarLabel = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private final JLabel previewLabel = new JLabel();
    private final JSeparator divider = new JSeparator();

    public ContactListRenderer() {
        setLayout(new BorderLayout(10, 0));
        setBackground(new Color(40, 40, 40));
        setBorder(new EmptyBorder(5, 10, 5, 10));

        avatarLabel.setPreferredSize(new Dimension(36, 36));
        avatarLabel.setMinimumSize(new Dimension(36, 36));
        avatarLabel.setMaximumSize(new Dimension(36, 36));
        avatarLabel.setOpaque(false);
        avatarLabel.setBackground(new Color(100, 100, 100)); // цвет круга
        avatarLabel.setBorder(null);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatarLabel.setUI(new javax.swing.plaf.basic.BasicLabelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Определяем размеры компонента
                int w = c.getWidth();
                int h = c.getHeight();
                // Берём минимальный размер, чтобы круг не становился эллипсом
                int size = Math.min(w, h);

                // Вычисляем координаты, чтобы круг был по центру
                int x = (w - size) / 2;
                int y = (h - size) / 2;

                // Заливаем круг цветом фона
                g2.setColor(avatarLabel.getBackground());
                g2.fillOval(x, y, size, size);

                // Отрисовываем текст (букву) поверх круга
                super.paint(g, c);

                g2.dispose();
            }

        });



        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);
        previewLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        previewLabel.setForeground(Color.LIGHT_GRAY);
        textPanel.add(nameLabel);
        textPanel.add(previewLabel);

        add(avatarLabel, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);

        // Настраиваем divider, но не скрываем/показываем
        divider.setPreferredSize(new Dimension(1, 1));
        divider.setBackground(new Color(60, 60, 60));
        add(divider, BorderLayout.SOUTH);
    }


    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        // Получаем логин, убираем префикс "Вы: " если он есть
        String login = value.startsWith("Вы: ") ? value.substring(4) : value;
        nameLabel.setText(login);

        // Настраиваем аватар: первая буква имени, фон и граница
        avatarLabel.setText(login.substring(0, 1).toUpperCase());
        avatarLabel.setBackground(new Color(100, 100, 100));
// Убираем обводку, чтобы сохранить круглый вид:
        avatarLabel.setBorder(null);


        // Обновляем фон всего элемента в зависимости от выделения
        Color hoverColor = new Color(146,146,146,102); // #929292, alpha ~ 40%
        Color normalColor = new Color(40,40,40);

        if (isSelected) {
            setBackground(hoverColor);
        } else {
            setBackground(normalColor);
        }


        // Получаем превью последнего сообщения и обрезаем, если слишком длинное
        String preview = ChatWindow.getInstance().getLastMessagePreviewForUser(login);
        if (preview != null && preview.length() > 40) {
            preview = preview.substring(0, 40) + "...";
        }
        previewLabel.setText(preview != null ? preview : " ");

        // Показываем разделитель, если элемент не последний
        divider.setOrientation(SwingConstants.HORIZONTAL);
        divider.setPreferredSize(new Dimension(1, 1));
        divider.setBackground(new Color(60, 60, 60));
        divider.setForeground(new Color(60, 60, 60));
        divider.setVisible(true); // всегда виден
        add(divider, BorderLayout.SOUTH);


        return this;
    }

}

class RoundedBorder extends AbstractBorder {
    private final int radius;

    public RoundedBorder(int radius) {
        this.radius = radius;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Цвет и толщина рамки (можно настроить по вкусу)
        g2.setColor(new Color(255, 255, 255, 128));
        g2.setStroke(new BasicStroke(1f));

        // Рисуем скруглённый прямоугольник
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);

        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(radius, radius, radius, radius);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.right = insets.top = insets.bottom = radius;
        return insets;
    }
}
