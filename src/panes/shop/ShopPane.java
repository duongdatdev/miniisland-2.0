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
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(bgColor);
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        JLabel titleLabel = new JLabel("👕 SKIN SHOP");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        coinsLabel = new JLabel("💰 0");
        coinsLabel.setForeground(goldColor);
        coinsLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerPanel.add(coinsLabel, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(panelColor);
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 13));
        
        // Shop tab
        shopPanel = new JPanel();
        shopPanel.setLayout(new GridLayout(0, 3, 10, 10));
        shopPanel.setBackground(panelColor);
        shopPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane shopScroll = new JScrollPane(shopPanel);
        shopScroll.setBackground(panelColor);
        shopScroll.getViewport().setBackground(panelColor);
        shopScroll.setBorder(null);
        tabbedPane.addTab("🏪 Shop", shopScroll);
        
        // My Skins tab  
        mySkinsPanel = new JPanel();
        mySkinsPanel.setLayout(new GridLayout(0, 3, 10, 10));
        mySkinsPanel.setBackground(panelColor);
        mySkinsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane mySkinsScroll = new JScrollPane(mySkinsPanel);
        mySkinsScroll.setBackground(panelColor);
        mySkinsScroll.getViewport().setBackground(panelColor);
        mySkinsScroll.setBorder(null);
        tabbedPane.addTab("🎒 My Skins", mySkinsScroll);
        
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) {
                requestMySkins();
            }
        });
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Refresh button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(bgColor);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setBackground(accentColor);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> requestShopData());
        bottomPanel.add(refreshBtn);
        
        add(bottomPanel, BorderLayout.SOUTH);
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
                SwingUtilities.invokeLater(() -> coinsLabel.setText("💰 " + playerCoins));
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
            
            // Notify listener
            if (skinChangeListener != null) {
                SwingUtilities.invokeLater(() -> skinChangeListener.onSkinChanged(currentSkinFolder));
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
                coinsLabel.setText("💰 " + playerCoins);
                
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
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(55, 55, 75));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 90), 2),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        // Skin preview image
        JLabel previewLabel = new JLabel();
        previewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        try {
            BufferedImage img = ImageIO.read(getClass().getResource("/player/" + skin.skinFolder + "/Character_Stand.png"));
            if (img != null) {
                Image scaled = img.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception e) {
            previewLabel.setText("?");
            previewLabel.setForeground(Color.GRAY);
            previewLabel.setFont(new Font("Arial", Font.BOLD, 32));
        }
        previewLabel.setPreferredSize(new Dimension(64, 64));
        card.add(previewLabel);
        card.add(Box.createVerticalStrut(5));
        
        // Name
        JLabel nameLabel = new JLabel(skin.name);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(nameLabel);
        
        // Description
        JLabel descLabel = new JLabel("<html><center>" + skin.description + "</center></html>");
        descLabel.setForeground(Color.LIGHT_GRAY);
        descLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(descLabel);
        card.add(Box.createVerticalStrut(5));
        
        // Price / Buy button
        if (skin.price == 0) {
            JLabel freeLabel = new JLabel("FREE");
            freeLabel.setForeground(successColor);
            freeLabel.setFont(new Font("Arial", Font.BOLD, 12));
            freeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(freeLabel);
        } else {
            JButton buyBtn = new JButton("💰 " + skin.price);
            buyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            buyBtn.setBackground(playerCoins >= skin.price ? accentColor : Color.GRAY);
            buyBtn.setForeground(Color.WHITE);
            buyBtn.setFont(new Font("Arial", Font.BOLD, 11));
            buyBtn.setFocusPainted(false);
            buyBtn.setEnabled(playerCoins >= skin.price);
            
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
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        Color bgCol = skin.isEquipped ? new Color(50, 80, 60) : new Color(55, 55, 75);
        card.setBackground(bgCol);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(skin.isEquipped ? successColor : new Color(70, 70, 90), 2),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        // Preview
        JLabel previewLabel = new JLabel();
        previewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        try {
            BufferedImage img = ImageIO.read(getClass().getResource("/player/" + skin.skinFolder + "/Character_Stand.png"));
            if (img != null) {
                Image scaled = img.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception e) {
            previewLabel.setText("?");
            previewLabel.setForeground(Color.GRAY);
        }
        card.add(previewLabel);
        card.add(Box.createVerticalStrut(5));
        
        // Name + equipped status
        String status = skin.isEquipped ? " ✅" : "";
        JLabel nameLabel = new JLabel(skin.name + status);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(5));
        
        // Equip button
        if (!skin.isEquipped) {
            JButton equipBtn = new JButton("Equip");
            equipBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            equipBtn.setBackground(accentColor);
            equipBtn.setForeground(Color.WHITE);
            equipBtn.setFont(new Font("Arial", Font.BOLD, 11));
            equipBtn.setFocusPainted(false);
            
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
            equippedLabel.setFont(new Font("Arial", Font.BOLD, 11));
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
