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
    private float velocity = 10.0f; // Increased velocity for realistic movement

    private String playerShot;
    
    // Bullet damage
    private int damage = 25;

    public Bullet(int x, int y, int direction, String playerShot) {
        xPosi = x;
        yPosi = y;
        this.direction = direction;
        stop = false;
        try {
            bulletImg = ImageIO.read(getClass().getResource("/player/Bomb/bomb.PNG"));
            bulletBuffImage = ImageIO.read(getClass().getResource("/player/Bomb/bomb.PNG"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.playerShot = playerShot;
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
        int maxDistance = 400; // Maximum distance a bomb can travel

        public BombShotThread(boolean chCollision) {
            checkCollis = chCollision;
        }

        public void run() {
            while (!stop && distanceTraveled < maxDistance) {
                int oldXPosi = xPosi;
                int oldYPosi = yPosi;

                switch (direction) {
                    case 2 -> yPosi -= velocity; // Up
                    case 1 -> yPosi += velocity; // Down
                    case 3 -> xPosi -= velocity; // Left
                    case 4 -> xPosi += velocity; // Right
                }

                distanceTraveled += Math.sqrt(Math.pow(xPosi - oldXPosi, 2) + Math.pow(yPosi - oldYPosi, 2));

                // Kiểm tra collision với quái trong Score Battle mode
                int goldEarned = checkMonsterCollision();
                if (goldEarned > 0) {
                    GameScene.getInstance().getPvpMap().addScore(goldEarned);
                }

                if (checkCollis && checkCollision()) {
                    stop = true;
                }

                try {
                    Thread.sleep(40);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            stop = true;
        }
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
}
