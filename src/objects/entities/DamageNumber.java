package objects.entities;

import java.awt.*;

/**
 * DamageNumber class displays floating damage numbers when monsters are hit.
 * Numbers float up and fade out over time.
 */
public class DamageNumber {
    private int x, y;
    private int damage;
    private float alpha;
    private int lifetime;
    private int maxLifetime;
    private float velocityY;
    private Color color;
    private boolean isCritical;
    private boolean isGold; // For gold earned display
    
    private static final int DEFAULT_LIFETIME = 60; // 1 second at 60fps
    
    public DamageNumber(int x, int y, int damage, boolean isCritical) {
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.isCritical = isCritical;
        this.alpha = 1.0f;
        this.maxLifetime = DEFAULT_LIFETIME;
        this.lifetime = maxLifetime;
        this.velocityY = -2.0f; // Float upward
        this.isGold = false;
        
        // Color based on damage type
        if (isCritical) {
            this.color = new Color(255, 200, 0); // Gold for critical
        } else {
            this.color = Color.WHITE;
        }
    }
    
    /**
     * Create gold earned number (green/gold color)
     */
    public DamageNumber(int x, int y, int gold) {
        this.x = x;
        this.y = y;
        this.damage = gold;
        this.isCritical = false;
        this.isGold = true;
        this.alpha = 1.0f;
        this.maxLifetime = 90; // 1.5 seconds
        this.lifetime = maxLifetime;
        this.velocityY = -1.5f;
        this.color = new Color(255, 215, 0); // Gold color
    }
    
    /**
     * Update the damage number animation
     */
    public void update() {
        lifetime--;
        y += velocityY;
        
        // Slow down upward movement
        velocityY *= 0.95f;
        
        // Fade out in last 30% of lifetime
        if (lifetime < maxLifetime * 0.3f) {
            alpha = lifetime / (maxLifetime * 0.3f);
        }
    }
    
    /**
     * Render the damage number
     */
    public void render(Graphics2D g2d, int playerWorldX, int playerWorldY, int playerScreenX, int playerScreenY) {
        if (alpha <= 0) return;
        
        int screenX = x - playerWorldX + playerScreenX;
        int screenY = y - playerWorldY + playerScreenY;
        
        // Set font based on type
        Font font;
        if (isCritical) {
            font = new Font("Arial", Font.BOLD, 20);
        } else if (isGold) {
            font = new Font("Arial", Font.BOLD, 16);
        } else {
            font = new Font("Arial", Font.BOLD, 14);
        }
        g2d.setFont(font);
        
        // Create text
        String text;
        if (isGold) {
            text = "+" + damage + "G";
        } else {
            text = "-" + damage;
            if (isCritical) {
                text += "!";
            }
        }
        
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        
        // Draw shadow with alpha
        g2d.setColor(new Color(0, 0, 0, (int)(alpha * 150)));
        g2d.drawString(text, screenX - textWidth / 2 + 2, screenY + 2);
        
        // Draw main text with alpha
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)));
        g2d.drawString(text, screenX - textWidth / 2, screenY);
        
        // Draw outline for critical hits
        if (isCritical) {
            g2d.setColor(new Color(255, 100, 0, (int)(alpha * 200)));
            g2d.drawString(text, screenX - textWidth / 2 - 1, screenY - 1);
        }
    }
    
    /**
     * Check if this damage number should be removed
     */
    public boolean shouldRemove() {
        return lifetime <= 0 || alpha <= 0;
    }
    
    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getDamage() { return damage; }
    public boolean isExpired() { return shouldRemove(); }
}
