package objects;

import objects.entities.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * PowerUp class for Score Battle (PvP) mode.
 * Different types of power-ups that give temporary buffs.
 */
public class PowerUp {
    private int x, y;
    private int size;
    private PowerUpType type;
    private boolean isCollected;
    private boolean isExpired;
    
    // Animation
    private int animFrame = 0;
    private int animCounter = 0;
    private float floatOffset = 0;
    private float floatDirection = 1;
    
    // Lifetime
    private int lifetime;
    private int lifetimeTimer = 0;
    private static final int DEFAULT_LIFETIME = 600; // 10 seconds at 60fps
    
    public enum PowerUpType {
        // Type: Name, Color, Duration (frames), Description
        SPEED_BOOST(new Color(0, 200, 255), 300, "Speed +50%", 1.5f, 0, 0),      // 5 seconds
        DOUBLE_DAMAGE(new Color(255, 100, 0), 360, "Damage x2", 1.0f, 2.0f, 0),   // 6 seconds
        SHIELD(new Color(100, 255, 100), 240, "Invincible", 1.0f, 1.0f, 1),       // 4 seconds
        GOLD_MAGNET(new Color(255, 215, 0), 480, "Gold +50%", 1.0f, 1.0f, 2),     // 8 seconds
        HEALTH_PACK(new Color(255, 50, 50), 0, "Heal +30", 1.0f, 1.0f, 3);        // Instant heal
        
        public final Color color;
        public final int duration;
        public final String description;
        public final float speedMultiplier;
        public final float damageMultiplier;
        public final int effectType; // 0=speed, 1=shield, 2=gold, 3=health
        
        PowerUpType(Color color, int duration, String description, 
                    float speedMultiplier, float damageMultiplier, int effectType) {
            this.color = color;
            this.duration = duration;
            this.description = description;
            this.speedMultiplier = speedMultiplier;
            this.damageMultiplier = damageMultiplier;
            this.effectType = effectType;
        }
    }
    
    public PowerUp(int x, int y, PowerUpType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.size = 32;
        this.isCollected = false;
        this.isExpired = false;
        this.lifetime = DEFAULT_LIFETIME;
    }
    
    public void update() {
        if (isCollected || isExpired) return;
        
        // Animation - floating effect
        animCounter++;
        if (animCounter >= 3) {
            animFrame = (animFrame + 1) % 8;
            animCounter = 0;
            
            floatOffset += 0.5f * floatDirection;
            if (floatOffset > 5 || floatOffset < -5) {
                floatDirection *= -1;
            }
        }
        
        // Lifetime countdown
        lifetimeTimer++;
        if (lifetimeTimer >= lifetime) {
            isExpired = true;
        }
    }
    
    /**
     * Check collision with player
     */
    public boolean checkCollision(Player player) {
        if (isCollected || isExpired) return false;
        
        Rectangle powerUpRect = new Rectangle(x, y, size, size);
        Rectangle playerRect = new Rectangle(
            player.getWorldX() + player.getHitBox().x,
            player.getWorldY() + player.getHitBox().y,
            player.getHitBox().width, player.getHitBox().height
        );
        
        return powerUpRect.intersects(playerRect);
    }
    
    /**
     * Collect the power-up
     */
    public void collect() {
        isCollected = true;
    }
    
    /**
     * Render the power-up
     */
    public void render(Graphics2D g2d, int screenX, int screenY, int tileSize) {
        if (isCollected || isExpired) return;
        
        int drawY = screenY + (int) floatOffset;
        
        // Blinking effect when about to expire
        if (lifetimeTimer > lifetime - 120 && (lifetimeTimer / 10) % 2 == 0) {
            return; // Skip drawing (blink)
        }
        
        // Glow effect
        g2d.setColor(new Color(type.color.getRed(), type.color.getGreen(), 
                               type.color.getBlue(), 50));
        g2d.fillOval(screenX - 8, drawY - 8, size + 16, size + 16);
        
        // Main circle
        g2d.setColor(type.color);
        g2d.fillOval(screenX + 4, drawY + 4, size - 8, size - 8);
        
        // Inner highlight
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.fillOval(screenX + 8, drawY + 8, size / 3, size / 3);
        
        // Border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(screenX + 4, drawY + 4, size - 8, size - 8);
        
        // Icon based on type
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String icon = getIcon();
        FontMetrics fm = g2d.getFontMetrics();
        int iconX = screenX + (size - fm.stringWidth(icon)) / 2;
        int iconY = drawY + (size + fm.getAscent()) / 2 - 4;
        g2d.drawString(icon, iconX, iconY);
    }
    
    private String getIcon() {
        switch (type) {
            case SPEED_BOOST: return "⚡";
            case DOUBLE_DAMAGE: return "⚔";
            case SHIELD: return "🛡";
            case GOLD_MAGNET: return "💰";
            case HEALTH_PACK: return "♥";
            default: return "?";
        }
    }
    
    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public PowerUpType getType() { return type; }
    public boolean isCollected() { return isCollected; }
    public boolean isExpired() { return isExpired; }
    public boolean shouldRemove() { return isCollected || isExpired; }
}
