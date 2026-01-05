package objects.entities;

import main.GameScene;
import maps.MazeMap;
import maps.TileType;
import objects.entities.MazeEnemy.EnemyType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MazeEnemySpawner manages enemy spawning and traps in maze mode.
 * Spawns AI enemies with pathfinding (A* or BFS) that chase the player.
 * Also manages trap placement throughout the maze.
 */
public class MazeEnemySpawner {
    private CopyOnWriteArrayList<MazeEnemy> enemies;
    private CopyOnWriteArrayList<Trap> traps;
    private GameScene gameScene;
    private Random random;
    
    // Spawn settings
    private int maxEnemies = 5;
    private int spawnInterval = 300; // 5 seconds at 60fps
    private int spawnTimer = 0;
    private int enemiesKilled = 0;
    
    // Enemy ID counter
    private int nextEnemyId = 0;
    
    // Game state
    private boolean isActive = false;
    
    // Tile size for position calculations
    private int tileSize;
    
    public MazeEnemySpawner(GameScene gameScene) {
        this.gameScene = gameScene;
        this.enemies = new CopyOnWriteArrayList<>();
        this.traps = new CopyOnWriteArrayList<>();
        this.random = new Random();
        this.tileSize = gameScene.getTileSize();
    }
    
    /**
     * Start the spawner
     */
    public void start() {
        isActive = true;
        enemiesKilled = 0;
        spawnTimer = 0;
        enemies.clear();
        traps.clear();
        nextEnemyId = 0;
        
        // No enemies in maze mode - only traps
        // spawnInitialEnemies();
        
        // Place traps throughout the maze
        placeTraps();
    }
    
    /**
     * Stop spawner and clear all enemies and traps
     */
    public void stop() {
        isActive = false;
        enemies.clear();
        traps.clear();
    }
    
    /**
     * Update spawner - called each frame
     */
    public void update(Player targetPlayer) {
        if (!isActive) return;
        
        // No enemies in maze - only update traps
        // Update all enemies
        // for (MazeEnemy enemy : enemies) {
        //     if (enemy.isAlive()) {
        //         enemy.updateAI(targetPlayer);
        //     }
        // }
        
        // Remove dead enemies
        // enemies.removeIf(e -> !e.isAlive());
        
        // Update traps
        for (Trap trap : traps) {
            trap.update();
        }
        
        // No enemy respawn
        // Spawn timer for reinforcements
        // spawnTimer++;
        // if (spawnTimer >= spawnInterval && enemies.size() < maxEnemies) {
        //     spawnEnemy();
        //     spawnTimer = 0;
        // }
    }
    
    /**
     * Spawn initial enemies at random walkable positions
     */
    private void spawnInitialEnemies() {
        int initialCount = 3;
        
        for (int i = 0; i < initialCount && enemies.size() < maxEnemies; i++) {
            spawnEnemy();
        }
    }
    
    /**
     * Spawn a single enemy at a valid position
     */
    private void spawnEnemy() {
        if (enemies.size() >= maxEnemies) return;
        
        // Find valid spawn position
        int[] spawnPos = findValidSpawnPosition();
        if (spawnPos == null) return;
        
        // Select random enemy type
        EnemyType type = selectEnemyType();
        
        // Create and add enemy
        MazeEnemy enemy = new MazeEnemy(nextEnemyId++, spawnPos[0], spawnPos[1], type, gameScene);
        enemies.add(enemy);
    }
    
    /**
     * Find a valid spawn position away from player
     */
    private int[] findValidSpawnPosition() {
        MazeMap mazeMap = gameScene.getMazeMap();
        if (mazeMap == null) return null;
        
        int mapCols = mazeMap.getMapTileCol();
        int mapRows = mazeMap.getMapTileRow();
        int[][] mapTileNum = mazeMap.getMapTileNum();
        
        int playerTileX = gameScene.getPlayer().getWorldX() / tileSize;
        int playerTileY = gameScene.getPlayer().getWorldY() / tileSize;
        
        // Try to find a valid position
        int attempts = 0;
        while (attempts < 100) {
            int tileX = random.nextInt(mapCols);
            int tileY = random.nextInt(mapRows);
            
            // Check if walkable and far enough from player
            if (isWalkable(tileX, tileY, mazeMap)) {
                int distance = Math.abs(tileX - playerTileX) + Math.abs(tileY - playerTileY);
                if (distance > 5) { // At least 5 tiles away from player
                    return new int[]{tileX * tileSize, tileY * tileSize};
                }
            }
            
            attempts++;
        }
        
        return null;
    }
    
    /**
     * Check if a tile is walkable
     */
    private boolean isWalkable(int tileX, int tileY, MazeMap mazeMap) {
        try {
            int[][] mapTileNum = mazeMap.getMapTileNum();
            if (tileX < 0 || tileY < 0 || 
                tileX >= mazeMap.getMapTileCol() || 
                tileY >= mazeMap.getMapTileRow()) {
                return false;
            }
            
            int tileNum = mapTileNum[tileX][tileY];
            TileType tileType = mazeMap.getTiles()[tileNum].getType();
            
            return tileType != TileType.Wall && tileType != TileType.Hole;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Select enemy type based on game progress
     */
    private EnemyType selectEnemyType() {
        int roll = random.nextInt(100);
        
        if (enemiesKilled >= 10) {
            // After killing 10, spawn stronger enemies
            if (roll < 20) return EnemyType.GHOST;
            if (roll < 50) return EnemyType.ZOMBIE;
            if (roll < 85) return EnemyType.SKELETON;
            return EnemyType.DEMON; // 15% chance for demon
        } else if (enemiesKilled >= 5) {
            // After killing 5
            if (roll < 30) return EnemyType.GHOST;
            if (roll < 70) return EnemyType.ZOMBIE;
            return EnemyType.SKELETON;
        } else {
            // Initial phase
            if (roll < 50) return EnemyType.GHOST;
            return EnemyType.ZOMBIE;
        }
    }
    
    /**
     * Place traps throughout the maze, ensuring a safe path exists
     */
    private void placeTraps() {
        MazeMap mazeMap = gameScene.getMazeMap();
        if (mazeMap == null) return;
        
        int mapCols = mazeMap.getMapTileCol();
        int mapRows = mazeMap.getMapTileRow();
        
        // Find the safe path from start to finish using BFS
        // Start position: player spawn (usually near entrance)
        // End position: finish line (tile type 2)
        HashSet<String> safePath = findSafePath(mazeMap);
        
        // Place traps - mix of different types
        int trapCount = Math.max(10, (mapCols * mapRows) / 40);
        
        for (int i = 0; i < trapCount; i++) {
            int[] pos = findValidTrapPosition(mazeMap, safePath);
            if (pos != null) {
                // Variety of trap types
                Trap.TrapType type = selectTrapType();
                traps.add(new Trap(pos[0], pos[1], tileSize, type));
            }
        }
    }
    
    /**
     * Select trap type with weighted randomness
     */
    private Trap.TrapType selectTrapType() {
        Random rand = new Random();
        int roll = rand.nextInt(100);
        
        if (roll < 50) return Trap.TrapType.SPIKE;        // 50% - standard damage
        if (roll < 75) return Trap.TrapType.SLOW;         // 25% - slows player
        return Trap.TrapType.POISON;                       // 25% - damage over time
    }
    
    /**
     * Find safe path from start to finish using BFS
     * Returns set of tile coordinates (as "x,y" strings) that are on the safe path
     */
    private HashSet<String> findSafePath(MazeMap mazeMap) {
        HashSet<String> safePath = new HashSet<>();
        
        int mapCols = mazeMap.getMapTileCol();
        int mapRows = mazeMap.getMapTileRow();
        int[][] mapTileNum = mazeMap.getMapTileNum();
        
        // Find start position (near player spawn, first walkable tile)
        int startX = -1, startY = -1;
        int endX = -1, endY = -1;
        
        // Start: find first walkable tile in first few rows (entrance area)
        for (int y = 0; y < Math.min(10, mapRows) && startX == -1; y++) {
            for (int x = 0; x < mapCols; x++) {
                if (isWalkable(x, y, mazeMap)) {
                    startX = x;
                    startY = y;
                    break;
                }
            }
        }
        
        // End: find finish line tile (type 2) or last walkable tile
        for (int y = mapRows - 1; y >= Math.max(0, mapRows - 10) && endX == -1; y--) {
            for (int x = 0; x < mapCols; x++) {
                try {
                    int tileNum = mapTileNum[x][y];
                    TileType type = mazeMap.getTiles()[tileNum].getType();
                    if (type == TileType.FinishLine || (endX == -1 && isWalkable(x, y, mazeMap))) {
                        endX = x;
                        endY = y;
                        if (type == TileType.FinishLine) break;
                    }
                } catch (Exception e) {
                    // Skip invalid tiles
                }
            }
        }
        
        if (startX == -1 || endX == -1) {
            return safePath; // Return empty if no valid start/end
        }
        
        // BFS to find shortest path
        Queue<int[]> queue = new LinkedList<>();
        HashMap<String, String> parent = new HashMap<>();
        HashSet<String> visited = new HashSet<>();
        
        String startKey = startX + "," + startY;
        queue.add(new int[]{startX, startY});
        visited.add(startKey);
        parent.put(startKey, null);
        
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        String endKey = endX + "," + endY;
        boolean found = false;
        
        while (!queue.isEmpty() && !found) {
            int[] current = queue.poll();
            String currentKey = current[0] + "," + current[1];
            
            if (current[0] == endX && current[1] == endY) {
                found = true;
                break;
            }
            
            for (int[] dir : directions) {
                int newX = current[0] + dir[0];
                int newY = current[1] + dir[1];
                String newKey = newX + "," + newY;
                
                if (!visited.contains(newKey) && isWalkable(newX, newY, mazeMap)) {
                    visited.add(newKey);
                    parent.put(newKey, currentKey);
                    queue.add(new int[]{newX, newY});
                }
            }
        }
        
        // Backtrack to build safe path
        if (found) {
            String current = endKey;
            while (current != null) {
                safePath.add(current);
                
                // Also add adjacent tiles to make path wider (safer)
                String[] parts = current.split(",");
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                
                // Add 1-tile buffer around the path
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        safePath.add((x + dx) + "," + (y + dy));
                    }
                }
                
                current = parent.get(current);
            }
        }
        
        return safePath;
    }
    
    /**
     * Find a valid trap position that is NOT on the safe path
     */
    private int[] findValidTrapPosition(MazeMap mazeMap, HashSet<String> safePath) {
        int mapCols = mazeMap.getMapTileCol();
        int mapRows = mazeMap.getMapTileRow();
        
        int playerTileX = gameScene.getPlayer().getWorldX() / tileSize;
        int playerTileY = gameScene.getPlayer().getWorldY() / tileSize;
        
        // Try to find a valid position not on the safe path
        int attempts = 0;
        while (attempts < 200) {
            int tileX = random.nextInt(mapCols);
            int tileY = random.nextInt(mapRows);
            String tileKey = tileX + "," + tileY;
            
            // Check if walkable, not on safe path, and far enough from player
            if (isWalkable(tileX, tileY, mazeMap) && !safePath.contains(tileKey)) {
                int distance = Math.abs(tileX - playerTileX) + Math.abs(tileY - playerTileY);
                if (distance > 3) { // At least 3 tiles away from player
                    // Check if this position doesn't already have a trap
                    boolean hasTrap = false;
                    for (Trap trap : traps) {
                        if (trap.getX() == tileX * tileSize && trap.getY() == tileY * tileSize) {
                            hasTrap = true;
                            break;
                        }
                    }
                    if (!hasTrap) {
                        return new int[]{tileX * tileSize, tileY * tileSize};
                    }
                }
            }
            
            attempts++;
        }
        
        return null;
    }
    
    /**
     * Check bullet collision with all enemies
     * @return damage dealt if hit, 0 otherwise
     */
    public int checkBulletCollision(Bullet bullet) {
        for (MazeEnemy enemy : enemies) {
            if (enemy.checkBulletCollision(bullet)) {
                bullet.setStop(true);
                
                // Enemy takes 25 damage
                if (enemy.takeDamage(25)) {
                    enemiesKilled++;
                    return enemy.getDamage(); // Return damage as score
                }
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * Check player collision with enemies
     * @return damage if attacked, 0 otherwise
     */
    public int checkPlayerEnemyCollision(Player player) {
        for (MazeEnemy enemy : enemies) {
            if (enemy.checkPlayerCollision(player) && enemy.canAttack()) {
                return enemy.getDamage();
            }
        }
        return 0;
    }
    
    /**
     * Check player collision with traps
     * @return trap effect if triggered, null otherwise
     */
    public Trap.TrapEffect checkPlayerTrapCollision(Player player) {
        Rectangle playerRect = new Rectangle(
            player.getWorldX() + player.getHitBox().x,
            player.getWorldY() + player.getHitBox().y,
            player.getHitBox().width, player.getHitBox().height
        );
        
        for (Trap trap : traps) {
            if (trap.isActive() && trap.checkCollision(playerRect)) {
                return trap.trigger();
            }
        }
        return null;
    }
    
    /**
     * Render all enemies and traps
     */
    public void render(Graphics2D g2d, int playerWorldX, int playerWorldY, 
                       int playerScreenX, int playerScreenY, int tileSize) {
        // Render traps first (below enemies)
        for (Trap trap : traps) {
            int screenX = trap.getX() - playerWorldX + playerScreenX;
            int screenY = trap.getY() - playerWorldY + playerScreenY;
            trap.render(g2d, screenX, screenY, tileSize);
        }
        
        // Render enemies
        for (MazeEnemy enemy : enemies) {
            if (enemy.isAlive()) {
                int screenX = enemy.getWorldX() - playerWorldX + playerScreenX;
                int screenY = enemy.getWorldY() - playerWorldY + playerScreenY;
                enemy.render(g2d, screenX, screenY, tileSize);
            }
        }
    }
    
    // Getters
    public java.util.List<MazeEnemy> getEnemies() { return enemies; }
    public java.util.List<Trap> getTraps() { return traps; }
    public boolean isActive() { return isActive; }
    public int getEnemiesKilled() { return enemiesKilled; }
    public int getEnemiesAlive() { return (int) enemies.stream().filter(MazeEnemy::isAlive).count(); }
    
    // Setters
    public void setMaxEnemies(int maxEnemies) { this.maxEnemies = maxEnemies; }
    public void setSpawnInterval(int spawnInterval) { this.spawnInterval = spawnInterval; }
    
    /**
     * Inner class representing traps in the maze
     */
    public static class Trap {
        private int x, y;
        private int size;
        private TrapType type;
        private boolean isActive;
        private boolean isHidden;  // Trap is hidden until triggered (surprise)
        private boolean hasBeenTriggered; // Track if trap was ever triggered
        private int cooldown;
        private int cooldownTimer;
        
        // Animation
        private int animFrame = 0;
        private int animCounter = 0;
        private int revealTimer = 0; // Timer to show trap after triggered
        private static final int REVEAL_DURATION = 60; // Show trap for 1 second after trigger
        
        public enum TrapType {
            SPIKE(30, 180, false, new Color(200, 50, 50)),    // 30% damage
            SLOW(10, 120, false, new Color(100, 100, 255)),    // 10% damage + slow
            POISON(15, 90, false, new Color(100, 200, 50));    // 15% damage, fast reset
            
            public final int damage;
            public final int cooldown;
            public final boolean isDeadly;
            public final Color color;
            
            TrapType(int damage, int cooldown, boolean isDeadly, Color color) {
                this.damage = damage;
                this.cooldown = cooldown;
                this.isDeadly = isDeadly;
                this.color = color;
            }
        }
        
        public static class TrapEffect {
            public int damage;
            public float speedMultiplier;
            public int duration;
            public boolean isDeadly;
            public TrapType type;
            
            public TrapEffect(int damage, float speedMultiplier, int duration, boolean isDeadly, TrapType type) {
                this.damage = damage;
                this.speedMultiplier = speedMultiplier;
                this.duration = duration;
                this.isDeadly = isDeadly;
                this.type = type;
            }
        }
        
        public Trap(int x, int y, int size, TrapType type) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.type = type;
            this.isActive = true;
            this.isHidden = true;  // Hidden by default - surprise trap
            this.hasBeenTriggered = false;
            this.cooldown = type.cooldown;
            this.cooldownTimer = 0;
        }
        
        public void update() {
            // Animation
            animCounter++;
            if (animCounter >= 10) {
                animFrame = (animFrame + 1) % 4;
                animCounter = 0;
            }
            
            // Reveal timer - hide trap again after reveal duration
            if (revealTimer > 0) {
                revealTimer--;
                if (revealTimer <= 0) {
                    isHidden = true;  // Hide again after reveal
                }
            }
            
            // Cooldown
            if (!isActive) {
                cooldownTimer++;
                if (cooldownTimer >= cooldown) {
                    isActive = true;
                    cooldownTimer = 0;
                }
            }
        }
        
        public boolean checkCollision(Rectangle playerRect) {
            Rectangle trapRect = new Rectangle(x + 8, y + 8, size - 16, size - 16);
            return trapRect.intersects(playerRect);
        }
        
        public TrapEffect trigger() {
            if (!isActive) return null;
            
            isActive = false;
            isHidden = false;  // Reveal trap when triggered
            hasBeenTriggered = true;
            revealTimer = REVEAL_DURATION;
            
            // Different effects based on trap type
            switch (type) {
                case SPIKE:
                    return new TrapEffect(type.damage, 1.0f, 0, false, type);
                case SLOW:
                    return new TrapEffect(type.damage, 0.5f, 180, false, type); // 50% speed for 3 seconds
                case POISON:
                    return new TrapEffect(type.damage, 1.0f, 0, false, type);
                default:
                    return new TrapEffect(type.damage, 1.0f, 0, false, type);
            }
        }
        
        public void render(Graphics2D g2d, int screenX, int screenY, int tileSize) {
            // Hidden traps are invisible - surprise!
            if (isHidden) {
                return;
            }
            
            if (!isActive) {
                // Draw inactive trap (dimmed)
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            }
            
            // Draw trap based on type
            switch (type) {
                case SPIKE:
                    drawSpikeTrap(g2d, screenX, screenY, tileSize);
                    break;
                case SLOW:
                    drawSlowTrap(g2d, screenX, screenY, tileSize);
                    break;
                case POISON:
                    drawPoisonTrap(g2d, screenX, screenY, tileSize);
                    break;
            }
            
            // Reset composite
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        
        private void drawSpikeTrap(Graphics2D g2d, int screenX, int screenY, int tileSize) {
            // Red danger zone
            g2d.setColor(new Color(200, 50, 50, 180));
            g2d.fillRect(screenX + 2, screenY + 2, tileSize - 4, tileSize - 4);
            
            // Draw spikes (animated height)
            g2d.setColor(new Color(80, 80, 80));
            int spikeHeight = 12 + animFrame * 3;
            for (int i = 0; i < 4; i++) {
                int spikeX = screenX + 6 + i * 10;
                int[] xPoints = {spikeX, spikeX + 5, spikeX + 10};
                int[] yPoints = {screenY + tileSize - 6, screenY + tileSize - 6 - spikeHeight, screenY + tileSize - 6};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }
            
            // Warning symbol
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("!", screenX + tileSize/2 - 3, screenY + 16);
        }
        
        private void drawSlowTrap(Graphics2D g2d, int screenX, int screenY, int tileSize) {
            // Blue ice zone
            g2d.setColor(new Color(100, 150, 255, 180));
            g2d.fillRect(screenX + 2, screenY + 2, tileSize - 4, tileSize - 4);
            
            // Ice crystals pattern
            g2d.setColor(new Color(200, 230, 255));
            int offset = animFrame * 2;
            for (int i = 0; i < 3; i++) {
                int cx = screenX + 10 + i * 14;
                int cy = screenY + 12 + (i % 2) * 10 + offset % 8;
                g2d.fillPolygon(
                    new int[]{cx, cx + 6, cx + 3},
                    new int[]{cy + 8, cy + 8, cy},
                    3
                );
            }
            
            // Snowflake symbol
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("*", screenX + tileSize/2 - 6, screenY + tileSize - 8);
        }
        
        private void drawPoisonTrap(Graphics2D g2d, int screenX, int screenY, int tileSize) {
            // Green poison zone
            g2d.setColor(new Color(80, 180, 50, 180));
            g2d.fillRect(screenX + 2, screenY + 2, tileSize - 4, tileSize - 4);
            
            // Bubbles animation
            g2d.setColor(new Color(150, 255, 100, 200));
            int bubbleOffset = animFrame * 3;
            for (int i = 0; i < 4; i++) {
                int bx = screenX + 8 + (i * 10);
                int by = screenY + tileSize - 15 - (bubbleOffset + i * 5) % 20;
                int bsize = 4 + (i % 2) * 2;
                g2d.fillOval(bx, by, bsize, bsize);
            }
            
            // Skull symbol
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("☠", screenX + tileSize/2 - 6, screenY + 18);
        }
        
        // Getters
        public int getX() { return x; }
        public int getY() { return y; }
        public boolean isActive() { return isActive; }
        public TrapType getType() { return type; }
    }
}
