package client;

import client.LoginWindow;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

public class ClientMain {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new LoginWindow().setVisible(true);
        });
    }
}
