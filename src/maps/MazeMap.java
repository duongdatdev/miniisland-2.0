package maps;

import main.GameScene;
import objects.entities.MazeEnemySpawner;
import objects.entities.Player;

import java.awt.*;

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
    
    public MazeMap(GameScene gameScene) {
        super(gameScene);
        loadMap("/Maps/Maze/mazeTile.png");
        
        // Initialize enemy spawner
        enemySpawner = new MazeEnemySpawner(gameScene);
    }
    
    /**
     * Start maze mode with enemies and traps
     */
    public void startMazeMode() {
        isGameOver = false;
        isGameWon = false;
        gameOverTimer = 0;
        
        // Reset player health
        gameScene.getPlayer().resetForMaze();
        
        if (enemiesEnabled && enemySpawner != null) {
            enemySpawner.start();
        }
    }
    
    /**
     * Stop maze mode
     */
    public void stopMazeMode() {
        if (enemySpawner != null) {
            enemySpawner.stop();
        }
    }
    
    /**
     * Update maze mode - called each frame
     */
    public void update(Player targetPlayer) {
        if (isGameOver) {
            gameOverTimer++;
            return;
        }
        
        if (!targetPlayer.isPlayerAlive()) {
            isGameOver = true;
            return;
        }
        
        if (enemiesEnabled && enemySpawner != null && enemySpawner.isActive()) {
            enemySpawner.update(targetPlayer);
            
            // No enemies in maze mode - only traps
            // Check enemy collision with player
            // int enemyDamage = enemySpawner.checkPlayerEnemyCollision(targetPlayer);
            // if (enemyDamage > 0 && !targetPlayer.isInvincible()) {
            //     boolean died = targetPlayer.takeDamage(enemyDamage);
            //     if (died) {
            //         isGameOver = true;
            //     }
            // }
            
            // Check trap collision with player (hidden surprise traps)
            MazeEnemySpawner.Trap.TrapEffect trapEffect = enemySpawner.checkPlayerTrapCollision(targetPlayer);
            if (trapEffect != null && !targetPlayer.isInvincible()) {
                // Trap deals 30% damage
                if (trapEffect.damage > 0) {
                    boolean died = targetPlayer.takeDamage(trapEffect.damage);
                    if (died) {
                        isGameOver = true;
                    }
                }
            }
        }
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
        
        // Draw enemies and traps
        if (enemiesEnabled && enemySpawner != null && enemySpawner.isActive()) {
            enemySpawner.render(g2d, playerWorldX, playerWorldY, 
                               playerScreenX, playerScreenY, tileSize);
        }
        
        // Draw player health bar (top left)
        gameScene.getPlayer().renderHealthBar(g2d, gameScene.getScreenWidth());
        
        // Draw trap warning (top right)
        renderTrapWarning(g2d);
        
        // Draw game over screen if player died
        if (isGameOver) {
            renderGameOverScreen(g2d);
        }
    }
    
    /**
     * Render trap warning indicator
     */
    private void renderTrapWarning(Graphics2D g2d) {
        int screenWidth = gameScene.getScreenWidth();
        
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("⚠ Watch for hidden traps!", screenWidth - 200, 30);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Traps deal 30% damage", screenWidth - 180, 50);
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
        String gameOverText = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (screenWidth - fm.stringWidth(gameOverText)) / 2;
        int textY = screenHeight / 2 - 50;
        g2d.drawString(gameOverText, textX, textY);
        
        // Stats
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        g2d.setColor(Color.WHITE);
        String statsText = "You hit " + (enemySpawner != null ? enemySpawner.getTraps().size() : 0) + " hidden traps";
        fm = g2d.getFontMetrics();
        textX = (screenWidth - fm.stringWidth(statsText)) / 2;
        g2d.drawString(statsText, textX, textY + 60);
        
        // Restart instruction
        if (gameOverTimer >= GAME_OVER_DELAY) {
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.setColor(Color.YELLOW);
            String restartText = "Press SPACE to return to Lobby";
            fm = g2d.getFontMetrics();
            textX = (screenWidth - fm.stringWidth(restartText)) / 2;
            g2d.drawString(restartText, textX, textY + 120);
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
        return isGameOver && gameOverTimer >= GAME_OVER_DELAY;
    }
}
