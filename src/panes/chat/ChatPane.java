package panes.chat;

import main.GameScene;
import network.client.Client;
import network.client.Protocol;
import network.entitiesNet.PlayerMP;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class ChatPane extends JPanel implements ActionListener {
    private JButton submitButton;
    private JTextField chatField;
    private GameScene gameScene;
    private PlayerMP playerMP;
    private Client client;

    private BufferedImage chatImage;

    public ChatPane(GameScene gameScene) {
        this.gameScene = gameScene;
        this.playerMP = gameScene.getPlayerMP();
        client = Client.getGameClient();
        
        init();
    }
    
    private void init() {
        setOpaque(false);
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Main container with rounded corners
        JPanel container = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Shadow
                g2d.setColor(new Color(0, 0, 0, 40));
                g2d.fillRoundRect(3, 3, getWidth() - 3, getHeight() - 3, 15, 15);
                
                // Background gradient
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(60, 60, 70, 240),
                    0, getHeight(), new Color(40, 40, 50, 240)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 15, 15);
                
                // Border
                g2d.setColor(new Color(100, 100, 120));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(1, 1, getWidth() - 5, getHeight() - 5, 15, 15);
            }
        };
        container.setOpaque(false);
        container.setLayout(new BorderLayout(8, 8));
        container.setBorder(new EmptyBorder(12, 12, 12, 12));
        
        // Title label
        JLabel titleLabel = new JLabel("Chat");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(new Color(200, 200, 200));
        container.add(titleLabel, BorderLayout.NORTH);
        
        // Chat input field with custom styling
        chatField = new JTextField(25) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                g2d.setColor(new Color(255, 255, 255, 250));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                super.paintComponent(g);
            }
        };
        chatField.setOpaque(false);
        chatField.setFont(new Font("Arial", Font.PLAIN, 14));
        chatField.setForeground(new Color(30, 30, 30));
        chatField.setCaretColor(new Color(76, 175, 80));
        chatField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(76, 175, 80), 2, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        
        // Press Enter to send
        chatField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        
        container.add(chatField, BorderLayout.CENTER);
        
        // Submit button with custom styling
        submitButton = new JButton("Send") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color bgColor;
                if (getModel().isPressed()) {
                    bgColor = new Color(56, 142, 60);
                } else if (getModel().isRollover()) {
                    bgColor = new Color(102, 187, 106);
                } else {
                    bgColor = new Color(76, 175, 80);
                }
                
                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                // Text
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 13));
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), textX, textY);
            }
        };
        submitButton.setPreferredSize(new Dimension(70, 35));
        submitButton.setContentAreaFilled(false);
        submitButton.setBorderPainted(false);
        submitButton.setFocusPainted(false);
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.addActionListener(this);
        
        container.add(submitButton, BorderLayout.EAST);
        
        add(container, BorderLayout.CENTER);
    }
    
    private void sendMessage() {
        String message = chatField.getText().trim();
        if (!message.isEmpty()) {
            // Load the chat bubble image
            playerMP.setChatImage(playerMP.getDialogText().loadImage(message));
            
            // Send the message to the server
            client.sendToServer(new Protocol().chatPacket(gameScene.getPlayerMP().getUsername(), message));
            
            chatField.setText("");
        }
        gameScene.requestFocusInWindow();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == submitButton) {
            sendMessage();
        }
    }

    public BufferedImage getChatImage() {
        return chatImage;
    }

    public void setChatImage(BufferedImage chatImage) {
        this.chatImage = chatImage;
    }

    public void drawChatImage() {
        gameScene.getGraphics().drawImage(chatImage, 0, 0, null);
    }
}
