package objects.entities;

import main.GameScene;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 * Monster class represents monsters in Monster Hunt (Score Battle) mode.
 * Monsters can move randomly and attack players.
 */
public class Monster extends Entity {
    private int id;
    private int health;
    private int maxHealth;
    private int damage;
    private int goldReward; // Gold received when killed
    private boolean isAlive;
    private MonsterType type;
    
    // Animation - using shared sprites
    private int spriteIndex = 0;
    private int animationCounter = 0;
    private static final int ANIMATION_SPEED = 10;
    
    // Static cache for sprites - all monsters of same type share sprites
    private static HashMap<MonsterType, BufferedImage[]> spriteCache = new HashMap<>();
    private static boolean spritesInitialized = false;
    
    // AI Movement
    private int moveDirection; // 1=down, 2=up, 3=left, 4=right
    private int moveTimer;
    private int moveDuration;
    private Random random;
    
    // Attack
    private int attackCooldown;
    private int attackTimer;
    private int attackRange;
    
    // Map bounds - set by MonsterSpawner
    private int mapMinX = 528;  // Default: tile 11 * 48
    private int mapMaxX = 1824; // Default: tile 38 * 48
    private int mapMinY = 528;
    private int mapMaxY = 1824;
    
    // Death animation
    private boolean isDying = false;
    private int deathAnimTimer = 0;
    private static final int DEATH_ANIM_DURATION = 30; // 0.5 seconds
    private float deathScale = 1.0f;
    private float deathAlpha = 1.0f;
    private int deathRotation = 0;
    
    // Difficulty scaling
    private float difficultyMultiplier = 1.0f;
    
    // Hit flash effect
    private int hitFlashTimer = 0;
    private static final int HIT_FLASH_DURATION = 6;
    
    public enum MonsterType {
        SLIME(30, 5, 10, 2, 100), // health, damage, goldReward, speed, attackRange
        GOBLIN(50, 10, 25, 3, 80),
        ORC(100, 20, 50, 2, 120),
        BOSS(300, 30, 200, 1, 150);
        
        public final int health;
        public final int damage;
        public final int goldReward;
        public final int speed;
        public final int attackRange;
        
        MonsterType(int health, int damage, int goldReward, int speed, int attackRange) {
            this.health = health;
            this.damage = damage;
            this.goldReward = goldReward;
            this.speed = speed;
            this.attackRange = attackRange;
        }
    }
    
    public Monster(int id, int x, int y, MonsterType type) {
        this.id = id;
        this.worldX = x;
        this.worldY = y;
        this.type = type;
        this.maxHealth = type.health;
        this.health = type.health;
        this.damage = type.damage;
        this.goldReward = type.goldReward;
        this.speed = type.speed;
        this.attackRange = type.attackRange;
        this.isAlive = true;
        
        this.random = new Random();
        this.moveDirection = random.nextInt(4) + 1;
        this.moveDuration = random.nextInt(60) + 30;
        this.moveTimer = 0;
        
        this.attackCooldown = 60; // 1 second at 60fps
        this.attackTimer = 0;
        
        // Hitbox
        this.hitBox = new Rectangle(8, 8, 32, 32);
        
        // Load sprites (only created once per type)
        initSpritesIfNeeded();
    }
    
    /**
     * Constructor with difficulty multiplier for wave scaling
     */
    public Monster(int id, int x, int y, MonsterType type, float difficultyMultiplier) {
        this(id, x, y, type);
        this.difficultyMultiplier = difficultyMultiplier;
        
        // Scale stats based on difficulty
        this.maxHealth = (int)(type.health * difficultyMultiplier);
        this.health = this.maxHealth;
        this.damage = (int)(type.damage * difficultyMultiplier);
        this.goldReward = (int)(type.goldReward * (1 + (difficultyMultiplier - 1) * 0.5f)); // Gold scales slower
    }
    
    /**
     * Initialize sprites for all monster types (only runs once)
     */
    private static synchronized void initSpritesIfNeeded() {
        if (spritesInitialized) return;
        
        for (MonsterType t : MonsterType.values()) {
            BufferedImage[] sprites = new BufferedImage[4];
            
            // Try to load from file first
            if (!loadSpritesFromFile(sprites, t)) {
                // Fallback to generated sprites if file not found
                createFallbackSpritesForType(sprites, t);
            }
            
            spriteCache.put(t, sprites);
        }
        
        spritesInitialized = true;
    }
    
    /**
     * Try to load sprites from file
     * Supports: sprite sheet (192x48) or individual files
     * @return true if loaded successfully, false otherwise
     */
    private static boolean loadSpritesFromFile(BufferedImage[] sprites, MonsterType type) {
        String typeName = type.name().toLowerCase();
        
        try {
            // Try loading sprite sheet first: /NPC/monsters/slime.png (192x48 with 4 frames)
            java.io.InputStream is = Monster.class.getResourceAsStream("/NPC/monsters/" + typeName + ".png");
            if (is != null) {
                BufferedImage sheet = ImageIO.read(is);
                is.close();
                
                int frameWidth = sheet.getWidth() / 4;
                int frameHeight = sheet.getHeight();
                
                // If it's a sprite sheet (wider than tall)
                if (sheet.getWidth() >= sheet.getHeight() * 2) {
                    for (int i = 0; i < 4; i++) {
                        sprites[i] = sheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
                    }
                    System.out.println("Loaded sprite sheet for " + typeName);
                    return true;
                } else {
                    // Single image, use for all frames
                    for (int i = 0; i < 4; i++) {
                        sprites[i] = sheet;
                    }
                    System.out.println("Loaded single sprite for " + typeName);
                    return true;
                }
            }
            
            // Try loading individual frames: /NPC/monsters/slime_1.png, slime_2.png, etc.
            boolean allLoaded = true;
            for (int i = 0; i < 4; i++) {
                is = Monster.class.getResourceAsStream("/NPC/monsters/" + typeName + "_" + (i + 1) + ".png");
                if (is != null) {
                    sprites[i] = ImageIO.read(is);
                    is.close();
                } else {
                    allLoaded = false;
                    break;
                }
            }
            
            if (allLoaded) {
                System.out.println("Loaded individual sprites for " + typeName);
                return true;
            }
            
        } catch (IOException e) {
            System.err.println("Error loading sprites for " + typeName + ": " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get sprites for current monster type
     */
    private BufferedImage[] getSprites() {
        return spriteCache.get(type);
    }
    
    /**
     * Create fallback sprites when image files are not available
     * Each monster type has a unique visual design
     */
    private static void createFallbackSpritesForType(BufferedImage[] sprites, MonsterType type) {
        int size = 48;
        
        for (int i = 0; i < 4; i++) {
            sprites[i] = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sprites[i].createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            switch (type) {
                case SLIME:
                    drawSlime(g, size, i);
                    break;
                case GOBLIN:
                    drawGoblin(g, size, i);
                    break;
                case ORC:
                    drawOrc(g, size, i);
                    break;
                case BOSS:
                    drawBoss(g, size, i);
                    break;
            }
            
            g.dispose();
        }
    }
    
    // === SLIME: Blob xanh lá, nhảy lên xuống ===
    private static void drawSlime(Graphics2D g, int size, int frame) {
        int bounce = (frame % 2 == 0) ? 0 : 4; // Animation bounce
        Color mainColor = new Color(50, 200, 50);
        Color darkColor = new Color(30, 150, 30);
        Color lightColor = new Color(100, 255, 100, 150);
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval(6, 38 - bounce/2, size - 12, 8);
        
        // Body (oval blob)
        g.setColor(mainColor);
        g.fillOval(4, 12 + bounce, size - 8, size - 20);
        
        // Darker bottom
        g.setColor(darkColor);
        g.fillArc(4, 20 + bounce, size - 8, size - 24, 180, 180);
        
        // Shine/highlight
        g.setColor(lightColor);
        g.fillOval(12, 16 + bounce, 10, 8);
        
        // Eyes
        g.setColor(Color.WHITE);
        g.fillOval(14, 22 + bounce, 7, 7);
        g.fillOval(27, 22 + bounce, 7, 7);
        g.setColor(Color.BLACK);
        g.fillOval(16 + (frame % 2), 24 + bounce, 3, 3);
        g.fillOval(29 + (frame % 2), 24 + bounce, 3, 3);
    }
    
    // === GOBLIN: Nhỏ, tai nhọn, mũi dài ===
    private static void drawGoblin(Graphics2D g, int size, int frame) {
        int walk = (frame % 2 == 0) ? 0 : 2;
        Color skinColor = new Color(100, 150, 80);
        Color darkSkin = new Color(70, 110, 50);
        Color earColor = new Color(120, 170, 90);
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval(10, 40, size - 20, 6);
        
        // Ears (pointed)
        g.setColor(earColor);
        int[] earLX = {6, 14, 10};
        int[] earLY = {12, 18, 24};
        g.fillPolygon(earLX, earLY, 3);
        int[] earRX = {42, 34, 38};
        int[] earRY = {12, 18, 24};
        g.fillPolygon(earRX, earRY, 3);
        
        // Head
        g.setColor(skinColor);
        g.fillOval(10, 8, 28, 24);
        
        // Body
        g.setColor(darkSkin);
        g.fillOval(14, 28 + walk, 20, 14);
        
        // Eyes (big, yellow)
        g.setColor(Color.YELLOW);
        g.fillOval(14, 14, 9, 9);
        g.fillOval(25, 14, 9, 9);
        g.setColor(Color.RED);
        g.fillOval(17 + (frame % 2), 17, 3, 3);
        g.fillOval(28 + (frame % 2), 17, 3, 3);
        
        // Nose (long pointy)
        g.setColor(darkSkin);
        int[] noseX = {24, 20, 28};
        int[] noseY = {30, 22, 22};
        g.fillPolygon(noseX, noseY, 3);
    }
    
    // === ORC: To, có răng nanh, cơ bắp ===
    private static void drawOrc(Graphics2D g, int size, int frame) {
        int walk = (frame % 2 == 0) ? -1 : 1;
        Color skinColor = new Color(100, 140, 80);
        Color darkSkin = new Color(70, 100, 50);
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(4, 40, size - 8, 8);
        
        // Body (muscular)
        g.setColor(darkSkin);
        g.fillRoundRect(8, 24 + walk, 32, 18, 8, 8);
        
        // Head
        g.setColor(skinColor);
        g.fillOval(8, 4, 32, 26);
        
        // Jaw
        g.setColor(darkSkin);
        g.fillRect(12, 22, 24, 8);
        
        // Tusks (teeth)
        g.setColor(Color.WHITE);
        int[] tuskLX = {14, 10, 16};
        int[] tuskLY = {24, 32, 28};
        g.fillPolygon(tuskLX, tuskLY, 3);
        int[] tuskRX = {34, 38, 32};
        int[] tuskRY = {24, 32, 28};
        g.fillPolygon(tuskRX, tuskRY, 3);
        
        // Eyes (small, angry)
        g.setColor(Color.RED);
        g.fillOval(14, 12, 8, 6);
        g.fillOval(26, 12, 8, 6);
        g.setColor(Color.BLACK);
        g.fillOval(16 + (frame % 2), 13, 4, 4);
        g.fillOval(28 + (frame % 2), 13, 4, 4);
        
        // Eyebrows (angry)
        g.setColor(darkSkin);
        g.setStroke(new BasicStroke(2));
        g.drawLine(12, 10, 20, 12);
        g.drawLine(36, 10, 28, 12);
    }
    
    // === BOSS: To gấp đôi, có sừng, aura đỏ ===
    private static void drawBoss(Graphics2D g, int size, int frame) {
        int pulse = (frame % 2 == 0) ? 0 : 2;
        Color skinColor = new Color(80, 40, 40);
        Color darkSkin = new Color(50, 20, 20);
        Color auraColor = new Color(255, 50, 50, 100);
        
        // Aura effect (pulsing)
        g.setColor(auraColor);
        g.fillOval(-2 - pulse, -2 - pulse, size + 4 + pulse*2, size + 4 + pulse*2);
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(2, 40, size - 4, 8);
        
        // Horns
        g.setColor(Color.DARK_GRAY);
        int[] hornLX = {8, 2, 14};
        int[] hornLY = {10, -4, 14};
        g.fillPolygon(hornLX, hornLY, 3);
        int[] hornRX = {40, 46, 34};
        int[] hornRY = {10, -4, 14};
        g.fillPolygon(hornRX, hornRY, 3);
        
        // Head
        g.setColor(skinColor);
        g.fillOval(4, 6, 40, 30);
        
        // Body
        g.setColor(darkSkin);
        g.fillRoundRect(6, 30, 36, 14, 8, 8);
        
        // Jaw with fangs
        g.setColor(darkSkin);
        g.fillRect(10, 28, 28, 10);
        
        // Big fangs
        g.setColor(Color.WHITE);
        int[] fangLX = {14, 8, 18};
        int[] fangLY = {30, 42, 34};
        g.fillPolygon(fangLX, fangLY, 3);
        int[] fangRX = {34, 40, 30};
        int[] fangRY = {30, 42, 34};
        g.fillPolygon(fangRX, fangRY, 3);
        
        // Glowing eyes
        g.setColor(new Color(255, 200, 0)); // Yellow glow
        g.fillOval(12, 14, 10, 8);
        g.fillOval(26, 14, 10, 8);
        g.setColor(Color.RED);
        g.fillOval(15 + (frame % 2), 16, 4, 4);
        g.fillOval(29 + (frame % 2), 16, 4, 4);
        
        // Crown/spikes on head
        g.setColor(new Color(150, 50, 50));
        for (int i = 0; i < 3; i++) {
            int[] spikeX = {16 + i*8, 20 + i*8, 24 + i*8};
            int[] spikeY = {8, 0, 8};
            g.fillPolygon(spikeX, spikeY, 3);
        }
    }
    
    private static Color getColorForType(MonsterType type) {
        switch (type) {
            case SLIME:
                return new Color(50, 200, 50); // Green
            case GOBLIN:
                return new Color(100, 150, 50); // Dark green
            case ORC:
                return new Color(100, 80, 60); // Brown
            case BOSS:
                return new Color(150, 50, 50); // Dark red
            default:
                return Color.GRAY;
        }
    }
    
    /**
     * Update AI movement and attack
     */
    public void updateAI(Player targetPlayer) {
        // Update death animation
        if (isDying) {
            updateDeathAnimation();
            return;
        }
        
        if (!isAlive) return;
        
        // Update hit flash
        if (hitFlashTimer > 0) {
            hitFlashTimer--;
        }
        
        // Animation update
        animationCounter++;
        if (animationCounter >= ANIMATION_SPEED) {
            spriteIndex = (spriteIndex + 1) % 4;
            animationCounter = 0;
        }
        
        // Attack cooldown
        if (attackTimer > 0) {
            attackTimer--;
        }
        
        // Move towards player if within vision range
        int playerX = targetPlayer.getWorldX();
        int playerY = targetPlayer.getWorldY();
        double distance = Math.sqrt(Math.pow(worldX - playerX, 2) + Math.pow(worldY - playerY, 2));
        
        if (distance < 300) {
            // Chase player
            chasePlayer(playerX, playerY);
        } else {
            // Random movement
            randomMove();
        }
    }
    
    /**
     * Update death animation
     */
    private void updateDeathAnimation() {
        deathAnimTimer++;
        
        // Scale down and fade out
        float progress = (float) deathAnimTimer / DEATH_ANIM_DURATION;
        deathScale = 1.0f + progress * 0.3f; // Grow slightly
        deathAlpha = 1.0f - progress;
        deathRotation = (int)(progress * 180); // Rotate while dying
        
        // Float upward
        worldY -= 2;
        
        if (deathAnimTimer >= DEATH_ANIM_DURATION) {
            // Animation complete, monster can be removed
            isDying = false;
            // isAlive is already false from takeDamage()
        }
    }
    
    private void chasePlayer(int playerX, int playerY) {
        int dx = playerX - worldX;
        int dy = playerY - worldY;
        
        // Normalize and move
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length > 0) {
            int newX = worldX + (int) (speed * dx / length);
            int newY = worldY + (int) (speed * dy / length);
            
            // Clamp to map bounds (account for monster size ~48px)
            worldX = Math.max(mapMinX, Math.min(newX, mapMaxX - 48));
            worldY = Math.max(mapMinY, Math.min(newY, mapMaxY - 48));
            
            // Update direction for animation
            if (Math.abs(dx) > Math.abs(dy)) {
                direction = dx > 0 ? "RIGHT" : "LEFT";
            } else {
                direction = dy > 0 ? "DOWN" : "UP";
            }
        }
    }
    
    private void randomMove() {
        moveTimer++;
        
        if (moveTimer >= moveDuration) {
            moveTimer = 0;
            moveDirection = random.nextInt(4) + 1;
            moveDuration = random.nextInt(60) + 30;
        }
        
        int newX = worldX;
        int newY = worldY;
        
        switch (moveDirection) {
            case 1: // Down
                newY += speed;
                direction = "DOWN";
                break;
            case 2: // Up
                newY -= speed;
                direction = "UP";
                break;
            case 3: // Left
                newX -= speed;
                direction = "LEFT";
                break;
            case 4: // Right
                newX += speed;
                direction = "RIGHT";
                break;
        }
        
        // Keep monster within playable area bounds (account for monster size ~48px)
        worldX = Math.max(mapMinX, Math.min(newX, mapMaxX - 48));
        worldY = Math.max(mapMinY, Math.min(newY, mapMaxY - 48));
        
        // Change direction if hitting boundary
        if (worldX == mapMinX || worldX == mapMaxX - 48 || 
            worldY == mapMinY || worldY == mapMaxY - 48) {
            moveDirection = random.nextInt(4) + 1;
        }
    }
    
    /**
     * Take damage from bullet
     * @return gold reward if monster died, 0 otherwise
     */
    public int takeDamage(int damage) {
        // Only take damage if alive and not already dying
        if (!isAlive || isDying) {
            // System.out.println("[DEBUG] Monster cannot take damage - isAlive: " + isAlive + ", isDying: " + isDying);
            return 0;
        }
        
        health -= damage;
        hitFlashTimer = HIT_FLASH_DURATION; // Trigger hit flash
        
        // System.out.println("[DEBUG] Monster took " + damage + " damage, health: " + health + "/" + maxHealth);
        
        if (health <= 0) {
            health = 0;
            // Start death animation instead of immediately dying
            isDying = true;
            isAlive = false; // Mark as not alive immediately to prevent double-counting
            deathAnimTimer = 0;
            // System.out.println("[DEBUG] Monster KILLED! Gold reward: " + goldReward);
            return goldReward; // Return gold reward
        }
        return 0;
    }
    
    /**
     * Old method for compatibility - returns boolean
     */
    public boolean takeDamageBoolean(int damage) {
        return takeDamage(damage) > 0;
    }
    
    /**
     * Check collision with player
     */
    public boolean checkPlayerCollision(Player player) {
        if (!isAlive || isDying) return false;
        
        Rectangle monsterRect = new Rectangle(worldX + hitBox.x, worldY + hitBox.y, hitBox.width, hitBox.height);
        Rectangle playerRect = new Rectangle(player.getWorldX() + player.getHitBox().x, 
                                              player.getWorldY() + player.getHitBox().y,
                                              player.getHitBox().width, player.getHitBox().height);
        
        return monsterRect.intersects(playerRect);
    }
    
    /**
     * Check collision with bullet
     */
    public boolean checkBulletCollision(Bullet bullet) {
        if (!isAlive || isDying || bullet.isStop()) return false;
        
        Rectangle monsterRect = new Rectangle(worldX + hitBox.x, worldY + hitBox.y, hitBox.width, hitBox.height);
        Rectangle bulletRect = new Rectangle(bullet.getPosiX(), bullet.getPosiY(), 10, 10);
        
        return monsterRect.intersects(bulletRect);
    }
    
    /**
     * Render monster on screen
     */
    public void render(Graphics2D g2d, int screenX, int screenY, int tileSize) {
        if (!isAlive && !isDying) return;
        
        // Save original composite for transparency
        java.awt.Composite originalComposite = g2d.getComposite();
        java.awt.geom.AffineTransform originalTransform = g2d.getTransform();
        
        // Apply death animation effects
        if (isDying) {
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, deathAlpha));
            
            // Rotate and scale from center
            int centerX = screenX + tileSize / 2;
            int centerY = screenY + tileSize / 2;
            g2d.translate(centerX, centerY);
            g2d.rotate(Math.toRadians(deathRotation));
            g2d.scale(deathScale, deathScale);
            g2d.translate(-centerX, -centerY);
        }
        
        // Hit flash effect - tint red when hit
        BufferedImage[] sprites = getSprites();
        if (sprites != null && sprites[spriteIndex] != null) {
            if (hitFlashTimer > 0) {
                // Draw with red tint
                g2d.drawImage(sprites[spriteIndex], screenX, screenY, tileSize, tileSize, null);
                g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f));
                g2d.setColor(Color.RED);
                g2d.fillRect(screenX, screenY, tileSize, tileSize);
                g2d.setComposite(originalComposite);
            } else {
                g2d.drawImage(sprites[spriteIndex], screenX, screenY, tileSize, tileSize, null);
            }
        }
        
        // Restore transform after death animation
        if (isDying) {
            g2d.setTransform(originalTransform);
            g2d.setComposite(originalComposite);
            return; // Don't draw health bar when dying
        }
        
        // Draw health bar
        int healthBarWidth = 40;
        int healthBarHeight = 6;
        int healthBarX = screenX + (tileSize - healthBarWidth) / 2;
        int healthBarY = screenY - 10;
        
        // Background (red)
        g2d.setColor(Color.RED);
        g2d.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
        
        // Health (green to yellow to red based on HP)
        float healthPercent = (float) health / maxHealth;
        Color healthColor = new Color(
            (int)(255 * (1 - healthPercent)),
            (int)(255 * healthPercent),
            0
        );
        int currentHealthWidth = (int) (healthBarWidth * healthPercent);
        g2d.setColor(healthColor);
        g2d.fillRect(healthBarX, healthBarY, currentHealthWidth, healthBarHeight);
        
        // Border
        g2d.setColor(Color.BLACK);
        g2d.drawRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
        
        // Draw monster type name (Boss) with glow effect
        if (type == MonsterType.BOSS) {
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String name = "BOSS";
            int nameWidth = g2d.getFontMetrics().stringWidth(name);
            int nameX = screenX + (tileSize - nameWidth) / 2;
            int nameY = healthBarY - 5;
            
            // Glow effect
            g2d.setColor(new Color(255, 0, 0, 100));
            g2d.drawString(name, nameX - 1, nameY);
            g2d.drawString(name, nameX + 1, nameY);
            g2d.drawString(name, nameX, nameY - 1);
            g2d.drawString(name, nameX, nameY + 1);
            
            g2d.setColor(Color.RED);
            g2d.drawString(name, nameX, nameY);
        }
        
        // Draw difficulty indicator for scaled monsters
        if (difficultyMultiplier > 1.0f) {
            int level = (int)(difficultyMultiplier * 10 - 9);
            String lvl = "Lv." + level;
            
            // Background for level text
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g2d.getFontMetrics();
            int lvlWidth = fm.stringWidth(lvl);
            int lvlX = screenX + (tileSize - lvlWidth) / 2;
            int lvlY = screenY - 5;
            
            // Draw background pill
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRoundRect(lvlX - 4, lvlY - 10, lvlWidth + 8, 14, 7, 7);
            
            // Draw level text with color based on level
            if (level >= 5) {
                g2d.setColor(Color.RED);
            } else if (level >= 3) {
                g2d.setColor(Color.ORANGE);
            } else {
                g2d.setColor(Color.YELLOW);
            }
            g2d.drawString(lvl, lvlX, lvlY);
        }
    }
    
    @Override
    public void update(float delta) {
        // Called from outside with target player
    }

    @Override
    public void render(Graphics graphics) {
        // Use specific render method with parameters
    }    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getHealth() {
        return health;
    }
    
    public void setHealth(int health) {
        this.health = health;
    }
    
    public int getMaxHealth() {
        return maxHealth;
    }
    
    public int getDamage() {
        return damage;
    }
    
    public int getGoldReward() {
        return goldReward;
    }
    
    public boolean isAlive() {
        return isAlive;
    }
    
    public void setAlive(boolean alive) {
        isAlive = alive;
    }
    
    public MonsterType getType() {
        return type;
    }
    
    public int getAttackRange() {
        return attackRange;
    }
    
    public boolean canAttack() {
        return attackTimer <= 0 && isAlive && !isDying;
    }
    
    /**
     * Reset attack cooldown after dealing damage
     */
    public void resetAttackCooldown() {
        this.attackTimer = attackCooldown;
    }
    
    /**
     * Check if monster is currently playing death animation
     */
    public boolean isDying() {
        return isDying;
    }
    
    /**
     * Get difficulty multiplier
     */
    public float getDifficultyMultiplier() {
        return difficultyMultiplier;
    }
    
    /**
     * Set map bounds for movement restriction
     */
    public void setMapBounds(int minX, int maxX, int minY, int maxY) {
        this.mapMinX = minX;
        this.mapMaxX = maxX;
        this.mapMinY = minY;
        this.mapMaxY = maxY;
        
        // Clamp current position to new bounds
        worldX = Math.max(mapMinX, Math.min(worldX, mapMaxX - 48));
        worldY = Math.max(mapMinY, Math.min(worldY, mapMaxY - 48));
    }
    
    /**
     * Set difficulty multiplier and update stats
     */
    public void setDifficultyMultiplier(float multiplier) {
        this.difficultyMultiplier = multiplier;
        this.maxHealth = (int)(type.health * multiplier);
        this.health = this.maxHealth;
        this.damage = (int)(type.damage * multiplier);
        this.goldReward = (int)(type.goldReward * (1 + (multiplier - 1) * 0.5f));
    }
}
