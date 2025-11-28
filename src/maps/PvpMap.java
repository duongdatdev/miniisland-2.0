package maps;

import main.GameScene;
import network.client.Client;
import network.client.Protocol;
import objects.entities.Bullet;
import objects.entities.Monster;
import objects.entities.MonsterSpawner;
import objects.entities.Player;

import java.awt.*;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * PvpMap - Chế độ Đối kháng điểm số
 * Nhiều người chơi cùng xuất hiện trong một bản đồ top-down.
 * Người chơi bắn quái để nhận vàng trong thời gian giới hạn.
 * Quái có thể di chuyển và tấn công người chơi.
 */
public class PvpMap extends Map {
    private int startX;
    private int startY;

    private Client client;
    
    // Monster spawner
    private MonsterSpawner monsterSpawner;
    
    // Score system
    private HashMap<String, Integer> playerScores; // username -> gold
    private int localPlayerScore = 0;
    
    // Timer system
    private int gameTimeLimit = 180; // 3 minutes in seconds
    private int remainingTime = gameTimeLimit;
    private long lastTimeUpdate = 0;
    private boolean gameStarted = false;
    private boolean gameEnded = false;
    
    // Player health system
    private int playerHealth = 100;
    private int maxPlayerHealth = 100;
    private long lastDamageTime = 0;
    private int damageCooldown = 1000; // 1 second immunity after damage

    public PvpMap(GameScene gameScene) {
        super(gameScene);
        mapTileCol = 50;
        mapTileRow = 50;
        mapTileNum = new int[mapTileCol][mapTileRow];
        loadMap("/Maps/Pvp/pvpMap.png");
        readMap("/Maps/Pvp/pvpMap.csv");
        
        // Initialize monster spawner
        monsterSpawner = new MonsterSpawner(gameScene);
        monsterSpawner.setMapBounds(100, 2300, 100, 2300);
        
        // Initialize score system
        playerScores = new HashMap<>();
    }
    
    /**
     * Bắt đầu game mode
     */
    public void startGame() {
        gameStarted = true;
        gameEnded = false;
        remainingTime = gameTimeLimit;
        lastTimeUpdate = System.currentTimeMillis();
        localPlayerScore = 0;
        playerHealth = maxPlayerHealth;
        playerScores.clear();
        
        monsterSpawner.start();
    }
    
    /**
     * Kết thúc game mode
     */
    public void endGame() {
        gameStarted = false;
        gameEnded = true;
        monsterSpawner.stop();
    }
    
    /**
     * Reset game state khi rời map
     */
    public void resetGame() {
        gameStarted = false;
        gameEnded = false;
        remainingTime = gameTimeLimit;
        localPlayerScore = 0;
        playerHealth = maxPlayerHealth;
        playerScores.clear();
        monsterSpawner.stop();
    }
    
    /**
     * Update game logic - gọi mỗi frame
     */
    public void update() {
        if (!gameStarted || gameEnded) return;
        
        Player player = gameScene.getPlayer();
        
        // Update timer
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTimeUpdate >= 1000) {
            remainingTime--;
            lastTimeUpdate = currentTime;
            
            if (remainingTime <= 0) {
                endGame();
                return;
            }
        }
        
        // Update monsters
        monsterSpawner.update(player);
        
        // Check bullet-monster collisions
        checkBulletMonsterCollisions();
        
        // Check player-monster collisions
        checkPlayerMonsterCollision();
    }
    
    /**
     * Kiểm tra va chạm đạn với quái
     */
    private void checkBulletMonsterCollisions() {
        String username = gameScene.getPlayerMP().getUsername();
        
        // Check bullets from local player
        for (Bullet bullet : gameScene.getPlayerMP().getBullets()) {
            if (bullet != null && !bullet.isStop()) {
                int goldEarned = monsterSpawner.checkBulletCollision(bullet, username);
                if (goldEarned > 0) {
                    addScore(goldEarned);
                    // Send score update to server
                    Client.getGameClient().sendToServer(
                        new Protocol().scoreUpdatePacket(username, localPlayerScore)
                    );
                }
            }
        }
    }
    
    /**
     * Kiểm tra va chạm người chơi với quái
     */
    private void checkPlayerMonsterCollision() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDamageTime < damageCooldown) return;
        
        int damage = monsterSpawner.checkPlayerCollision(gameScene.getPlayer());
        if (damage > 0) {
            playerHealth -= damage;
            lastDamageTime = currentTime;
            
            if (playerHealth <= 0) {
                playerHealth = 0;
                handlePlayerDeath();
            }
        }
    }
    
    /**
     * Xử lý khi người chơi chết
     */
    private void handlePlayerDeath() {
        // Respawn với một số gold bị mất
        int goldLost = localPlayerScore / 4; // Mất 25% gold
        localPlayerScore -= goldLost;
        if (localPlayerScore < 0) localPlayerScore = 0;
        
        playerHealth = maxPlayerHealth;
        
        // Teleport player to spawn point
        gameScene.getPlayer().setWorldX(1000);
        gameScene.getPlayer().setWorldY(1000);
        
        // Send respawn to server
        Client.getGameClient().sendToServer(
            new Protocol().respawnPacket(gameScene.getPlayerMP().getUsername())
        );
    }
    
    /**
     * Thêm điểm cho người chơi
     */
    public void addScore(int gold) {
        localPlayerScore += gold;
    }
    
    /**
     * Cập nhật điểm của người chơi khác
     */
    public void updatePlayerScore(String username, int score) {
        playerScores.put(username, score);
    }

    @Override
    public void draw(Graphics2D g2d, int tileSize) {
        super.draw(g2d, tileSize);
        
        // Draw monsters
        drawMonsters(g2d, tileSize);
        
        // Draw UI overlay
        drawUI(g2d);
    }
    
    /**
     * Vẽ tất cả quái vật
     */
    private void drawMonsters(Graphics2D g2d, int tileSize) {
        Player player = gameScene.getPlayer();
        int playerWorldX = player.getWorldX();
        int playerWorldY = player.getWorldY();
        int playerScreenX = player.getScreenX();
        int playerScreenY = player.getScreenY();
        
        for (Monster monster : monsterSpawner.getMonsters()) {
            if (!monster.isAlive()) continue;
            
            int worldX = monster.getWorldX();
            int worldY = monster.getWorldY();
            
            // Culling - chỉ vẽ monster trong màn hình
            if (Math.abs(worldX - playerWorldX) > playerScreenX + tileSize * 2 ||
                Math.abs(worldY - playerWorldY) > playerScreenY + tileSize * 2) {
                continue;
            }
            
            int screenX = worldX - playerWorldX + playerScreenX;
            int screenY = worldY - playerWorldY + playerScreenY;
            
            monster.render(g2d, screenX, screenY, tileSize);
        }
    }
    
    /**
     * Vẽ UI overlay (điểm số, timer, health)
     */
    private void drawUI(Graphics2D g2d) {
        // Save original state
        Color originalColor = g2d.getColor();
        Font originalFont = g2d.getFont();
        
        int screenWidth = gameScene.getScreenWidth();
        
        // Draw semi-transparent background for UI
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 200, 120, 10, 10);
        
        // Timer
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(remainingTime <= 30 ? Color.RED : Color.WHITE);
        String timeStr = String.format("Time: %02d:%02d", remainingTime / 60, remainingTime % 60);
        g2d.drawString(timeStr, 20, 40);
        
        // Wave info
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.YELLOW);
        String waveStr = "Wave: " + monsterSpawner.getWaveNumber();
        g2d.drawString(waveStr, 20, 65);
        
        // Score/Gold
        g2d.setColor(Color.ORANGE);
        String scoreStr = "Gold: " + localPlayerScore;
        g2d.drawString(scoreStr, 20, 90);
        
        // Monsters alive
        g2d.setColor(Color.CYAN);
        String monsterStr = "Monsters: " + monsterSpawner.getMonstersAlive();
        g2d.drawString(monsterStr, 20, 115);
        
        // Health bar
        drawHealthBar(g2d, screenWidth);
        
        // Leaderboard (top right)
        drawLeaderboard(g2d, screenWidth);
        
        // Game over/waiting screen
        if (gameEnded) {
            drawGameOver(g2d, screenWidth);
        } else if (!gameStarted) {
            drawWaitingScreen(g2d, screenWidth);
        }
        
        // Restore original state
        g2d.setColor(originalColor);
        g2d.setFont(originalFont);
    }
    
    /**
     * Vẽ thanh máu người chơi
     */
    private void drawHealthBar(Graphics2D g2d, int screenWidth) {
        int barWidth = 200;
        int barHeight = 20;
        int barX = (screenWidth - barWidth) / 2;
        int barY = 20;
        
        // Background
        g2d.setColor(new Color(50, 50, 50, 200));
        g2d.fillRoundRect(barX - 5, barY - 5, barWidth + 10, barHeight + 10, 10, 10);
        
        // Health background (red)
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(barX, barY, barWidth, barHeight);
        
        // Current health (green to red based on health)
        float healthPercent = (float) playerHealth / maxPlayerHealth;
        Color healthColor = new Color(
            (int) (255 * (1 - healthPercent)),
            (int) (255 * healthPercent),
            0
        );
        g2d.setColor(healthColor);
        g2d.fillRect(barX, barY, (int) (barWidth * healthPercent), barHeight);
        
        // Border
        g2d.setColor(Color.WHITE);
        g2d.drawRect(barX, barY, barWidth, barHeight);
        
        // Health text
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String healthText = playerHealth + "/" + maxPlayerHealth;
        int textWidth = g2d.getFontMetrics().stringWidth(healthText);
        g2d.drawString(healthText, barX + (barWidth - textWidth) / 2, barY + 15);
    }
    
    /**
     * Vẽ bảng xếp hạng
     */
    private void drawLeaderboard(Graphics2D g2d, int screenWidth) {
        int lbWidth = 180;
        int lbHeight = 30 + playerScores.size() * 25 + 30; // +30 for local player
        int lbX = screenWidth - lbWidth - 10;
        int lbY = 10;
        
        // Background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(lbX, lbY, lbWidth, lbHeight, 10, 10);
        
        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("Leaderboard", lbX + 10, lbY + 22);
        
        // Local player
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(Color.GREEN);
        String localStr = gameScene.getPlayerMP().getUsername() + ": " + localPlayerScore;
        g2d.drawString("> " + localStr, lbX + 10, lbY + 47);
        
        // Other players
        g2d.setColor(Color.WHITE);
        int yOffset = 72;
        for (Entry<String, Integer> entry : playerScores.entrySet()) {
            String playerStr = entry.getKey() + ": " + entry.getValue();
            g2d.drawString("  " + playerStr, lbX + 10, lbY + yOffset);
            yOffset += 25;
        }
    }
    
    /**
     * Vẽ màn hình game over
     */
    private void drawGameOver(Graphics2D g2d, int screenWidth) {
        int screenHeight = gameScene.getScreenHeight();
        
        // Overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, screenWidth, screenHeight);
        
        // Game Over text
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.setColor(Color.RED);
        String gameOverText = "TIME'S UP!";
        int textWidth = g2d.getFontMetrics().stringWidth(gameOverText);
        g2d.drawString(gameOverText, (screenWidth - textWidth) / 2, screenHeight / 2 - 50);
        
        // Final score
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.setColor(Color.YELLOW);
        String scoreText = "Your Score: " + localPlayerScore + " Gold";
        textWidth = g2d.getFontMetrics().stringWidth(scoreText);
        g2d.drawString(scoreText, (screenWidth - textWidth) / 2, screenHeight / 2 + 10);
        
        // Instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.setColor(Color.WHITE);
        String instrText = "Press SPACE to play again or ESC to exit";
        textWidth = g2d.getFontMetrics().stringWidth(instrText);
        g2d.drawString(instrText, (screenWidth - textWidth) / 2, screenHeight / 2 + 60);
    }
    
    /**
     * Vẽ màn hình chờ
     */
    private void drawWaitingScreen(Graphics2D g2d, int screenWidth) {
        int screenHeight = gameScene.getScreenHeight();
        
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, screenWidth, screenHeight);
        
        // Instructions
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.setColor(Color.WHITE);
        String text = "Press SPACE to Start!";
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, (screenWidth - textWidth) / 2, screenHeight / 2);
        
        // Game description
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.setColor(Color.CYAN);
        String desc1 = "Defeat monsters to earn gold!";
        String desc2 = "Survive and compete for the highest score!";
        String desc3 = "Time limit: 3 minutes";
        
        textWidth = g2d.getFontMetrics().stringWidth(desc1);
        g2d.drawString(desc1, (screenWidth - textWidth) / 2, screenHeight / 2 + 40);
        textWidth = g2d.getFontMetrics().stringWidth(desc2);
        g2d.drawString(desc2, (screenWidth - textWidth) / 2, screenHeight / 2 + 65);
        textWidth = g2d.getFontMetrics().stringWidth(desc3);
        g2d.drawString(desc3, (screenWidth - textWidth) / 2, screenHeight / 2 + 90);
    }

    @Override
    protected void renderNPC(Graphics2D g2d) {
        gameScene.getLobbyMap().getPvpNPC().checkDraw(gameScene.getPlayer(), g2d);
    }

    @Override
    public void setTileType(int i) {
        if(i == 13) {
            tiles[i].setType(TileType.Wall);
        }
    }
    
    // Getters và Setters
    public MonsterSpawner getMonsterSpawner() {
        return monsterSpawner;
    }
    
    public int getLocalPlayerScore() {
        return localPlayerScore;
    }
    
    public void setLocalPlayerScore(int score) {
        this.localPlayerScore = score;
    }
    
    public int getRemainingTime() {
        return remainingTime;
    }
    
    public void setRemainingTime(int time) {
        this.remainingTime = time;
    }
    
    public boolean isGameStarted() {
        return gameStarted;
    }
    
    public void setGameStarted(boolean started) {
        this.gameStarted = started;
        if (started) {
            lastTimeUpdate = System.currentTimeMillis();
        }
    }
    
    public boolean isGameEnded() {
        return gameEnded;
    }
    
    public void setGameEnded(boolean ended) {
        this.gameEnded = ended;
    }
    
    public int getPlayerHealth() {
        return playerHealth;
    }
    
    public void setPlayerHealth(int health) {
        this.playerHealth = health;
    }
    
    public int getMaxPlayerHealth() {
        return maxPlayerHealth;
    }
    
    public HashMap<String, Integer> getPlayerScores() {
        return playerScores;
    }
    
    public int getGameTimeLimit() {
        return gameTimeLimit;
    }
    
    public void setGameTimeLimit(int timeLimit) {
        this.gameTimeLimit = timeLimit;
    }
}
