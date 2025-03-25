package client;

import client.ClientConnection;
import client.ChatWindow;
import server.HiPacket;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class LoginWindow extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    // Хранение пользователей и их паролей
    private final Map<String, String> validUsers = new HashMap<>() {{
        put("User1", "password1");
        put("User2", "password2");
        put("User3", "password3");
    }};

    public LoginWindow() {
        setTitle("Login");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        JButton loginButton = new JButton("Login");
        panel.add(loginButton);

        add(panel, BorderLayout.CENTER);

        loginButton.addActionListener(e -> handleLogin());
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введите имя пользователя и пароль.");
            return;
        }

        // Проверка логина и пароля
        if (!validUsers.containsKey(username) || !validUsers.get(username).equals(password)) {
            JOptionPane.showMessageDialog(this, "Неверный логин или пароль.");
            return;
        }

        try {
            ClientConnection connection = new ClientConnection("localhost", 20000, packet -> {
                if (ChatWindow.getInstance() != null) {
                    ChatWindow.getInstance().displayIncomingPacket(packet);
                }
            });

            connection.start();

            HiPacket hiPacket = new HiPacket();
            hiPacket.login = username;
            hiPacket.password = password;

            connection.sendPacket(hiPacket);

            ChatWindow chatWindow = new ChatWindow(connection, username);
            chatWindow.setVisible(true);
            this.dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка подключения: " + ex.getMessage());
        }

    }
}
