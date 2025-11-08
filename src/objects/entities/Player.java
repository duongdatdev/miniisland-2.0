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

    public Player(GameScene gameScene, KeyHandler keyHandler) {
        this.keyHandler = keyHandler;
        this.gameScene = gameScene;

        hitBox = new Rectangle();
        // Based on actual sprite visible in game
        // Character body occupies roughly center 16-18px of 32px tile
        // Need larger hitbox to prevent clipping into walls
        hitBox.width = 18;   // Wider body to match sprite (~56% of tile)
        hitBox.height = 24;  // Taller to cover head to feet (~75% of tile)
        hitBox.x = 7;        // Center: (32-18)/2 = 7
        hitBox.y = 6;        // Start slightly below top to account for head

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

    public void setDefaultPosition() {
        screenX = gameScene.getScreenWidth() / 2 - gameScene.getTileSize() * scale / 2;
        screenY = gameScene.getScreenHeight() / 2 - gameScene.getTileSize() * scale / 2;
        worldX = 1645;
        worldY = 754;
    }

    private void setDefaultSpeed() {
        speed = 3;
    }

    //Counter for the number of times the player has moved
    int count = 1;

    /**
     * Updates the player's position and direction
     */
    public void update() {

        if (isMove()) {

            // Store old position for rollback
            int oldX = worldX;
            int oldY = worldY;
            
            int futureX = worldX;
            int futureY = worldY;
            
            boolean movingX = false;
            boolean movingY = false;

            if (keyHandler.isUp()) {
                direction = "UP";
                futureY -= speed;
                movingY = true;
            }
            if (keyHandler.isDown()) {
                direction = "DOWN";
                futureY += speed;
                movingY = true;
            }
            if (keyHandler.isLeft()) {
                direction = "LEFT";
                futureX -= speed;
                movingX = true;
            }
            if (keyHandler.isRight()) {
                direction = "RIGHT";
                futureX += speed;
                movingX = true;
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


        } else if (isSpace()) {
            gameScene.getPlayerMP().shot();
        } else {
            if (count == 1) {
                lastDirection = direction;
                direction = "STAND";
            }
            count = 0;
        }

    }

    public void render(Graphics2D g2d, int tileSize) {
        g2d.drawImage(currentSprite(), screenX, screenY, tileSize * scale, tileSize * scale, null);
        
        // DEBUG: Draw hitbox to check collision (toggle by comment/uncomment)
        // Uncomment the 4 lines below to see red hitbox in game:
        g2d.setColor(new Color(255, 0, 0, 100)); // Red semi-transparent
        g2d.fillRect(screenX + hitBox.x, screenY + hitBox.y, hitBox.width, hitBox.height);
        g2d.setColor(Color.RED);
        g2d.drawRect(screenX + hitBox.x, screenY + hitBox.y, hitBox.width, hitBox.height);
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

    @Override
    public void update(float delta) {

    }

    @Override
    public void render(Graphics graphics) {

    }
}
