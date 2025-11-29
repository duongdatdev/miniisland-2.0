package network.entitiesNet;

import input.MouseHandler;
import main.GameScene;
import network.client.Client;
import network.client.Protocol;
import objects.entities.Bullet;
import objects.entities.Bullet.BulletType;
import objects.entities.Player;
import panes.chat.DialogText;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class PlayerMP {
    private Player player;
    private int x, y;
    private int id;
    private int direction;
    private int lastDirection = 1;
    private String username;

    //Bullets - Optimized with ArrayList
    private ArrayList<Bullet> bullets = new ArrayList<>();
    private static final int MAX_BULLETS = 100; // Reasonable limit
    
    // === NEW: Shooting System ===
    private BulletType currentBulletType = BulletType.NORMAL;
    private long lastShotTime;
    private int shotCooldown = 250; // Base cooldown in ms
    private boolean autoFire = false;
    
    // === NEW: Aim Direction (for 8-way shooting) ===
    private int aimDirection = 1; // Current aim direction
    private boolean isAiming = false;
    
    // === NEW: Mouse aiming ===
    private boolean useMouseAiming = true; // Enable mouse aiming in PvP
    private float mouseAimX = 0;
    private float mouseAimY = 0;

    private BufferedImage chatImage;
    private Timer chatImageTimer;

    private DialogText dialogText;

    private boolean isAlive = true;

    public PlayerMP(Player player) {
        this.player = player;
        this.x = player.getWorldX();
        this.y = player.getWorldY();

        dialogText = new DialogText();

        lastShotTime = System.currentTimeMillis();

        chatImageTimer = new Timer(2000, e -> clearChatImage());
        chatImageTimer.setRepeats(false);
    }

    public PlayerMP(String username, int x, int y, int direction, int id) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.direction = direction;
        this.username = username;
        this.player = new Player(username, x, y, direction, id);

        dialogText = new DialogText();

        lastShotTime = System.currentTimeMillis();

        chatImageTimer = new Timer(2000, e -> clearChatImage());
        chatImageTimer.setRepeats(false);
    }

    //Counter for the number of times the player has moved
    int count = 1;

    /**
     * Updates the player's position and direction
     */
    public void update() {
        if (player.getWorldX() != x || player.getWorldY() != y || player.getId() != id) {
            player.setId(id);
            x = player.getWorldX();
            y = player.getWorldY();
        }
        if (player.isMove() && !player.isCollision()) {
            x = player.getWorldX();
            y = player.getWorldY();
            switch (player.getDirection()) {
                case "DOWN" -> direction = 1;
                case "UP" -> direction = 2;
                case "LEFT" -> direction = 3;
                case "RIGHT" -> direction = 4;
            }
            updatePlayerInServer();

            count = 1;
        } else {
            if (count == 1) {
                if (player.getDirection().equals("STAND")) {
                    lastDirection = direction;
                    direction = 0;
                }
                updatePlayerInServer();

                count = 0;
            }
        }

        player.setWorldX(x);
        player.setWorldY(y);
        player.setId(id);
        switch (direction) {
            case 1 -> player.setDirection("DOWN");
            case 2 -> player.setDirection("UP");
            case 3 -> player.setDirection("LEFT");
            case 4 -> player.setDirection("RIGHT");
            default -> player.setDirection("STAND");
        }
    }

    public void updateLocation() {
        x = player.getWorldX();
        y = player.getWorldY();
    }

    public void updatePlayerInServer() {
        Client.getGameClient().sendToServer(new Protocol().UpdatePacket(username, x, y, direction));
    }

    public void render(Graphics2D g2d, int tileSize) {
        // Calculate the center of the player's sprite
        int centerX = player.getScreenX() + tileSize / 2;

        // Get the width of the username string
        int stringWidth = g2d.getFontMetrics().stringWidth(username);

        // Adjust the x-coordinate of the username to center it
        int usernameX = centerX - stringWidth / 2;

        // Draw the username at the calculated position
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString(username, usernameX, player.getScreenY());
        player.render(g2d, tileSize);
        if (chatImage != null) {
            g2d.drawImage(chatImage, player.getScreenX() - 50, player.getScreenY() - chatImage.getHeight() - 5, null);
        }
    }

    public void sendToServer(String message) {
        if (message.equals("exit"))
            System.exit(0);
        else {
            Client.getGameClient().sendToServer(message);
        }
    }

    public void shot() {
        long currentTime = System.currentTimeMillis();
        int actualCooldown = getCooldownForBulletType();
        
        if (currentTime - lastShotTime >= actualCooldown) {
            String currentMap = GameScene.getInstance().getCurrentMap();
            // Allow shooting in both pvp mode (Score Battle) and original pvp
            if (currentMap.equals("pvp")) {
                // Clean up stopped bullets before adding new ones
                bullets.removeIf(b -> b.stop);
                
                if (bullets.size() < MAX_BULLETS) {
                    GameScene gs = GameScene.getInstance();
                    MouseHandler mouse = gs.getMouseHandler();
                    
                    Bullet newBullet;
                    
                    // Use mouse aiming if enabled and in PvP
                    if (useMouseAiming && mouse != null) {
                        // Create bullet with mouse direction
                        newBullet = new Bullet(
                            this.getX(), this.getY(),
                            mouse.getAimDirectionX(), mouse.getAimDirectionY(),
                            username, currentBulletType
                        );
                    } else {
                        // Fallback to keyboard direction
                        int bombDirection = getShootDirection();
                        newBullet = new Bullet(this.getX(), this.getY(), bombDirection, username, currentBulletType);
                    }
                    
                    // Apply damage multiplier from PvP map if available
                    if (gs.getPvpMap() != null && gs.getPvpMap().getDamageMultiplier() > 1.0f) {
                        newBullet.setDamage((int)(newBullet.getDamage() * gs.getPvpMap().getDamageMultiplier()));
                    }
                    
                    newBullet.startBombThread(true);
                    bullets.add(newBullet);
                    
                    Client.getGameClient().sendToServer(new Protocol().ShotPacket(username));
                }
            }
            lastShotTime = currentTime;
        }
    }
    
    /**
     * Shoot with mouse - called when left mouse is held
     */
    public void shootWithMouse() {
        GameScene gs = GameScene.getInstance();
        if (gs.getCurrentMap().equals("pvp")) {
            shot();
        }
    }
    
    /**
     * Enable/disable mouse aiming
     */
    public void setUseMouseAiming(boolean use) {
        this.useMouseAiming = use;
    }
    
    public boolean isUseMouseAiming() {
        return useMouseAiming;
    }
    
    /**
     * Get shoot direction supporting 8-way shooting
     */
    private int getShootDirection() {
        // Use aim direction if explicitly aiming, otherwise use movement direction
        if (isAiming) {
            return aimDirection;
        }
        return direction != 0 ? direction : lastDirection;
    }
    
    /**
     * Get cooldown based on bullet type
     */
    private int getCooldownForBulletType() {
        switch (currentBulletType) {
            case RAPID:
                return 150; // Faster shooting
            case HEAVY:
                return 400; // Slower shooting
            case PIERCING:
                return 350;
            default:
                return shotCooldown;
        }
    }
    
    /**
     * Set aim direction for 8-way shooting
     */
    public void setAimDirection(int horizontal, int vertical) {
        isAiming = true;
        
        if (horizontal == 0 && vertical < 0) {
            aimDirection = Bullet.DIR_UP;
        } else if (horizontal == 0 && vertical > 0) {
            aimDirection = Bullet.DIR_DOWN;
        } else if (horizontal < 0 && vertical == 0) {
            aimDirection = Bullet.DIR_LEFT;
        } else if (horizontal > 0 && vertical == 0) {
            aimDirection = Bullet.DIR_RIGHT;
        } else if (horizontal < 0 && vertical < 0) {
            aimDirection = Bullet.DIR_UP_LEFT;
        } else if (horizontal > 0 && vertical < 0) {
            aimDirection = Bullet.DIR_UP_RIGHT;
        } else if (horizontal < 0 && vertical > 0) {
            aimDirection = Bullet.DIR_DOWN_LEFT;
        } else if (horizontal > 0 && vertical > 0) {
            aimDirection = Bullet.DIR_DOWN_RIGHT;
        }
    }
    
    /**
     * Stop aiming (will use movement direction)
     */
    public void stopAiming() {
        isAiming = false;
    }
    
    /**
     * Cycle through bullet types
     */
    public void cycleBulletType() {
        BulletType[] types = BulletType.values();
        int currentIndex = currentBulletType.ordinal();
        currentBulletType = types[(currentIndex + 1) % types.length];
    }
    
    /**
     * Set specific bullet type
     */
    public void setBulletType(BulletType type) {
        this.currentBulletType = type;
    }
    
    public BulletType getCurrentBulletType() {
        return currentBulletType;
    }

    public void Shot() {
        // Clean up stopped bullets
        bullets.removeIf(b -> b.stop);
        
        if (bullets.size() < MAX_BULLETS) {
            int bombDirection = direction != 0 ? direction : lastDirection;
            Bullet newBullet = new Bullet(this.getX(), this.getY(), bombDirection, username);
            newBullet.startBombThread(false);
            bullets.add(newBullet);
        }
    }

    public int getDirection() {
        switch (player.getDirection()) {

            case "DOWN" -> direction = 1;
            case "UP" -> direction = 2;
            case "LEFT" -> direction = 3;
            case "RIGHT" -> direction = 4;
            case "STAND" -> direction = 0;

        }
        return direction;
    }

    private void clearChatImage() {
        this.chatImage = null;
    }

    public void setDirection(int direction) {
        switch (direction) {

            case 1 -> player.setDirection("DOWN");
            case 2 -> player.setDirection("UP");
            case 3 -> player.setDirection("LEFT");
            case 4 -> player.setDirection("RIGHT");
            default -> player.setDirection("STAND");
        }
        this.direction = direction;
    }

    public ArrayList<Bullet> getBullets() {
        return bullets;
    }
    
    // Deprecated: Keep for backward compatibility, but use getBullets() instead
    @Deprecated
    public Bullet[] getBullet() {
        return bullets.toArray(new Bullet[0]);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getX() {
        x = player.getWorldX();
        return x;
    }

    public void setX(int x) {
        player.setWorldX(x);
        this.x = x;
    }

    public int getY() {
        y = player.getWorldY();
        return y;
    }

    public void setY(int y) {
        player.setWorldY(y);
        this.y = y;
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        player.setId(id);
        this.id = id;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public BufferedImage getChatImage() {
        return chatImage;
    }

    public void setChatImage(BufferedImage chatImage) {
        this.chatImage = chatImage;
        chatImageTimer.restart();
    }

    public DialogText getDialogText() {
        return dialogText;
    }

    public void setDialogText(DialogText dialogText) {
        this.dialogText = dialogText;
    }

    public int getLastDirection() {
        return lastDirection;
    }

    public void setLastDirection(int lastDirection) {
        this.lastDirection = lastDirection;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }
    
    /**
     * Change skin for this player
     * @param skinFolder folder containing skin (e.g., "1", "2")
     */
    public void changeSkin(String skinFolder) {
        if (player != null) {
            player.changeSkin(skinFolder);
            System.out.println("PlayerMP " + username + " changed skin to: " + skinFolder);
        }
    }
}
