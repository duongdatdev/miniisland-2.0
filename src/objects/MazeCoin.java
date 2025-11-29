package objects;

import objects.entities.Player;

import java.awt.*;

/**
 * MazeCoin class for Maze mode.
 * Collectible coins that give bonus points.
 */
public class MazeCoin {
    private int x, y;
    private int size;
    private int value;
    private boolean isCollected;
    private CoinType type;
    
    // Animation
    private int animFrame = 0;
    private int animCounter = 0;
    private float rotation = 0;
    
    public enum CoinType {
        BRONZE(10, new Color(205, 127, 50)),
        SILVER(25, new Color(192, 192, 192)),
        GOLD(50, new Color(255, 215, 0)),
        DIAMOND(100, new Color(185, 242, 255));
        
        public final int value;
        public final Color color;
        
        CoinType(int value, Color color) {
            this.value = value;
            this.color = color;
        }
    }
    
    public MazeCoin(int x, int y, CoinType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.value = type.value;
        this.size = 24;
        this.isCollected = false;
    }
    
    public void update() {
        if (isCollected) return;
        
        // Rotation animation
        animCounter++;
        if (animCounter >= 4) {
            animFrame = (animFrame + 1) % 8;
            rotation += 0.15f;
            animCounter = 0;
        }
    }
    
    /**
     * Check collision with player
     */
    public boolean checkCollision(Player player) {
        if (isCollected) return false;
        
        Rectangle coinRect = new Rectangle(x + 4, y + 4, size - 8, size - 8);
        Rectangle playerRect = new Rectangle(
            player.getWorldX() + player.getHitBox().x,
            player.getWorldY() + player.getHitBox().y,
            player.getHitBox().width, player.getHitBox().height
        );
        
        return coinRect.intersects(playerRect);
    }
    
    /**
     * Collect the coin
     */
    public void collect() {
        isCollected = true;
    }
    
    /**
     * Render the coin
     */
    public void render(Graphics2D g2d, int screenX, int screenY, int tileSize) {
        if (isCollected) return;
        
        // Calculate coin width based on "rotation" for 3D effect
        int coinWidth = (int) (size * Math.abs(Math.cos(rotation)));
        coinWidth = Math.max(4, coinWidth);
        
        int offsetX = (size - coinWidth) / 2;
        
        // Glow effect
        g2d.setColor(new Color(type.color.getRed(), type.color.getGreen(), 
                               type.color.getBlue(), 80));
        g2d.fillOval(screenX - 4, screenY - 4, size + 8, size + 8);
        
        // Main coin body
        g2d.setColor(type.color);
        g2d.fillOval(screenX + offsetX, screenY, coinWidth, size);
        
        // Highlight
        g2d.setColor(new Color(255, 255, 255, 180));
        if (coinWidth > 8) {
            g2d.fillOval(screenX + offsetX + 4, screenY + 4, coinWidth / 3, size / 3);
        }
        
        // Border
        g2d.setColor(type.color.darker());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(screenX + offsetX, screenY, coinWidth, size);
        
        // Value indicator for special coins
        if (type == CoinType.DIAMOND || type == CoinType.GOLD) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String valueStr = "+" + value;
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(valueStr, screenX + (size - fm.stringWidth(valueStr)) / 2, 
                          screenY + size + 12);
        }
    }
    
    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getValue() { return value; }
    public CoinType getType() { return type; }
    public boolean isCollected() { return isCollected; }
}
