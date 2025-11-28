package objects.entities;

import main.GameScene;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 * Monster class represents monsters in Score Battle mode.
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
     * Initialize sprites for all monster types (only runs once)
     */
    private static synchronized void initSpritesIfNeeded() {
        if (spritesInitialized) return;
        
        for (MonsterType t : MonsterType.values()) {
            BufferedImage[] sprites = new BufferedImage[4];
            createFallbackSpritesForType(sprites, t);
            spriteCache.put(t, sprites);
        }
        
        spritesInitialized = true;
    }
    
    /**
     * Get sprites for current monster type
     */
    private BufferedImage[] getSprites() {
        return spriteCache.get(type);
    }
    
    private static void createFallbackSpritesForType(BufferedImage[] sprites, MonsterType type) {
        int size = 48;
        Color color = getColorForType(type);
        
        for (int i = 0; i < 4; i++) {
            sprites[i] = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sprites[i].createGraphics();
            
            // Draw body
            g.setColor(color);
            g.fillOval(4, 8, size - 8, size - 16);
            
            // Draw eyes
            g.setColor(Color.WHITE);
            g.fillOval(12, 16, 8, 8);
            g.fillOval(28, 16, 8, 8);
            
            g.setColor(Color.BLACK);
            g.fillOval(14 + (i % 2), 18, 4, 4);
            g.fillOval(30 + (i % 2), 18, 4, 4);
            
            g.dispose();
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
        if (!isAlive) return;
        
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
            
            // Check attack
            if (distance < attackRange && attackTimer <= 0) {
                attackTimer = attackCooldown;
            }
        } else {
            // Random movement
            randomMove();
        }
    }
    
    private void chasePlayer(int playerX, int playerY) {
        int dx = playerX - worldX;
        int dy = playerY - worldY;
        
        // Normalize and move
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length > 0) {
            worldX += (int) (speed * dx / length);
            worldY += (int) (speed * dy / length);
            
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
        
        switch (moveDirection) {
            case 1: // Down
                worldY += speed;
                direction = "DOWN";
                break;
            case 2: // Up
                worldY -= speed;
                direction = "UP";
                break;
            case 3: // Left
                worldX -= speed;
                direction = "LEFT";
                break;
            case 4: // Right
                worldX += speed;
                direction = "RIGHT";
                break;
        }
        
        // Keep monster within map bounds (assuming 50x50 tiles map, 48px per tile)
        int mapWidth = 50 * 48;
        int mapHeight = 50 * 48;
        worldX = Math.max(50, Math.min(worldX, mapWidth - 100));
        worldY = Math.max(50, Math.min(worldY, mapHeight - 100));
    }
    
    /**
     * Take damage from bullet
     */
    public boolean takeDamage(int damage) {
        if (!isAlive) return false;
        
        health -= damage;
        if (health <= 0) {
            health = 0;
            isAlive = false;
            return true; // Monster died
        }
        return false;
    }
    
    /**
     * Check collision with player
     */
    public boolean checkPlayerCollision(Player player) {
        if (!isAlive) return false;
        
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
        if (!isAlive || bullet.isStop()) return false;
        
        Rectangle monsterRect = new Rectangle(worldX + hitBox.x, worldY + hitBox.y, hitBox.width, hitBox.height);
        Rectangle bulletRect = new Rectangle(bullet.getPosiX(), bullet.getPosiY(), 10, 10);
        
        return monsterRect.intersects(bulletRect);
    }
    
    /**
     * Render monster on screen
     */
    public void render(Graphics2D g2d, int screenX, int screenY, int tileSize) {
        if (!isAlive) return;
        
        // Draw sprite from cache
        BufferedImage[] sprites = getSprites();
        if (sprites != null && sprites[spriteIndex] != null) {
            g2d.drawImage(sprites[spriteIndex], screenX, screenY, tileSize, tileSize, null);
        }
        
        // Draw health bar
        int healthBarWidth = 40;
        int healthBarHeight = 6;
        int healthBarX = screenX + (tileSize - healthBarWidth) / 2;
        int healthBarY = screenY - 10;
        
        // Background (red)
        g2d.setColor(Color.RED);
        g2d.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
        
        // Health (green)
        int currentHealthWidth = (int) ((double) health / maxHealth * healthBarWidth);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(healthBarX, healthBarY, currentHealthWidth, healthBarHeight);
        
        // Border
        g2d.setColor(Color.BLACK);
        g2d.drawRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
        
        // Draw monster type name (Boss)
        if (type == MonsterType.BOSS) {
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.setColor(Color.RED);
            String name = "BOSS";
            int nameWidth = g2d.getFontMetrics().stringWidth(name);
            g2d.drawString(name, screenX + (tileSize - nameWidth) / 2, healthBarY - 5);
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
        return attackTimer <= 0 && isAlive;
    }
}
