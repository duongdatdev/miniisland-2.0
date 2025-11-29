package maps;

import main.GameScene;
import network.client.Client;
import network.client.Protocol;
import objects.MazeCoin;
import objects.MazeCoin.CoinType;
import objects.entities.MazeEnemySpawner;
import objects.entities.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

/**
 * MazeMap represents the maze game mode with randomly generated maze,
 * AI enemies using pathfinding (A* and BFS), and traps.
 */
public class MazeMap extends Map {
    // Enemy spawner for maze mode
    private MazeEnemySpawner enemySpawner;
    private boolean enemiesEnabled = true;
    
    // Game state
    private boolean isGameOver = false;
    private boolean isGameWon = false;
    private int gameOverTimer = 0;
    private static final int GAME_OVER_DELAY = 180; // 3 seconds before restart option
    private boolean scoreSent = false; // Đánh dấu đã gửi điểm chưa
    
    // === NEW: Timer System ===
    private int mazeTimeLimit = 120; // 2 minutes in seconds
    private int remainingTime = mazeTimeLimit;
    private long lastTimeUpdate = 0;
    private boolean timerStarted = false;
    
    // === NEW: Score System ===
    private int totalScore = 0;
    private int coinsCollected = 0;
    private int trapHits = 0;
    private int bonusPoints = 0;
    
    // === NEW: Coin System ===
    private ArrayList<MazeCoin> coins;
    private Random random;
    
    // === NEW: Difficulty Level ===
    public enum Difficulty { EASY, MEDIUM, HARD }
    private Difficulty currentDifficulty = Difficulty.MEDIUM;
    
    public MazeMap(GameScene gameScene) {
        super(gameScene);
        loadMap("/Maps/Maze/mazeTile.png");
        
        // Initialize enemy spawner
        enemySpawner = new MazeEnemySpawner(gameScene);
        
        // Initialize coin system
        coins = new ArrayList<>();
        random = new Random();
    }
    
    /**
     * Start maze mode with enemies and traps
     */
    public void startMazeMode() {
        isGameOver = false;
        isGameWon = false;
        gameOverTimer = 0;
        scoreSent = false; // Reset cờ gửi điểm
        
        // Reset score
        totalScore = 0;
        coinsCollected = 0;
        trapHits = 0;
        bonusPoints = 0;
        
        // Reset timer
        remainingTime = mazeTimeLimit;
        timerStarted = true;
        lastTimeUpdate = System.currentTimeMillis();
        
        // Reset player health
        gameScene.getPlayer().resetForMaze();
        
        if (enemiesEnabled && enemySpawner != null) {
            enemySpawner.start();
        }
        
        // Spawn coins throughout the maze
        spawnCoins();
    }
    
    /**
     * Stop maze mode
     */
    public void stopMazeMode() {
        if (enemySpawner != null) {
            enemySpawner.stop();
        }
        timerStarted = false;
        coins.clear();
    }
    
    /**
     * Spawn coins throughout the maze
     */
    private void spawnCoins() {
        coins.clear();
        
        int coinCount = 15 + (currentDifficulty == Difficulty.HARD ? 5 : 0);
        int tileSize = gameScene.getTileSize();
        
        for (int i = 0; i < coinCount; i++) {
            int[] pos = findValidCoinPosition();
            if (pos != null) {
                CoinType type = selectCoinType();
                coins.add(new MazeCoin(pos[0], pos[1], type));
            }
        }
    }
    
    /**
     * Find a valid position for coin placement
     */
    private int[] findValidCoinPosition() {
        int tileSize = gameScene.getTileSize();
        int attempts = 0;
        
        while (attempts < 100) {
            int tileX = random.nextInt(mapTileCol);
            int tileY = random.nextInt(mapTileRow);
            
            // Check if walkable
            if (isWalkableTile(tileX, tileY)) {
                int worldX = tileX * tileSize;
                int worldY = tileY * tileSize;
                
                // Check not too close to other coins
                boolean tooClose = false;
                for (MazeCoin coin : coins) {
                    if (Math.abs(coin.getX() - worldX) < tileSize * 2 && 
                        Math.abs(coin.getY() - worldY) < tileSize * 2) {
                        tooClose = true;
                        break;
                    }
                }
                
                if (!tooClose) {
                    return new int[]{worldX + tileSize / 4, worldY + tileSize / 4};
                }
            }
            attempts++;
        }
        return null;
    }
    
    /**
     * Check if a tile is walkable
     */
    private boolean isWalkableTile(int tileX, int tileY) {
        try {
            if (tileX < 0 || tileY < 0 || tileX >= mapTileCol || tileY >= mapTileRow) {
                return false;
            }
            int tileNum = mapTileNum[tileX][tileY];
            TileType type = tiles[tileNum].getType();
            return type != TileType.Wall && type != TileType.Hole;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Select coin type (weighted random)
     */
    private CoinType selectCoinType() {
        int roll = random.nextInt(100);
        if (roll < 50) return CoinType.BRONZE;     // 50%
        if (roll < 80) return CoinType.SILVER;     // 30%
        if (roll < 95) return CoinType.GOLD;       // 15%
        return CoinType.DIAMOND;                    // 5%
    }
    
    /**
     * Update maze mode - called each frame
     */
    public void update(Player targetPlayer) {
        if (isGameOver || isGameWon) {
            gameOverTimer++;
            return;
        }
        
        // Update timer
        if (timerStarted) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTimeUpdate >= 1000) {
                remainingTime--;
                lastTimeUpdate = currentTime;
                
                if (remainingTime <= 0) {
                    // Time's up - game over
                    isGameOver = true;
                    if (!scoreSent) {
                        sendScoreToServer(false);
                        scoreSent = true;
                    }
                    return;
                }
            }
        }
        
        if (!targetPlayer.isPlayerAlive()) {
            isGameOver = true;
            if (!scoreSent) {
                sendScoreToServer(false);
                scoreSent = true;
            }
            return;
        }
        
        // Update coins
        updateCoins(targetPlayer);
        
        if (enemiesEnabled && enemySpawner != null && enemySpawner.isActive()) {
            enemySpawner.update(targetPlayer);
            
            // Check trap collision with player (hidden surprise traps)
            MazeEnemySpawner.Trap.TrapEffect trapEffect = enemySpawner.checkPlayerTrapCollision(targetPlayer);
            if (trapEffect != null && !targetPlayer.isInvincible()) {
                trapHits++;
                
                // Apply trap effect
                if (trapEffect.damage > 0) {
                    boolean died = targetPlayer.takeDamage(trapEffect.damage);
                    if (died) {
                        isGameOver = true;
                        if (!scoreSent) {
                            sendScoreToServer(false);
                            scoreSent = true;
                        }
                    }
                }
                
                // Apply slow effect if it's a slow trap
                if (trapEffect.speedMultiplier < 1.0f && trapEffect.duration > 0) {
                    targetPlayer.applySlowEffect(trapEffect.speedMultiplier, trapEffect.duration);
                }
            }
        }
    }
    
    /**
     * Update coins and check collection
     */
    private void updateCoins(Player player) {
        for (MazeCoin coin : coins) {
            coin.update();
            
            if (coin.checkCollision(player)) {
                coin.collect();
                coinsCollected++;
                totalScore += coin.getValue();
            }
        }
        
        // Remove collected coins
        coins.removeIf(MazeCoin::isCollected);
    }
    
    /**
     * Handle winning the maze
     */
    public void handleWin() {
        isGameWon = true;
        timerStarted = false;
        
        // Calculate bonus points
        bonusPoints = 0;
        
        // Time bonus: +5 points per second remaining
        bonusPoints += remainingTime * 5;
        
        // Health bonus: +2 points per HP remaining
        bonusPoints += gameScene.getPlayer().getHealth() * 2;
        
        // No trap hits bonus
        if (trapHits == 0) {
            bonusPoints += 200; // Perfect run bonus
        }
        
        // Collect all coins bonus
        if (coins.isEmpty() && coinsCollected > 0) {
            bonusPoints += 100;
        }
        
        // Difficulty multiplier
        float difficultyMultiplier = 1.0f;
        switch (currentDifficulty) {
            case EASY: difficultyMultiplier = 0.5f; break;
            case MEDIUM: difficultyMultiplier = 1.0f; break;
            case HARD: difficultyMultiplier = 1.5f; break;
        }
        
        totalScore = (int) ((totalScore + bonusPoints) * difficultyMultiplier);
        
        // Gửi điểm lên server để cập nhật leaderboard
        if (!scoreSent) {
            sendScoreToServer(true);
            scoreSent = true;
        }
    }
    
    /**
     * Gửi điểm lên server
     */
    private void sendScoreToServer(boolean won) {
        String username = gameScene.getPlayerMP().getUsername();
        Client.getGameClient().sendToServer(
            new Protocol().mazeEndPacket(username, totalScore, coinsCollected, won)
        );
    }

    public void readMap(String map, Runnable process) {
        int row = 0;
        int col = 0;

        char wall = '#';
        char space = ' ';
        char star = '*';
        char hole = 'O';
        char finishLine = '-';

        try {
            String[] lines = map.split("/");
            mapTileCol = lines[0].length();
            mapTileRow = lines.length;
            System.out.println(mapTileCol + " " + mapTileRow);
            while (col < mapTileCol && row < mapTileRow) {
                while (col < mapTileCol) {
                    char[] charArray = lines[row].toCharArray();
                    if (charArray[col] == wall) {
                        mapTileNum[col][row] = 0;
                        System.out.print(mapTileNum[col][row] + " ");
                        col++;
                    } else if (charArray[col] == space) {
                        mapTileNum[col][row] = 3;
                        System.out.print(mapTileNum[col][row] + " ");
                        col++;
                    } else if (charArray[col] == star) {
                        mapTileNum[col][row] = 3;
                        System.out.print(mapTileNum[col][row] + " ");
                        col++;
                    } else if (charArray[col] == hole) {
                        mapTileNum[col][row] = 4;
                        System.out.print(mapTileNum[col][row] + " ");
                        col++;
                    } else if (charArray[col] == finishLine) {
                        mapTileNum[col][row] = 2;
                        System.out.print(mapTileNum[col][row] + " ");
                        col++;
                    }

                }
                if (col == mapTileCol) {
                    System.out.println("hhh");
                    col = 0;
                    row++;
                }
            }
            process.run();
        } catch (Exception e)

        {
            e.printStackTrace();
        }
    }

    @Override
    protected void renderNPC(Graphics2D g2d) {
        gameScene.getLobbyMap().getMazeNPC().checkDraw(gameScene.getPlayer(), g2d);
    }

    @Override
    public void setTileType(int i) {
        if (i == 4) {
            tiles[i].setType(TileType.Hole);
        } else if (i == 0) {
            tiles[i].setType(TileType.Wall);
        } else if (i == 2) {
            tiles[i].setType(TileType.FinishLine);
        }
    }

    public void clear() {
        for (int i = 0; i < mapTileCol; i++) {
            for (int j = 0; j < mapTileRow; j++) {
                mapTileNum[i][j] = 0;
            }
        }
        
        // Stop enemy spawner when clearing maze
        if (enemySpawner != null) {
            enemySpawner.stop();
        }
    }
    
    /**
     * Override draw to include enemies, traps, health bar, and game over screen
     */
    @Override
    public void draw(Graphics2D g2d, int tileSize) {
        // Draw base map
        super.draw(g2d, tileSize);
        
        int playerWorldX = gameScene.getPlayer().getWorldX();
        int playerWorldY = gameScene.getPlayer().getWorldY();
        int playerScreenX = gameScene.getPlayer().getScreenX();
        int playerScreenY = gameScene.getPlayer().getScreenY();
        
        // Draw coins
        for (MazeCoin coin : coins) {
            int screenX = coin.getX() - playerWorldX + playerScreenX;
            int screenY = coin.getY() - playerWorldY + playerScreenY;
            
            // Culling
            if (Math.abs(coin.getX() - playerWorldX) < playerScreenX + tileSize * 2 &&
                Math.abs(coin.getY() - playerWorldY) < playerScreenY + tileSize * 2) {
                coin.render(g2d, screenX, screenY, tileSize);
            }
        }
        
        // Draw enemies and traps
        if (enemiesEnabled && enemySpawner != null && enemySpawner.isActive()) {
            enemySpawner.render(g2d, playerWorldX, playerWorldY, 
                               playerScreenX, playerScreenY, tileSize);
        }
        
        // Draw UI overlay
        drawUI(g2d);
        
        // Draw game over screen if player died
        if (isGameOver) {
            renderGameOverScreen(g2d);
        }
        
        // Draw win screen
        if (isGameWon) {
            renderWinScreen(g2d);
        }
    }
    
    /**
     * Draw UI overlay (timer, score, health)
     */
    private void drawUI(Graphics2D g2d) {
        int screenWidth = gameScene.getScreenWidth();
        int screenHeight = gameScene.getScreenHeight();
        
        // === TOP LEFT: Health Bar ===
        Player player = gameScene.getPlayer();
        int healthBarX = 10;
        int healthBarY = 10;
        int healthBarW = 180;
        int healthBarH = 25;
        
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(healthBarX, healthBarY, healthBarW + 10, healthBarH + 10, 10, 10);
        
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRoundRect(healthBarX + 5, healthBarY + 5, healthBarW, healthBarH, 8, 8);
        
        float hpPercent = (float) player.getHealth() / player.getMaxHealth();
        Color hpColor = new Color((int)(255 * (1 - hpPercent)), (int)(255 * hpPercent), 0);
        g2d.setColor(hpColor);
        g2d.fillRoundRect(healthBarX + 5, healthBarY + 5, (int)(healthBarW * hpPercent), healthBarH, 8, 8);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        String hpText = "❤ " + player.getHealth() + "/" + player.getMaxHealth();
        g2d.drawString(hpText, healthBarX + 55, healthBarY + 21);
        
        // === TOP CENTER: Timer ===
        int timerW = 120;
        int timerX = (screenWidth - timerW) / 2;
        
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(timerX, 10, timerW, 40, 10, 10);
        g2d.setColor(remainingTime <= 30 ? new Color(150, 50, 50) : new Color(60, 60, 60));
        g2d.drawRoundRect(timerX, 10, timerW, 40, 10, 10);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(remainingTime <= 30 ? Color.RED : Color.WHITE);
        String timeStr = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(timeStr, timerX + (timerW - fm.stringWidth(timeStr)) / 2, 38);
        
        // === TOP RIGHT: Score Panel ===
        int scoreW = 130;
        int scoreX = screenWidth - scoreW - 10;
        
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(scoreX, 10, scoreW, 75, 10, 10);
        g2d.setColor(new Color(60, 60, 60));
        g2d.drawRoundRect(scoreX, 10, scoreW, 75, 10, 10);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("⭐ " + totalScore, scoreX + 10, 30);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(Color.ORANGE);
        g2d.drawString("🪙 Coins: " + coinsCollected, scoreX + 10, 50);
        
        g2d.setColor(new Color(255, 100, 100));
        g2d.drawString("💥 Traps: " + trapHits, scoreX + 10, 70);
        
        // === BOTTOM LEFT: Controls Hint (avoid teleport button in center) ===
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.setColor(new Color(200, 200, 200, 180));
        String hint = "WASD: Move | Collect coins, avoid traps!";
        g2d.drawString(hint, 15, screenHeight - 120);
    }
    
    /**
     * Render game over screen
     */
    private void renderGameOverScreen(Graphics2D g2d) {
        int screenWidth = gameScene.getScreenWidth();
        int screenHeight = gameScene.getScreenHeight();
        
        // Dark overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, screenWidth, screenHeight);
        
        // Game Over text
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        g2d.setColor(Color.RED);
        String gameOverText = remainingTime <= 0 ? "TIME'S UP!" : "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (screenWidth - fm.stringWidth(gameOverText)) / 2;
        int textY = screenHeight / 2 - 80;
        g2d.drawString(gameOverText, textX, textY);
        
        // Stats
        g2d.setFont(new Font("Arial", Font.PLAIN, 22));
        g2d.setColor(Color.WHITE);
        
        String scoreText = "Final Score: " + totalScore;
        fm = g2d.getFontMetrics();
        textX = (screenWidth - fm.stringWidth(scoreText)) / 2;
        g2d.drawString(scoreText, textX, textY + 50);
        
        String coinsText = "Coins Collected: " + coinsCollected;
        textX = (screenWidth - fm.stringWidth(coinsText)) / 2;
        g2d.drawString(coinsText, textX, textY + 80);
        
        String trapText = "Traps Hit: " + trapHits;
        textX = (screenWidth - fm.stringWidth(trapText)) / 2;
        g2d.drawString(trapText, textX, textY + 110);
        
        // Restart instruction
        if (gameOverTimer >= GAME_OVER_DELAY) {
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.setColor(Color.YELLOW);
            String restartText = "Press SPACE to return to Lobby";
            fm = g2d.getFontMetrics();
            textX = (screenWidth - fm.stringWidth(restartText)) / 2;
            g2d.drawString(restartText, textX, textY + 160);
        }
    }
    
    /**
     * Render win screen
     */
    private void renderWinScreen(Graphics2D g2d) {
        int screenWidth = gameScene.getScreenWidth();
        int screenHeight = gameScene.getScreenHeight();
        
        // Dark overlay with golden tint
        g2d.setColor(new Color(20, 20, 0, 180));
        g2d.fillRect(0, 0, screenWidth, screenHeight);
        
        // Victory text
        g2d.setFont(new Font("Arial", Font.BOLD, 60));
        g2d.setColor(new Color(255, 215, 0)); // Gold
        String winText = "🏆 MAZE COMPLETED! 🏆";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (screenWidth - fm.stringWidth(winText)) / 2;
        int textY = screenHeight / 2 - 100;
        g2d.drawString(winText, textX, textY);
        
        // Score breakdown
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(Color.WHITE);
        
        String[] scoreLines = {
            "Coins Collected: " + coinsCollected + " (+" + (totalScore - bonusPoints) + " pts)",
            "Time Bonus: +" + (remainingTime * 5) + " pts (" + remainingTime + "s left)",
            "Health Bonus: +" + (gameScene.getPlayer().getHealth() * 2) + " pts",
            trapHits == 0 ? "Perfect Run Bonus: +200 pts!" : "",
            "─────────────────────",
            "TOTAL SCORE: " + totalScore
        };
        
        int y = textY + 60;
        for (String line : scoreLines) {
            if (!line.isEmpty()) {
                fm = g2d.getFontMetrics();
                textX = (screenWidth - fm.stringWidth(line)) / 2;
                
                if (line.startsWith("TOTAL")) {
                    g2d.setColor(Color.YELLOW);
                    g2d.setFont(new Font("Arial", Font.BOLD, 26));
                } else if (line.startsWith("Perfect")) {
                    g2d.setColor(new Color(255, 100, 255));
                }
                
                g2d.drawString(line, textX, y);
                y += 30;
                
                // Reset font
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
            }
        }
        
        // Continue instruction
        if (gameOverTimer >= GAME_OVER_DELAY) {
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.setColor(Color.CYAN);
            String continueText = "Press SPACE to continue";
            fm = g2d.getFontMetrics();
            textX = (screenWidth - fm.stringWidth(continueText)) / 2;
            g2d.drawString(continueText, textX, screenHeight - 100);
        }
    }
    
    // Getters and setters
    public MazeEnemySpawner getEnemySpawner() {
        return enemySpawner;
    }
    
    public boolean isEnemiesEnabled() {
        return enemiesEnabled;
    }
    
    public void setEnemiesEnabled(boolean enabled) {
        this.enemiesEnabled = enabled;
    }
    
    public boolean isGameOver() {
        return isGameOver;
    }
    
    public boolean isGameWon() {
        return isGameWon;
    }
    
    public boolean canRestart() {
        return (isGameOver || isGameWon) && gameOverTimer >= GAME_OVER_DELAY;
    }
    
    // === NEW Getters ===
    public int getTotalScore() { return totalScore; }
    public int getCoinsCollected() { return coinsCollected; }
    public int getTrapHits() { return trapHits; }
    public int getRemainingTime() { return remainingTime; }
    public Difficulty getDifficulty() { return currentDifficulty; }
    
    public void setDifficulty(Difficulty difficulty) {
        this.currentDifficulty = difficulty;
        switch (difficulty) {
            case EASY:
                mazeTimeLimit = 180; // 3 minutes
                break;
            case MEDIUM:
                mazeTimeLimit = 120; // 2 minutes
                break;
            case HARD:
                mazeTimeLimit = 90;  // 1.5 minutes
                break;
        }
    }
}
