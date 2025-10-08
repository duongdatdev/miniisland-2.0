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
        hitBox.width = 28;
        hitBox.height = 18;
        hitBox.x = 10;
        hitBox.y = 24;

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

            int futureX = worldX;
            int futureY = worldY;

            if (keyHandler.isUp()) {
                direction = "UP";
                futureY -= speed;
            }
            if (keyHandler.isDown()) {
                direction = "DOWN";
                futureY += speed;
            }
            if (keyHandler.isLeft()) {
                direction = "LEFT";
                futureX -= speed;
            }
            if (keyHandler.isRight()) {
                direction = "RIGHT";
                futureX += speed;
            }

            count = 1;
            collision = false;
            flagUpdate = true;

            int finalFutureX = futureX;
            int finalFutureY = futureY;
            gameScene.getCollisionChecker().checkTile(this, () -> {
                if (!collision) {
                    // Check if the current tile is a wall
                    int currentTileNum = gameScene.getMap().getMapTileNum()[finalFutureX / gameScene.getTileSize()][finalFutureY / gameScene.getTileSize()];
                    if (gameScene.getMap().getTiles()[currentTileNum].getType() != TileType.Wall) {
                        // Move player in the opposite direction
                        if (flagUpdate) {
                            worldX = finalFutureX;
                            worldY = finalFutureY;
                        }
                    }
//                } else {
//                    direction = "STAND";
                }
            });


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
    }

    public boolean isSpace() {
        return keyHandler.isSpace();
    }

//    public void shot() {
//        Bullet bullet = new Bullet(worldX, worldY, lastDirection);
//        bullets.add(bullet);
//    }

    public BufferedImage currentSprite() {
        BufferedImage playerImage = null;
        countFrames++;
        if (countFrames > 11) {
            spriteIndex++;
            if (spriteIndex > 3) {
                spriteIndex = 0;
            }
            countFrames = 0;
        }
        switch (direction) {
            case "UP":
                if (upImages == null) {
                    System.err.println("[Player] upImages is null! Returning null for playerImage.");
                    return null;
                }
                if (spriteIndex == 0) {
                    playerImage = upImages[0];
                }
                if (spriteIndex == 1) {
                    playerImage = upImages[1];
                }
                if (spriteIndex == 2) {
                    playerImage = upImages[2];
                }
                if (spriteIndex == 3) {
                    playerImage = upImages[3];
                }
                break;
            case "DOWN":
                if (downImages == null) {
                    System.err.println("[Player] downImages is null! Returning null for playerImage.");
                    return null;
                }
                if (spriteIndex == 0) {
                    playerImage = downImages[0];
                }
                if (spriteIndex == 1) {
                    playerImage = downImages[1];
                }
                if (spriteIndex == 2) {
                    playerImage = downImages[2];
                }
                if (spriteIndex == 3) {
                    playerImage = downImages[3];
                }
                break;
            case "LEFT":
                if (leftImages == null) {
                    System.err.println("[Player] leftImages is null! Returning null for playerImage.");
                    return null;
                }
                if (spriteIndex == 0) {
                    playerImage = leftImages[0];
                }
                if (spriteIndex == 1) {
                    playerImage = leftImages[1];
                }
                if (spriteIndex == 2) {
                    playerImage = leftImages[2];
                }
                if (spriteIndex == 3) {
                    playerImage = leftImages[3];
                }
                break;
            case "RIGHT":
                if (rightImages == null) {
                    System.err.println("[Player] rightImages is null! Returning null for playerImage.");
                    return null;
                }
                if (spriteIndex == 0) {
                    playerImage = rightImages[0];
                }
                if (spriteIndex == 1) {
                    playerImage = rightImages[1];
                }
                if (spriteIndex == 2) {
                    playerImage = rightImages[2];
                }
                if (spriteIndex == 3) {
                    playerImage = rightImages[3];
                }
                break;
            case "STAND":
                if (standingImages == null) {
                    System.err.println("[Player] standingImages is null! Returning null for playerImage.");
                    return null;
                }
                if (spriteIndex == 0) {
                    playerImage = standingImages[0];
                }
                if (spriteIndex == 1) {
                    playerImage = standingImages[1];
                }
                if (spriteIndex == 2) {
                    playerImage = standingImages[2];
                }
                if (spriteIndex == 3) {
                    playerImage = standingImages[3];
                }
                break;
        }
        return playerImage;
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
