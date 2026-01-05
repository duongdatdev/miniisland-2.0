package panes.shop;

import network.client.WebSocketGameClient;
import network.client.Protocol;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Shop panel để mua và đổi skin
 */
public class ShopPane extends JPanel {
    
    private WebSocketGameClient client;
    private Protocol protocol;
    private JTabbedPane tabbedPane;
    private JPanel shopPanel;
    private JPanel mySkinsPanel;
    private JLabel coinsLabel;
    private int playerCoins = 0;
    
    private List<SkinItem> shopSkins = new ArrayList<>();
    private List<PlayerSkin> mySkins = new ArrayList<>();
    private String currentSkinFolder = "1";
    
    // Listener để thông báo khi đổi skin
    private SkinChangeListener skinChangeListener;
    
    // Colors
    private Color bgColor = new Color(30, 30, 40);
    private Color panelColor = new Color(45, 45, 60);
    private Color accentColor = new Color(100, 150, 255);
    private Color goldColor = new Color(255, 215, 0);
    private Color successColor = new Color(100, 255, 100);
    
    public interface SkinChangeListener {
        void onSkinChanged(String skinFolder);
    }
    
    public ShopPane(WebSocketGameClient client) {
        this.client = client;
        this.protocol = new Protocol();
        
        setLayout(new BorderLayout());
        setBackground(bgColor);
        setPreferredSize(new Dimension(550, 450));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        initUI();
        requestShopData();
    }
    
    public void setSkinChangeListener(SkinChangeListener listener) {
        this.skinChangeListener = listener;
    }
    
    private void initUI() {
        // Header with gradient-style background
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(35, 35, 50));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, accentColor),
            new EmptyBorder(5, 10, 10, 10)
        ));
        
        // Title - simple text without symbols
        JLabel titleLabel = new JLabel("SKIN SHOP");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // Coins display
        JPanel coinsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        coinsPanel.setBackground(new Color(45, 50, 65));
        coinsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(goldColor, 2),
            new EmptyBorder(5, 12, 5, 12)
        ));
        
        coinsLabel = new JLabel("0");
        coinsLabel.setForeground(goldColor);
        coinsLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        coinsPanel.add(coinsLabel);
        headerPanel.add(coinsPanel, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Tabs with custom styling
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(panelColor);
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        // Shop tab
        shopPanel = new JPanel();
        shopPanel.setLayout(new GridLayout(0, 3, 12, 12));
        shopPanel.setBackground(panelColor);
        shopPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane shopScroll = new JScrollPane(shopPanel);
        shopScroll.setBackground(panelColor);
        shopScroll.getViewport().setBackground(panelColor);
        shopScroll.setBorder(null);
        shopScroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollPane(shopScroll);
        tabbedPane.addTab("Shop", shopScroll);
        
        // My Skins tab  
        mySkinsPanel = new JPanel();
        mySkinsPanel.setLayout(new GridLayout(0, 3, 12, 12));
        mySkinsPanel.setBackground(panelColor);
        mySkinsPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane mySkinsScroll = new JScrollPane(mySkinsPanel);
        mySkinsScroll.setBackground(panelColor);
        mySkinsScroll.getViewport().setBackground(panelColor);
        mySkinsScroll.setBorder(null);
        mySkinsScroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollPane(mySkinsScroll);
        tabbedPane.addTab("My Skins", mySkinsScroll);
        
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) {
                requestMySkins();
            }
        });
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Bottom panel with refresh button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        bottomPanel.setBackground(bgColor);
        bottomPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setBackground(accentColor);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> requestShopData());
        bottomPanel.add(refreshBtn);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Style scrollpane with dark theme
     */
    private void styleScrollPane(JScrollPane scrollPane) {
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();
        
        // Dark scrollbar styling
        Color scrollBg = new Color(35, 35, 50);
        Color scrollThumb = new Color(80, 80, 100);
        
        verticalBar.setBackground(scrollBg);
        verticalBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = scrollThumb;
                this.trackColor = scrollBg;
            }
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }
        });
        
        horizontalBar.setBackground(scrollBg);
        horizontalBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = scrollThumb;
                this.trackColor = scrollBg;
            }
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }
        });
    }
    
    private void requestShopData() {
        if (client != null && client.isOpen()) {
            client.sendMessage(protocol.getSkinsPacket());
            client.sendMessage(protocol.getCoinsPacket());
            client.sendMessage(protocol.getEquippedSkinPacket());
        }
    }
    
    private void requestMySkins() {
        if (client != null && client.isOpen()) {
            client.sendMessage(protocol.getMySkinsPacket());
        }
    }
    
    /**
     * Parse danh sách skins từ server
     * Format: SkinsList,id|name|desc|price|folder|isDefault,...
     */
    public void parseSkinsList(String data) {
        shopSkins.clear();
        String[] parts = data.split(",");
        
        for (int i = 1; i < parts.length; i++) {
            String[] skinData = parts[i].split("\\|");
            if (skinData.length >= 6) {
                try {
                    SkinItem skin = new SkinItem();
                    skin.id = Integer.parseInt(skinData[0]);
                    skin.name = skinData[1];
                    skin.description = skinData[2];
                    skin.price = Integer.parseInt(skinData[3]);
                    skin.skinFolder = skinData[4];
                    skin.isDefault = skinData[5].equals("1");
                    shopSkins.add(skin);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        SwingUtilities.invokeLater(this::updateShopPanel);
    }
    
    /**
     * Parse skins của người chơi
     * Format: PlayerSkins,id|name|desc|folder|isEquipped,...
     */
    public void parsePlayerSkins(String data) {
        mySkins.clear();
        String[] parts = data.split(",");
        
        for (int i = 1; i < parts.length; i++) {
            String[] skinData = parts[i].split("\\|");
            if (skinData.length >= 5) {
                try {
                    PlayerSkin skin = new PlayerSkin();
                    skin.id = Integer.parseInt(skinData[0]);
                    skin.name = skinData[1];
                    skin.description = skinData[2];
                    skin.skinFolder = skinData[3];
                    skin.isEquipped = skinData[4].equals("1");
                    mySkins.add(skin);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        SwingUtilities.invokeLater(this::updateMySkinsPanel);
    }
    
    /**
     * Parse coins
     */
    public void parseCoins(String data) {
        try {
            String[] parts = data.split(",");
            if (parts.length >= 2) {
                playerCoins = Integer.parseInt(parts[1]);
                SwingUtilities.invokeLater(() -> coinsLabel.setText(String.valueOf(playerCoins)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Parse equipped skin
     */
    public void parseEquippedSkin(String data) {
        String[] parts = data.split(",");
        if (parts.length >= 2) {
            currentSkinFolder = parts[1];
            
            // Update local skins list
            boolean changed = false;
            for (PlayerSkin skin : mySkins) {
                boolean wasEquipped = skin.isEquipped;
                skin.isEquipped = skin.skinFolder.equals(currentSkinFolder);
                if (wasEquipped != skin.isEquipped) {
                    changed = true;
                }
            }
            
            // Notify listener
            if (skinChangeListener != null) {
                SwingUtilities.invokeLater(() -> skinChangeListener.onSkinChanged(currentSkinFolder));
            }
            
            // Refresh UI if needed
            if (changed) {
                SwingUtilities.invokeLater(this::updateMySkinsPanel);
            }
        }
    }
    
    /**
     * Parse buy result
     */
    public void parseBuyResult(String data) {
        String[] parts = data.split(",");
        if (parts.length >= 4) {
            boolean success = parts[1].equals("success");
            String message = parts[2];
            int newBalance = Integer.parseInt(parts[3]);
            
            SwingUtilities.invokeLater(() -> {
                playerCoins = newBalance;
                coinsLabel.setText(String.valueOf(playerCoins));
                
                JOptionPane.showMessageDialog(this, 
                    message, 
                    success ? "Success!" : "Failed",
                    success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                
                if (success) {
                    requestShopData();
                }
            });
        }
    }
    
    private void updateShopPanel() {
        shopPanel.removeAll();
        
        for (SkinItem skin : shopSkins) {
            shopPanel.add(createSkinCard(skin));
        }
        
        shopPanel.revalidate();
        shopPanel.repaint();
    }
    
    private JPanel createSkinCard(SkinItem skin) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(55, 55, 75));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 110), 2),
            new EmptyBorder(12, 12, 12, 12)
        ));
        
        // Skin preview image with styled container
        JPanel previewContainer = new JPanel();
        previewContainer.setLayout(new BorderLayout());
        previewContainer.setBackground(new Color(40, 40, 55));
        previewContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80), 1),
            new EmptyBorder(8, 8, 8, 8)
        ));
        previewContainer.setMaximumSize(new Dimension(80, 80));
        previewContainer.setPreferredSize(new Dimension(80, 80));
        previewContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel previewLabel = new JLabel();
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setVerticalAlignment(SwingConstants.CENTER);
        try {
            BufferedImage img = ImageIO.read(getClass().getResource("/player/" + skin.skinFolder + "/Character_Stand.png"));
            if (img != null) {
                // Get only the first frame from sprite sheet (divide width by 4)
                int frameWidth = img.getWidth() / 4;
                int frameHeight = img.getHeight();
                BufferedImage firstFrame = img.getSubimage(0, 0, frameWidth, frameHeight);
                Image scaled = firstFrame.getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception e) {
            // Create styled placeholder for missing skins
            previewLabel.setText("?");
            previewLabel.setForeground(new Color(150, 150, 180));
            previewLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        }
        previewContainer.add(previewLabel, BorderLayout.CENTER);
        
        // Center the preview container
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerPanel.setBackground(new Color(55, 55, 75));
        centerPanel.add(previewContainer);
        card.add(centerPanel);
        card.add(Box.createVerticalStrut(8));
        
        // Name with better styling
        JLabel nameLabel = new JLabel(skin.name);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(nameLabel);
        
        // Description
        JLabel descLabel = new JLabel("<html><center>" + skin.description + "</center></html>");
        descLabel.setForeground(new Color(170, 170, 190));
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(descLabel);
        card.add(Box.createVerticalStrut(8));
        
        // Price / Buy button
        if (skin.price == 0) {
            JLabel freeLabel = new JLabel("FREE");
            freeLabel.setForeground(successColor);
            freeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            freeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(freeLabel);
        } else {
            JButton buyBtn = new JButton(skin.price + " coins");
            buyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            boolean canAfford = playerCoins >= skin.price;
            buyBtn.setBackground(canAfford ? new Color(70, 130, 80) : new Color(80, 80, 90));
            buyBtn.setForeground(canAfford ? Color.WHITE : new Color(150, 150, 150));
            buyBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            buyBtn.setFocusPainted(false);
            buyBtn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            buyBtn.setCursor(canAfford ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            buyBtn.setEnabled(canAfford);
            
            int skinId = skin.id;
            String skinName = skin.name;
            int price = skin.price;
            buyBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Buy " + skinName + " for " + price + " coins?",
                    "Confirm Purchase",
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION && client != null && client.isOpen()) {
                    client.sendMessage(protocol.buySkinPacket(skinId));
                }
            });
            card.add(buyBtn);
        }
        
        return card;
    }
    
    private void updateMySkinsPanel() {
        mySkinsPanel.removeAll();
        
        if (mySkins.isEmpty()) {
            JLabel emptyLabel = new JLabel("No skins owned yet!");
            emptyLabel.setForeground(Color.LIGHT_GRAY);
            emptyLabel.setFont(new Font("Arial", Font.ITALIC, 14));
            mySkinsPanel.add(emptyLabel);
        } else {
            for (PlayerSkin skin : mySkins) {
                mySkinsPanel.add(createMySkinCard(skin));
            }
        }
        
        mySkinsPanel.revalidate();
        mySkinsPanel.repaint();
    }
    
    private JPanel createMySkinCard(PlayerSkin skin) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        Color bgCol = skin.isEquipped ? new Color(45, 75, 55) : new Color(55, 55, 75);
        Color borderCol = skin.isEquipped ? successColor : new Color(80, 80, 110);
        card.setBackground(bgCol);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderCol, 2),
            new EmptyBorder(12, 12, 12, 12)
        ));
        
        // Preview with styled container
        JPanel previewContainer = new JPanel();
        previewContainer.setLayout(new BorderLayout());
        previewContainer.setBackground(new Color(40, 40, 55));
        previewContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80), 1),
            new EmptyBorder(8, 8, 8, 8)
        ));
        previewContainer.setMaximumSize(new Dimension(80, 80));
        previewContainer.setPreferredSize(new Dimension(80, 80));
        previewContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel previewLabel = new JLabel();
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setVerticalAlignment(SwingConstants.CENTER);
        try {
            BufferedImage img = ImageIO.read(getClass().getResource("/player/" + skin.skinFolder + "/Character_Stand.png"));
            if (img != null) {
                // Get only the first frame from sprite sheet (divide width by 4)
                int frameWidth = img.getWidth() / 4;
                int frameHeight = img.getHeight();
                BufferedImage firstFrame = img.getSubimage(0, 0, frameWidth, frameHeight);
                Image scaled = firstFrame.getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception e) {
            previewLabel.setText("?");
            previewLabel.setForeground(new Color(150, 150, 180));
            previewLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        }
        previewContainer.add(previewLabel, BorderLayout.CENTER);
        
        // Center the preview container
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerPanel.setBackground(bgCol);
        centerPanel.add(previewContainer);
        card.add(centerPanel);
        card.add(Box.createVerticalStrut(8));
        
        // Name + equipped status
        JLabel nameLabel = new JLabel(skin.name);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(8));
        
        // Equip button or Equipped label
        if (!skin.isEquipped) {
            JButton equipBtn = new JButton("Equip");
            equipBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            equipBtn.setBackground(accentColor);
            equipBtn.setForeground(Color.WHITE);
            equipBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            equipBtn.setFocusPainted(false);
            equipBtn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            equipBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            int skinId = skin.id;
            equipBtn.addActionListener(e -> {
                if (client != null && client.isOpen()) {
                    client.sendMessage(protocol.equipSkinPacket(skinId));
                }
            });
            card.add(equipBtn);
        } else {
            JLabel equippedLabel = new JLabel("EQUIPPED");
            equippedLabel.setForeground(successColor);
            equippedLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            equippedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(equippedLabel);
        }
        
        return card;
    }
    
    // Inner classes
    private static class SkinItem {
        int id;
        String name;
        String description;
        int price;
        String skinFolder;
        boolean isDefault;
    }
    
    private static class PlayerSkin {
        int id;
        String name;
        String description;
        String skinFolder;
        boolean isEquipped;
    }
}
