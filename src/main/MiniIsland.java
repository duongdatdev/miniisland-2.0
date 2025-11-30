package main;

import network.client.Client;
import network.client.ClientRecivingThread;
import network.client.Protocol;
import network.entitiesNet.PlayerMP;
import panes.auth.signIn.SignInControl;
import panes.auth.signIn.SignInModel;
import panes.auth.signIn.SignInPane;
import panes.auth.signUp.SignUpControl;
import panes.auth.signUp.SignUpModel;
import panes.auth.signUp.SignUpPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MiniIsland extends JFrame {
    private GameScene gameScene;
    private CardLayout cardLayout;

    //Network
    Socket socket = null;
    DataOutputStream writer = null;

    //Login
    private SignInPane signInPane;
    private SignInControl signInControl;
    private SignInModel signInModel;

    //Register
    private SignUpModel signUpModel;
    private SignUpPane signUpPane;
    private SignUpControl signUpControl;

    private Client client;
    private PlayerMP clientPlayer;

    public MiniIsland() {
        signInPane = new SignInPane();
        signInModel = SignInModel.getInstance();
        signInControl = new SignInControl(this, signInModel, signInPane);

        signUpPane = new SignUpPane();
        signUpModel = new SignUpModel();

        signUpControl = new SignUpControl(this, signUpModel, signUpPane);

        client = Client.getGameClient();

        cardLayout = new CardLayout();

        init();

        changePanel("SignInPanel");

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                showDialogExit();
            }
        });
        this.pack();
        this.setLocationRelativeTo(null);
    }

    public void startGame() {

        gameScene = GameScene.getInstance();
        this.add(gameScene, "GamePanel");

        actionRegister();

        gameScene.start();
        gameScene.setFocusable(true);
        gameScene.requestFocusInWindow();

        client = Client.getGameClient();
        
        clientPlayer = gameScene.getPlayerMP();
        ClientRecivingThread clientRecivingThread = new ClientRecivingThread(client.getWebSocketClient(), clientPlayer, gameScene);
        clientRecivingThread.start();

        client.sendToServer(new Protocol().HelloPacket(signInModel.getUsername()));
        
        // Request equipped skin after joining game
        client.sendToServer(new Protocol().getEquippedSkinPacket());

        changeToGamePanel();
    }

    private void sendToServer(String message) {
        if (message.equals("exit")) {
            System.exit(0);
        } else {
            try {
                writer.writeUTF(message);
            } catch (IOException ex) {
            }
        }
    }


    public void changePanel(String panelName) {
        cardLayout.show(this.getContentPane(), panelName);
    }

    public void changeToGamePanel() {
        cardLayout.show(this.getContentPane(), "GamePanel");
        this.pack();
        this.setLocationRelativeTo(null);
    }

    private void showDialogExit() {
        // Create custom styled dialog
        JDialog dialog = new JDialog(this, "Mini Island", true);
        dialog.setUndecorated(true);
        dialog.setSize(320, 150);
        dialog.setLocationRelativeTo(this);
        
        // Main panel with rounded corners effect
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background gradient
                GradientPaint gradient = new GradientPaint(0, 0, new Color(76, 175, 80), 0, getHeight(), new Color(56, 142, 60));
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                // Inner white panel
                g2d.setColor(new Color(255, 255, 255, 250));
                g2d.fillRoundRect(5, 40, getWidth() - 10, getHeight() - 45, 15, 15);
            }
        };
        mainPanel.setLayout(null);
        mainPanel.setPreferredSize(new Dimension(320, 150));
        
        // Title label
        JLabel titleLabel = new JLabel("Mini Island", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(0, 8, 320, 25);
        mainPanel.add(titleLabel);
        
        // Message label
        JLabel msgLabel = new JLabel("Are you sure you want to exit?", SwingConstants.CENTER);
        msgLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        msgLabel.setForeground(new Color(33, 33, 33));
        msgLabel.setBounds(0, 60, 320, 25);
        mainPanel.add(msgLabel);
        
        // Yes button
        JButton yesBtn = createStyledButton("Yes", new Color(76, 175, 80));
        yesBtn.setBounds(70, 100, 80, 35);
        yesBtn.addActionListener(e -> {
            dialog.dispose();
            if (clientPlayer != null) {
                Client.getGameClient().sendToServer(new Protocol().ExitMessagePacket(signInModel.getUsername()));
            }
            System.exit(0);
        });
        mainPanel.add(yesBtn);
        
        // No button
        JButton noBtn = createStyledButton("No", new Color(244, 67, 54));
        noBtn.setBounds(170, 100, 80, 35);
        noBtn.addActionListener(e -> dialog.dispose());
        mainPanel.add(noBtn);
        
        dialog.setContentPane(mainPanel);
        dialog.setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, 320, 150, 20, 20));
        dialog.setVisible(true);
        
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2d.setColor(bgColor.darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(bgColor.brighter());
                } else {
                    g2d.setColor(bgColor);
                }
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 13));
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), textX, textY);
            }
        };
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    public void actionRegister() {
        gameScene.setRunning(true);

        gameScene.start();
        gameScene.validate();
        gameScene.repaint();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        gameScene.setFocusable(true);
    }

    public GameScene getGameScene() {
        return gameScene;
    }

    private void init() {
        this.setTitle("Mini Island");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
//        this.setResizable(false);
        this.setLayout(cardLayout);

        this.add(signInPane, "SignInPanel");
        this.add(signUpPane, "SignUpPanel");
        this.setVisible(true);

    }

    public DataOutputStream getWriter() {
        return writer;
    }

    public void setWriter(DataOutputStream writer) {
        this.writer = writer;
    }
}
