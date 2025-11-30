package objects.entities;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class NPC extends Entity {
    protected Image sprite; // NPC's sprite
    protected String name;
    protected int tileSize;

    public NPC(String name,int worldX, int worldY,Image sprite, int tileSize) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.sprite = sprite;
        this.name = name;
        this.tileSize = tileSize;
    }

    public void render(Graphics2D g, int x, int y) {
        // Draw NPC sprite first
        g.drawImage(sprite, x , y , tileSize, tileSize, null);
        
        // Draw speech bubble with name
        drawSpeechBubble(g, x, y);
    }
    
    private void drawSpeechBubble(Graphics2D g, int x, int y) {
        // Set font for name
        Font nameFont = new Font("Arial", Font.BOLD, 12);
        g.setFont(nameFont);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(name);
        int textHeight = fm.getHeight();
        
        // Bubble dimensions
        int padding = 8;
        int bubbleWidth = textWidth + padding * 2;
        int bubbleHeight = textHeight + padding;
        int bubbleX = x + (tileSize - bubbleWidth) / 2;
        int bubbleY = y - bubbleHeight - 12;
        
        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw bubble shadow
        g.setColor(new Color(0, 0, 0, 50));
        g.fillRoundRect(bubbleX + 2, bubbleY + 2, bubbleWidth, bubbleHeight, 10, 10);
        
        // Draw bubble background
        g.setColor(new Color(255, 255, 255, 240));
        g.fillRoundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, 10, 10);
        
        // Draw bubble border
        g.setColor(new Color(76, 175, 80)); // Green border
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, 10, 10);
        
        // Draw pointer triangle
        int[] triangleX = {bubbleX + bubbleWidth/2 - 5, bubbleX + bubbleWidth/2 + 5, bubbleX + bubbleWidth/2};
        int[] triangleY = {bubbleY + bubbleHeight, bubbleY + bubbleHeight, bubbleY + bubbleHeight + 6};
        g.setColor(new Color(255, 255, 255, 240));
        g.fillPolygon(triangleX, triangleY, 3);
        g.setColor(new Color(76, 175, 80));
        g.drawLine(triangleX[0], triangleY[0], triangleX[2], triangleY[2]);
        g.drawLine(triangleX[1], triangleY[1], triangleX[2], triangleY[2]);
        
        // Draw name text
        g.setColor(new Color(33, 33, 33)); // Dark gray text
        int textX = bubbleX + padding;
        int textY = bubbleY + fm.getAscent() + padding/2;
        g.drawString(name, textX, textY);
    }

    public void checkDraw(Player player,Graphics2D g) {
        int screenX = worldX - player.getWorldX() + player.getScreenX();
        int screenY = worldY - player.getWorldY() + player.getScreenY();

        if (worldX > player.getWorldX() - player.getScreenX() - tileSize*2
                && worldX < player.getWorldX() + player.getScreenX() + tileSize*2
                && worldY > player.getWorldY() - player.getScreenY() - tileSize*2
                && worldY < player.getWorldY() + player.getScreenY()+ tileSize*2) {
            render(g, screenX, screenY);
        }
    }

    public boolean isPlayerNear(Player player) {
        int playerX = player.getWorldX();
        int playerY = player.getWorldY();

        // Assuming the sprite's width and height are equal to tileSize
        int spriteWidth = tileSize;
        int spriteHeight = tileSize;

        int expanded = 15;

        // Calculate the bounds of the NPC's sprite box
        int leftBound = worldX - expanded;
        int rightBound = worldX + spriteWidth + expanded;
        int upperBound = worldY - expanded;
        int lowerBound = worldY + spriteHeight + expanded;

        // Check if the player's position is within the bounds of the NPC's sprite box
        return playerX >= leftBound && playerX <= rightBound && playerY >= upperBound && playerY <= lowerBound;
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void render(Graphics graphics) {

    }
}
