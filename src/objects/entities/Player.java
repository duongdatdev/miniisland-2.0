package objects.entities;

import imageRender.ImageHandler;
import imageRender.ImageLoader;
import input.KeyHandler;
import main.GameScene;
import maps.TileType;

import java.awt.*;
import java.awt.image.BufferedImage;
public class Player extends Entity {
    private int screenX;
    private int screenY;
    private final int scale = 1;
    private KeyHandler keyHandler;
    private int id;
    private int spriteIndex = 0;
    private int countFrames = 0;
    private BufferedImage[] standingImages;
    public GameScene gameScene;
    private String username;

    private String lastDirection = "DOWN";

    private int state = 0;
    
    // Health system for maze mode
    private int health = 100;
    private int maxHealth = 100;
    private boolean isAlive = true;
    
    // Invincibility frames after taking damage
    private int invincibilityFrames = 0;
    private static final int INVINCIBILITY_DURATION = 60; // 1 second at 60fps
    
    // Slow effect from traps
    private int slowEffectTimer = 0;
    private float speedMultiplier = 1.0f;
    private int baseSpeed = 3;
    
    // === NEW: Dash System ===
    private boolean isDashing = false;
    private int dashTimer = 0;
    private int dashCooldown = 0;
    private static final int DASH_DURATION = 8;      // Frames of dash
    private static final int DASH_COOLDOWN = 90;     // Cooldown frames (1.5 seconds)
    private static final int DASH_SPEED = 12;        // Speed during dash
    private int dashDirectionX = 0;
    private int dashDirectionY = 0;
    
    // === NEW: PvP Mode Speed Boost ===
    private boolean pvpModeActive = false;
    private static final int PVP_SPEED_BONUS = 1;    // Extra speed in PvP

    public Player(GameScene gameScene, KeyHandler keyHandler) {
        this.keyHandler = keyHandler;
        this.gameScene = gameScene;

        hitBox = new Rectangle();
    
        hitBox.width = 12;   
        hitBox.height = 20;  
        hitBox.x = 20;       
        hitBox.y = 20;       

        setDefaultPosition();
        setDefaultSpeed();
        loadAssets();
    }

    public Player(String username, int x, int y, int dir, int id) {
        this.username = username;
        this.id = id;
        this.worldX = x;
        this.worldY = y;
        this.direction = "STAND";

//        setDefaultPosition();
//        setDefaultSpeed();
        loadAssets();
    }

    public void loadAssets() {
        upImages = ImageLoader.getInstance().upImages[0];
        downImages = ImageLoader.getInstance().downImages[0];
        leftImages = ImageLoader.getInstance().leftImages[0];
        rightImages = ImageLoader.getInstance().rightImages[0];
        standingImages = ImageLoader.getInstance().standingImages[0];
    }

    public void changeSprite(int spriteIndex){
        upImages = ImageLoader.getInstance().upImages[spriteIndex];
        downImages = ImageLoader.getInstance().downImages[spriteIndex];
        leftImages = ImageLoader.getInstance().leftImages[spriteIndex];
        rightImages = ImageLoader.getInstance().rightImages[spriteIndex];
        standingImages = ImageLoader.getInstance().standingImages[spriteIndex];
    }
    
    /**
     * Change skin by folder name (e.g., "1", "2")
     */
    public void changeSkin(String skinFolder) {
        try {
            int index = Integer.parseInt(skinFolder) - 1; // folder "1" = index 0
            if (index >= 0 && index < ImageLoader.getInstance().upImages.length) {
                changeSprite(index);
                System.out.println("Skin changed to: " + skinFolder);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid skin folder: " + skinFolder);
        }
    }

    public void setDefaultPosition() {
        screenX = gameScene.getScreenWidth() / 2 - gameScene.getTileSize() * scale / 2;
        screenY = gameScene.getScreenHeight() / 2 - gameScene.getTileSize() * scale / 2;
        worldX = 1645;
        worldY = 754;
    }

    private void setDefaultSpeed() {
        baseSpeed = 3;
        speed = baseSpeed;
    }

    //Counter for the number of times the player has moved
    int count = 1;

    /**
     * Updates the player's position and direction
     */
    public void update() {
        // Update invincibility frames
        if (invincibilityFrames > 0) {
            invincibilityFrames--;
        }
        
        // Update slow effect
        if (slowEffectTimer > 0) {
            slowEffectTimer--;
            if (slowEffectTimer <= 0) {
                speedMultiplier = 1.0f;
                speed = getEffectiveSpeed();
            }
        }
        
        // Update dash cooldown
        if (dashCooldown > 0) {
            dashCooldown--;
        }
        
        // Handle dash movement
        if (isDashing) {
            dashTimer--;
            if (dashTimer <= 0) {
                isDashing = false;
                speed = getEffectiveSpeed();
            } else {
                // Move in dash direction
                worldX += dashDirectionX * DASH_SPEED;
                worldY += dashDirectionY * DASH_SPEED;
                return; // Skip normal movement during dash
            }
        }

        if (isMove()) {

            // Store old position for rollback
            int oldX = worldX;
            int oldY = worldY;
            
            int futureX = worldX;
            int futureY = worldY;
            
            boolean movingX = false;
            boolean movingY = false;
            
            // Get current effective speed
            int currentSpeed = getEffectiveSpeed();
            
            // Calculate movement direction
            int moveX = 0;
            int moveY = 0;

            if (keyHandler.isUp()) {
                direction = "UP";
                moveY = -1;
                movingY = true;
            }
            if (keyHandler.isDown()) {
                direction = "DOWN";
                moveY = 1;
                movingY = true;
            }
            if (keyHandler.isLeft()) {
                direction = "LEFT";
                moveX = -1;
                movingX = true;
            }
            if (keyHandler.isRight()) {
                direction = "RIGHT";
                moveX = 1;
                movingX = true;
            }
            
            // Normalize diagonal movement to prevent faster speed
            if (movingX && movingY) {
                // Use ~0.707 factor for diagonal (1/sqrt(2))
                double diagonalFactor = 0.7071;
                futureX += (int)(moveX * currentSpeed * diagonalFactor);
                futureY += (int)(moveY * currentSpeed * diagonalFactor);
            } else {
                futureX += moveX * currentSpeed;
                futureY += moveY * currentSpeed;
            }

            count = 1;
            
            // Check X-axis collision separately
            if (movingX) {
                collision = false;
                flagUpdate = true;
                worldX = futureX; // Temporarily set for collision check
                
                gameScene.getCollisionChecker().checkTile(this, () -> {
                    if (collision || !flagUpdate) {
                        // Rollback X movement if collision detected
                        worldX = oldX;
                    }
                });
                
                futureX = worldX; // Update futureX with actual position after collision check
                worldX = oldX; // Reset for Y check
            }
            
            // Check Y-axis collision separately
            if (movingY) {
                collision = false;
                flagUpdate = true;
                worldY = futureY; // Temporarily set for collision check
                
                gameScene.getCollisionChecker().checkTile(this, () -> {
                    if (collision || !flagUpdate) {
                        // Rollback Y movement if collision detected
                        worldY = oldY;
                    }
                });
                
                futureY = worldY; // Update futureY with actual position after collision check
            }
            
            // Apply final validated movement
            worldX = futureX;
            worldY = futureY;
        }
        
        // Handle shooting based on map type
        String currentMap = gameScene.getCurrentMap();
        if (currentMap.equals("hunt")) {
            // In Monster Hunt: Use mouse for shooting (left click held)
            if (gameScene.getMouseHandler() != null && gameScene.getMouseHandler().isLeftHeld()) {
                // Update mouse handler screen center for aiming
                gameScene.getMouseHandler().setScreenCenter(screenX + 24, screenY + 24);
                gameScene.getPlayerMP().shootWithMouse();
            }
        } else {
            // In other maps: Use space bar for shooting
            if (isSpace()) {
                gameScene.getPlayerMP().shot();
            }
        }
        
        // Right click for dash in Monster Hunt
        if (currentMap.equals("hunt") && gameScene.getMouseHandler() != null) {
            if (gameScene.getMouseHandler().isRightHeld() && canDash()) {
                performDash();
            }
        } else if (keyHandler.isShift() && canDash()) {
            // Shift for dash in other maps
            performDash();
        }
        
        // Update aim direction for 8-way shooting (keyboard fallback)
        if (keyHandler.isAiming()) {
            gameScene.getPlayerMP().setAimDirection(
                keyHandler.getAimDirectionX(),
                keyHandler.getAimDirectionY()
            );
        } else {
            gameScene.getPlayerMP().stopAiming();
        }
        
        // Cycle bullet type with Q key or middle mouse button
        if (keyHandler.isQKey() || (gameScene.getMouseHandler() != null && gameScene.getMouseHandler().isMiddleClick())) {
            gameScene.getPlayerMP().cycleBulletType();
            keyHandler.setQKey(false);
        }
        
        // Handle standing state when not moving
        if (!isMove()) {
            if (count == 1) {
                lastDirection = direction;
                direction = "STAND";
            }
            count = 0;
        }

    }
    
    /**
     * Get effective speed considering PvP mode and multipliers
     */
    private int getEffectiveSpeed() {
        int effectiveSpeed = baseSpeed;
        
        // Add PvP speed bonus
        if (pvpModeActive) {
            effectiveSpeed += PVP_SPEED_BONUS;
        }
        
        // Apply speed multiplier (from power-ups or debuffs)
        effectiveSpeed = (int)(effectiveSpeed * speedMultiplier);
        
        // Apply PvP map speed multiplier if available
        if (gameScene != null && gameScene.getCurrentMap().equals("hunt")) {
            float pvpSpeedMultiplier = gameScene.getPvpMap().getSpeedMultiplier();
            if (pvpSpeedMultiplier > 1.0f) {
                effectiveSpeed = (int)(effectiveSpeed * pvpSpeedMultiplier);
            }
        }
        
        return Math.max(1, effectiveSpeed); // Minimum speed of 1
    }
    
    /**
     * Check if player can dash
     */
    public boolean canDash() {
        return !isDashing && dashCooldown <= 0 && isMove();
    }
    
    /**
     * Perform dash in current movement direction
     */
    public void performDash() {
        if (!canDash()) return;
        
        isDashing = true;
        dashTimer = DASH_DURATION;
        dashCooldown = DASH_COOLDOWN;
        invincibilityFrames = DASH_DURATION; // Brief invincibility during dash
        
        // Set dash direction based on current movement
        dashDirectionX = 0;
        dashDirectionY = 0;
        
        if (keyHandler.isUp()) dashDirectionY = -1;
        if (keyHandler.isDown()) dashDirectionY = 1;
        if (keyHandler.isLeft()) dashDirectionX = -1;
        if (keyHandler.isRight()) dashDirectionX = 1;
        
        // Normalize diagonal dash
        if (dashDirectionX != 0 && dashDirectionY != 0) {
            // Keep full speed for diagonal - feels better
        }
    }
    
    /**
     * Set PvP mode for speed bonus
     */
    public void setPvpModeActive(boolean active) {
        this.pvpModeActive = active;
        speed = getEffectiveSpeed();
    }
    
    /**
     * Check if currently dashing
     */
    public boolean isDashing() {
        return isDashing;
    }
    
    /**
     * Get dash cooldown percentage (0-1)
     */
    public float getDashCooldownPercent() {
        return dashCooldown > 0 ? (float) dashCooldown / DASH_COOLDOWN : 0;
    }

    public void render(Graphics2D g2d, int tileSize) {
        // Flash effect when invincible (hit recently)
        if (invincibilityFrames > 0 && (invincibilityFrames / 5) % 2 == 0) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        }
        
        g2d.drawImage(currentSprite(), screenX, screenY, tileSize * scale, tileSize * scale, null);
        
        // Reset composite
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        
        // DEBUG: Draw hitbox to check collision (toggle by comment/uncomment)
        // Uncomment the 4 lines below to see red hitbox in game:
        // g2d.setColor(new Color(255, 0, 0, 100)); // Red semi-transparent
        // g2d.fillRect(screenX + hitBox.x, screenY + hitBox.y, hitBox.width, hitBox.height);
        // g2d.setColor(Color.RED);
        // g2d.drawRect(screenX + hitBox.x, screenY + hitBox.y, hitBox.width, hitBox.height);
    }
    
    /**
     * Render player health bar (for maze mode)
     */
    public void renderHealthBar(Graphics2D g2d, int screenWidth) {
        int barWidth = 200;
        int barHeight = 20;
        int barX = 20;
        int barY = 20;
        
        // Background (dark gray)
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRect(barX, barY, barWidth, barHeight);
        
        // Health bar (gradient from green to red based on health)
        float healthPercent = (float) health / maxHealth;
        int healthWidth = (int) (barWidth * healthPercent);
        
        // Color based on health percentage
        Color healthColor;
        if (healthPercent > 0.6f) {
            healthColor = new Color(50, 200, 50); // Green
        } else if (healthPercent > 0.3f) {
            healthColor = new Color(255, 200, 0); // Yellow
        } else {
            healthColor = new Color(200, 50, 50); // Red
        }
        
        g2d.setColor(healthColor);
        g2d.fillRect(barX, barY, healthWidth, barHeight);
        
        // Border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(barX, barY, barWidth, barHeight);
        
        // Health text
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.WHITE);
        String healthText = health + " / " + maxHealth;
        FontMetrics fm = g2d.getFontMetrics();
        int textX = barX + (barWidth - fm.stringWidth(healthText)) / 2;
        int textY = barY + (barHeight + fm.getAscent()) / 2 - 2;
        g2d.drawString(healthText, textX, textY);
        
        // Heart icon
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("HP", barX + barWidth + 10, barY + 16);
    }
    
    /**
     * Take damage from enemies or traps
     * @return true if player died
     */
    public boolean takeDamage(int damage) {
        if (!isAlive || invincibilityFrames > 0) return false;
        
        health -= damage;
        invincibilityFrames = INVINCIBILITY_DURATION;
        
        if (health <= 0) {
            health = 0;
            isAlive = false;
            return true; // Player died
        }
        return false;
    }
    
    /**
     * Instant death (for deadly traps)
     */
    public void instantDeath() {
        health = 0;
        isAlive = false;
    }
    
    /**
     * Heal the player
     */
    public void heal(int amount) {
        health = Math.min(health + amount, maxHealth);
    }
    
    /**
     * Apply slow effect
     */
    public void applySlowEffect(float multiplier, int duration) {
        speedMultiplier = multiplier;
        slowEffectTimer = duration;
        speed = (int) (baseSpeed * speedMultiplier);
    }
    
    /**
     * Reset player for maze mode
     */
    public void resetForMaze() {
        health = maxHealth;
        isAlive = true;
        invincibilityFrames = 0;
        slowEffectTimer = 0;
        speedMultiplier = 1.0f;
        speed = baseSpeed;
    }
    
    /**
     * Check if player is invincible
     */
    public boolean isInvincible() {
        return invincibilityFrames > 0;
    }

    public boolean isSpace() {
        return keyHandler.isSpace();
    }

//    public void shot() {
//        Bullet bullet = new Bullet(worldX, worldY, lastDirection);
//        bullets.add(bullet);
//    }

    public BufferedImage currentSprite() {
        // Update animation frame
        countFrames++;
        if (countFrames > 11) {
            spriteIndex = (spriteIndex + 1) % 4; // Optimized modulo operation
            countFrames = 0;
        }
        
        // Get the appropriate sprite array based on direction
        BufferedImage[] currentSpriteArray = null;
        switch (direction) {
            case "UP":
                currentSpriteArray = upImages;
                break;
            case "DOWN":
                currentSpriteArray = downImages;
                break;
            case "LEFT":
                currentSpriteArray = leftImages;
                break;
            case "RIGHT":
                currentSpriteArray = rightImages;
                break;
            case "STAND":
                currentSpriteArray = standingImages;
                break;
        }
        
        // Validate and return sprite
        if (currentSpriteArray == null) {
            System.err.println("[Player] Sprite array for direction '" + direction + "' is null!");
            return null;
        }
        
        return currentSpriteArray[spriteIndex];
    }

    public boolean isMove() {
        return (keyHandler.isUp() || keyHandler.isDown() || keyHandler.isLeft() || keyHandler.isRight());
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public int getScreenX() {
        return screenX;
    }

    public void setScreenX(int screenX) {
        this.screenX = screenX;
    }

    public int getScreenY() {
        return screenY;
    }

    public void setScreenY(int screenY) {
        this.screenY = screenY;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public String getLastDirection() {
        return lastDirection;
    }

    public void setLastDirection(String lastDirection) {
        this.lastDirection = lastDirection;
    }
    
    // Health getters and setters
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
    public boolean isPlayerAlive() { return isAlive; }
    public void setAlive(boolean alive) { this.isAlive = alive; }

    @Override
    public void update(float delta) {

    }

    @Override
    public void render(Graphics graphics) {

    }
}
