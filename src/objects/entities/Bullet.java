package objects.entities;

import main.GameScene;
import maps.PvpMap;
import network.client.Client;
import network.client.Protocol;
import network.entitiesNet.PlayerMP;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

public class Bullet {
    private Image bulletImg;
    private BufferedImage bulletBuffImage;

    private int xPosi;
    private int yPosi;
    private int direction;
    public boolean stop = false;
    private float velocity = 12.0f; // Faster bullet speed
    
    // Velocity components for diagonal movement
    private float velocityX = 0;
    private float velocityY = 0;

    private String playerShot;
    
    // Bullet damage
    private int damage = 25;
    
    // Bullet type for different effects
    private BulletType type = BulletType.NORMAL;
    
    // Trail effect
    private ArrayList<int[]> trail = new ArrayList<>();
    private static final int MAX_TRAIL_LENGTH = 5;
    
    public enum BulletType {
        NORMAL(25, 12.0f, 400, new Color(255, 200, 50)),      // Standard bullet
        RAPID(15, 15.0f, 300, new Color(100, 200, 255)),      // Fast, less damage
        HEAVY(40, 8.0f, 350, new Color(255, 100, 50)),        // Slow, more damage
        PIERCING(20, 14.0f, 500, new Color(200, 50, 255));    // Goes through enemies
        
        public final int damage;
        public final float speed;
        public final int range;
        public final Color color;
        
        BulletType(int damage, float speed, int range, Color color) {
            this.damage = damage;
            this.speed = speed;
            this.range = range;
            this.color = color;
        }
    }
    
    // Direction constants for 8-way shooting
    public static final int DIR_UP = 2;
    public static final int DIR_DOWN = 1;
    public static final int DIR_LEFT = 3;
    public static final int DIR_RIGHT = 4;
    public static final int DIR_UP_LEFT = 5;
    public static final int DIR_UP_RIGHT = 6;
    public static final int DIR_DOWN_LEFT = 7;
    public static final int DIR_DOWN_RIGHT = 8;

    public Bullet(int x, int y, int direction, String playerShot) {
        this(x, y, direction, playerShot, BulletType.NORMAL);
    }
    
    public Bullet(int x, int y, int direction, String playerShot, BulletType type) {
        xPosi = x + 20; // Center offset
        yPosi = y + 20;
        this.direction = direction;
        this.type = type;
        this.damage = type.damage;
        this.velocity = type.speed;
        stop = false;
        
        // Calculate velocity components based on direction
        calculateVelocity();
        
        try {
            bulletImg = ImageIO.read(getClass().getResource("/player/Bomb/bomb.PNG"));
            bulletBuffImage = ImageIO.read(getClass().getResource("/player/Bomb/bomb.PNG"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.playerShot = playerShot;
    }
    
    /**
     * Constructor for mouse-aimed bullets with precise direction
     * @param x Player X position
     * @param y Player Y position
     * @param dirX Normalized direction X (-1 to 1)
     * @param dirY Normalized direction Y (-1 to 1)
     * @param playerShot Player username
     * @param type Bullet type
     */
    public Bullet(int x, int y, float dirX, float dirY, String playerShot, BulletType type) {
        xPosi = x + 20; // Center offset
        yPosi = y + 20;
        this.type = type;
        this.damage = type.damage;
        this.velocity = type.speed;
        stop = false;
        
        // Normalize the direction vector
        float magnitude = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (magnitude > 0) {
            dirX /= magnitude;
            dirY /= magnitude;
        } else {
            // Default to down if no direction
            dirX = 0;
            dirY = 1;
        }
        
        // Set velocity directly from normalized direction
        velocityX = dirX * velocity;
        velocityY = dirY * velocity;
        
        // Determine integer direction for rendering purposes
        this.direction = calculateDirectionFromVector(dirX, dirY);
        
        try {
            bulletImg = ImageIO.read(getClass().getResource("/player/Bomb/bomb.PNG"));
            bulletBuffImage = ImageIO.read(getClass().getResource("/player/Bomb/bomb.PNG"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.playerShot = playerShot;
    }
    
    /**
     * Convert direction vector to 8-direction integer
     */
    private int calculateDirectionFromVector(float dirX, float dirY) {
        double angle = Math.atan2(dirY, dirX);
        double degrees = Math.toDegrees(angle);
        
        if (degrees < 0) degrees += 360;
        
        if (degrees >= 337.5 || degrees < 22.5) return DIR_RIGHT;
        if (degrees >= 22.5 && degrees < 67.5) return DIR_DOWN_RIGHT;
        if (degrees >= 67.5 && degrees < 112.5) return DIR_DOWN;
        if (degrees >= 112.5 && degrees < 157.5) return DIR_DOWN_LEFT;
        if (degrees >= 157.5 && degrees < 202.5) return DIR_LEFT;
        if (degrees >= 202.5 && degrees < 247.5) return DIR_UP_LEFT;
        if (degrees >= 247.5 && degrees < 292.5) return DIR_UP;
        if (degrees >= 292.5 && degrees < 337.5) return DIR_UP_RIGHT;
        
        return DIR_DOWN;
    }
    
    /**
     * Calculate velocity components for 8-directional movement
     */
    private void calculateVelocity() {
        float diagonalFactor = 0.707f; // 1/sqrt(2) for diagonal movement
        
        switch (direction) {
            case DIR_UP:
                velocityX = 0;
                velocityY = -velocity;
                break;
            case DIR_DOWN:
                velocityX = 0;
                velocityY = velocity;
                break;
            case DIR_LEFT:
                velocityX = -velocity;
                velocityY = 0;
                break;
            case DIR_RIGHT:
                velocityX = velocity;
                velocityY = 0;
                break;
            case DIR_UP_LEFT:
                velocityX = -velocity * diagonalFactor;
                velocityY = -velocity * diagonalFactor;
                break;
            case DIR_UP_RIGHT:
                velocityX = velocity * diagonalFactor;
                velocityY = -velocity * diagonalFactor;
                break;
            case DIR_DOWN_LEFT:
                velocityX = -velocity * diagonalFactor;
                velocityY = velocity * diagonalFactor;
                break;
            case DIR_DOWN_RIGHT:
                velocityX = velocity * diagonalFactor;
                velocityY = velocity * diagonalFactor;
                break;
            default:
                velocityX = 0;
                velocityY = velocity;
        }
    }

    public int getPosiX() {
        return xPosi;
    }

    public int getPosiY() {
        return yPosi;
    }

    public void setPosiX(int x) {
        xPosi = x;
    }

    public void setPosiY(int y) {
        yPosi = y;
    }

    public BufferedImage getBulletBuffImage() {
        return bulletBuffImage;
    }

    public boolean checkCollision() {
        ArrayList<PlayerMP> clientPlayers = GameScene.getInstance().getMap().players;
        for (PlayerMP player : clientPlayers) {
            int x = player.getX();
            int y = player.getY();
            if ((yPosi >= y && yPosi <= y + 43) && (xPosi >= x && xPosi <= x + 43)) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                GameScene.getInstance().getMap().removePlayer(player.getUsername());
                if (player.isAlive()) {
                    Client.getGameClient().sendToServer(new Protocol().bulletCollisionPacket(playerShot, player.getUsername()));
                }
                player.setAlive(false);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Kiểm tra va chạm với quái vật trong chế độ Score Battle
     * @return gold earned if monster killed, 0 otherwise
     */
    public int checkMonsterCollision() {
        if (stop) return 0;
        
        GameScene gameScene = GameScene.getInstance();
        if (!gameScene.getCurrentMap().equals("pvp")) return 0;
        
        PvpMap pvpMap = gameScene.getPvpMap();
        if (!pvpMap.isGameStarted()) return 0;
        
        MonsterSpawner spawner = pvpMap.getMonsterSpawner();
        
        for (Monster monster : spawner.getMonsters()) {
            if (!monster.isAlive()) continue;
            
            if (monster.checkBulletCollision(this)) {
                stop = true;
                
                // Monster nhận damage
                if (monster.takeDamage(damage)) {
                    // Monster died - return gold reward
                    int goldEarned = monster.getGoldReward();
                    
                    // Gửi thông báo lên server
                    Client.getGameClient().sendToServer(
                        new Protocol().monsterKillPacket(playerShot, monster.getId(), goldEarned)
                    );
                    
                    return goldEarned;
                }
                return 0;
            }
        }
        return 0;
    }

    public void startBombThread(boolean checkCollision) {
        new BombShotThread(checkCollision).start();
    }

    private class BombShotThread extends Thread {
        boolean checkCollis;
        int distanceTraveled = 0;
        int maxDistance;
        int enemiesPierced = 0;
        int maxPierce = (type == BulletType.PIERCING) ? 3 : 1;

        public BombShotThread(boolean chCollision) {
            checkCollis = chCollision;
            maxDistance = type.range;
        }

        public void run() {
            while (!stop && distanceTraveled < maxDistance) {
                // Store old position for trail
                if (trail.size() >= MAX_TRAIL_LENGTH) {
                    trail.remove(0);
                }
                trail.add(new int[]{xPosi, yPosi});
                
                int oldXPosi = xPosi;
                int oldYPosi = yPosi;

                // Use velocity components for smooth diagonal movement
                xPosi += (int) velocityX;
                yPosi += (int) velocityY;

                distanceTraveled += Math.sqrt(Math.pow(xPosi - oldXPosi, 2) + Math.pow(yPosi - oldYPosi, 2));

                // Kiểm tra collision với quái trong Score Battle mode
                int goldEarned = checkMonsterCollision();
                if (goldEarned > 0) {
                    GameScene.getInstance().getPvpMap().addScore(goldEarned);
                    enemiesPierced++;
                    
                    // Stop if not piercing or pierced enough enemies
                    if (type != BulletType.PIERCING || enemiesPierced >= maxPierce) {
                        stop = true;
                    }
                }

                if (checkCollis && checkCollision()) {
                    stop = true;
                }

                try {
                    Thread.sleep(30); // Faster update for smoother movement
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            stop = true;
        }
    }
    
    /**
     * Render bullet with trail effect
     */
    public void render(Graphics2D g2d, int screenX, int screenY) {
        if (stop) return;
        
        // Draw trail
        for (int i = 0; i < trail.size(); i++) {
            float alpha = (float) (i + 1) / trail.size() * 0.5f;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setColor(type.color);
            int size = 4 + i;
            // Note: Trail positions would need screen coordinate conversion
        }
        
        // Draw main bullet
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        
        // Glow effect
        g2d.setColor(new Color(type.color.getRed(), type.color.getGreen(), type.color.getBlue(), 100));
        g2d.fillOval(screenX - 4, screenY - 4, 18, 18);
        
        // Main bullet
        g2d.setColor(type.color);
        g2d.fillOval(screenX, screenY, 10, 10);
        
        // Highlight
        g2d.setColor(Color.WHITE);
        g2d.fillOval(screenX + 2, screenY + 2, 4, 4);
    }

    public Image getBulletImg() {
        return bulletImg;
    }

    public void setBulletImg(Image bulletImg) {
        this.bulletImg = bulletImg;
    }

    public void setBulletBuffImage(BufferedImage bulletBuffImage) {
        this.bulletBuffImage = bulletBuffImage;
    }

    public int getxPosi() {
        return xPosi;
    }

    public void setxPosi(int xPosi) {
        this.xPosi = xPosi;
    }

    public int getyPosi() {
        return yPosi;
    }

    public void setyPosi(int yPosi) {
        this.yPosi = yPosi;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public float getVelocity() {
        return velocity;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }
    
    public int getDamage() {
        return damage;
    }
    
    public void setDamage(int damage) {
        this.damage = damage;
    }
    
    public BulletType getType() {
        return type;
    }
    
    public void setType(BulletType type) {
        this.type = type;
        this.damage = type.damage;
        this.velocity = type.speed;
        calculateVelocity();
    }
    
    public ArrayList<int[]> getTrail() {
        return trail;
    }
}
