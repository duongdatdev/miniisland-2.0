package objects.entities;

import main.GameScene;
import maps.MazeMap;
import maps.TileType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * MazeEnemy represents AI-controlled enemies in the maze mode.
 * Uses A* and BFS pathfinding algorithms to chase the player through the maze.
 * Enemies are randomly generated and navigate around walls and obstacles.
 */
public class MazeEnemy extends Entity {
    private int id;
    private int health;
    private int maxHealth;
    private int damage;
    private boolean isAlive;
    private EnemyType type;
    
    // Animation
    private int spriteIndex = 0;
    private int animationCounter = 0;
    private static final int ANIMATION_SPEED = 10;
    
    // Static cache for sprites - all enemies of same type share sprites
    private static HashMap<EnemyType, BufferedImage[]> spriteCache = new HashMap<>();
    private static boolean spritesInitialized = false;
    
    // Pathfinding
    private ArrayList<int[]> currentPath;
    private int pathIndex = 0;
    private int pathRecalculateTimer = 0;
    private static final int PATH_RECALCULATE_INTERVAL = 30; // Recalculate path every 0.5 seconds
    private PathfindingMode pathfindingMode;
    
    // Reference to maze map for collision detection
    private GameScene gameScene;
    private int tileSize;
    
    // Movement
    private Random random;
    private int moveTimer;
    
    // Attack
    private int attackCooldown = 60;
    private int attackTimer = 0;
    private int attackRange;
    
    /**
     * Pathfinding mode selection
     */
    public enum PathfindingMode {
        ASTAR,  // A* algorithm - optimal pathfinding
        BFS     // Breadth-First Search - simpler but guaranteed shortest path
    }
    
    /**
     * Enemy types with different stats
     */
    public enum EnemyType {
        GHOST(40, 10, 3, 60, PathfindingMode.ASTAR),      // Fast, uses A*
        ZOMBIE(60, 15, 2, 80, PathfindingMode.BFS),       // Medium, uses BFS
        SKELETON(80, 20, 2, 100, PathfindingMode.ASTAR),  // Strong, uses A*
        DEMON(150, 30, 1, 120, PathfindingMode.BFS);      // Boss, uses BFS
        
        public final int health;
        public final int damage;
        public final int speed;
        public final int attackRange;
        public final PathfindingMode pathfindingMode;
        
        EnemyType(int health, int damage, int speed, int attackRange, PathfindingMode mode) {
            this.health = health;
            this.damage = damage;
            this.speed = speed;
            this.attackRange = attackRange;
            this.pathfindingMode = mode;
        }
    }
    
    public MazeEnemy(int id, int x, int y, EnemyType type, GameScene gameScene) {
        this.id = id;
        this.worldX = x;
        this.worldY = y;
        this.type = type;
        this.maxHealth = type.health;
        this.health = type.health;
        this.damage = type.damage;
        this.speed = type.speed;
        this.attackRange = type.attackRange;
        this.pathfindingMode = type.pathfindingMode;
        this.isAlive = true;
        this.gameScene = gameScene;
        this.tileSize = gameScene.getTileSize();
        
        this.random = new Random();
        this.currentPath = new ArrayList<>();
        this.moveTimer = 0;
        
        // Hitbox for collision
        this.hitBox = new Rectangle(8, 8, 32, 32);
        
        // Initialize sprites
        initSpritesIfNeeded();
    }
    
    /**
     * Initialize sprites for all enemy types (only runs once)
     */
    private static synchronized void initSpritesIfNeeded() {
        if (spritesInitialized) return;
        
        for (EnemyType t : EnemyType.values()) {
            BufferedImage[] sprites = new BufferedImage[4];
            createFallbackSpritesForType(sprites, t);
            spriteCache.put(t, sprites);
        }
        
        spritesInitialized = true;
    }
    
    /**
     * Create fallback sprites for enemy type
     */
    private static void createFallbackSpritesForType(BufferedImage[] sprites, EnemyType type) {
        int size = 48;
        Color color = getColorForType(type);
        
        for (int i = 0; i < 4; i++) {
            sprites[i] = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sprites[i].createGraphics();
            
            // Draw body based on enemy type
            switch (type) {
                case GHOST:
                    // Ghost - oval with wavy bottom
                    g.setColor(color);
                    g.fillOval(4, 4, size - 8, size - 12);
                    g.fillRect(4, size/2, size - 8, size/2 - 4);
                    // Wavy bottom
                    g.setColor(Color.BLACK);
                    for (int j = 0; j < 4; j++) {
                        g.fillArc(4 + j * 10, size - 12, 10, 10, 180, 180);
                    }
                    break;
                    
                case ZOMBIE:
                    // Zombie - rectangle body with arms
                    g.setColor(color);
                    g.fillRect(10, 8, size - 20, size - 16);
                    g.fillRect(4, 16, 8, 20);  // Left arm
                    g.fillRect(size - 12, 16, 8, 20); // Right arm
                    break;
                    
                case SKELETON:
                    // Skeleton - thin lines forming bones
                    g.setColor(color);
                    g.fillRect(20, 4, 8, 12); // Skull
                    g.fillRect(22, 16, 4, 20); // Spine
                    g.fillRect(12, 18, 24, 4); // Ribs
                    g.fillRect(20, 36, 2, 8); // Left leg
                    g.fillRect(26, 36, 2, 8); // Right leg
                    break;
                    
                case DEMON:
                    // Demon - large scary figure with horns
                    g.setColor(color);
                    g.fillOval(8, 8, size - 16, size - 16);
                    // Horns
                    g.fillPolygon(
                        new int[]{10, 4, 14},
                        new int[]{12, 0, 16},
                        3
                    );
                    g.fillPolygon(
                        new int[]{size - 10, size - 4, size - 14},
                        new int[]{12, 0, 16},
                        3
                    );
                    break;
            }
            
            // Draw eyes
            g.setColor(Color.RED);
            g.fillOval(14, 16, 6, 6);
            g.fillOval(28, 16, 6, 6);
            
            // Animate eyes based on frame
            g.setColor(Color.BLACK);
            g.fillOval(15 + (i % 2), 17, 3, 3);
            g.fillOval(29 + (i % 2), 17, 3, 3);
            
            g.dispose();
        }
    }
    
    /**
     * Get color for enemy type
     */
    private static Color getColorForType(EnemyType type) {
        switch (type) {
            case GHOST:
                return new Color(200, 200, 255, 200); // Transparent blue-white
            case ZOMBIE:
                return new Color(100, 150, 80);       // Greenish
            case SKELETON:
                return new Color(240, 240, 240);      // White-gray
            case DEMON:
                return new Color(180, 50, 50);        // Dark red
            default:
                return Color.GRAY;
        }
    }
    
    /**
     * Get sprites for current enemy type
     */
    private BufferedImage[] getSprites() {
        return spriteCache.get(type);
    }
    
    /**
     * Update AI - pathfinding and movement towards player
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
        
        // Recalculate path periodically
        pathRecalculateTimer++;
        if (pathRecalculateTimer >= PATH_RECALCULATE_INTERVAL || currentPath.isEmpty()) {
            recalculatePath(targetPlayer);
            pathRecalculateTimer = 0;
        }
        
        // Follow path
        followPath();
        
        // Check if close enough to attack
        double distance = getDistanceToPlayer(targetPlayer);
        if (distance < attackRange && attackTimer <= 0) {
            attackTimer = attackCooldown;
        }
    }
    
    /**
     * Calculate distance to player
     */
    private double getDistanceToPlayer(Player player) {
        return Math.sqrt(Math.pow(worldX - player.getWorldX(), 2) + 
                        Math.pow(worldY - player.getWorldY(), 2));
    }
    
    /**
     * Recalculate path to player using selected algorithm
     */
    private void recalculatePath(Player targetPlayer) {
        int startTileX = worldX / tileSize;
        int startTileY = worldY / tileSize;
        int endTileX = targetPlayer.getWorldX() / tileSize;
        int endTileY = targetPlayer.getWorldY() / tileSize;
        
        if (pathfindingMode == PathfindingMode.ASTAR) {
            currentPath = findPathAStar(startTileX, startTileY, endTileX, endTileY);
        } else {
            currentPath = findPathBFS(startTileX, startTileY, endTileX, endTileY);
        }
        
        pathIndex = 0;
    }
    
    /**
     * A* pathfinding algorithm
     * Uses heuristic (Manhattan distance) for optimal path finding
     */
    private ArrayList<int[]> findPathAStar(int startX, int startY, int endX, int endY) {
        ArrayList<int[]> path = new ArrayList<>();
        
        // Priority queue ordered by f = g + h
        PriorityQueue<Node> openSet = new PriorityQueue<>(
            Comparator.comparingDouble(n -> n.f)
        );
        
        HashSet<String> closedSet = new HashSet<>();
        HashMap<String, Node> nodeMap = new HashMap<>();
        
        Node startNode = new Node(startX, startY, 0, heuristic(startX, startY, endX, endY), null);
        openSet.add(startNode);
        nodeMap.put(startX + "," + startY, startNode);
        
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}; // Up, Down, Left, Right
        
        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            
            // Found the goal
            if (current.x == endX && current.y == endY) {
                // Reconstruct path
                Node node = current;
                while (node != null) {
                    path.add(0, new int[]{node.x, node.y});
                    node = node.parent;
                }
                return path;
            }
            
            String currentKey = current.x + "," + current.y;
            closedSet.add(currentKey);
            
            // Explore neighbors
            for (int[] dir : directions) {
                int newX = current.x + dir[0];
                int newY = current.y + dir[1];
                String newKey = newX + "," + newY;
                
                // Skip if already visited or blocked
                if (closedSet.contains(newKey) || !isWalkable(newX, newY)) {
                    continue;
                }
                
                double newG = current.g + 1;
                double newH = heuristic(newX, newY, endX, endY);
                double newF = newG + newH;
                
                Node existingNode = nodeMap.get(newKey);
                if (existingNode == null || newG < existingNode.g) {
                    Node newNode = new Node(newX, newY, newG, newF, current);
                    openSet.add(newNode);
                    nodeMap.put(newKey, newNode);
                }
            }
        }
        
        // No path found, return empty
        return path;
    }
    
    /**
     * BFS pathfinding algorithm
     * Guaranteed shortest path (unweighted)
     */
    private ArrayList<int[]> findPathBFS(int startX, int startY, int endX, int endY) {
        ArrayList<int[]> path = new ArrayList<>();
        
        Queue<Node> queue = new LinkedList<>();
        HashSet<String> visited = new HashSet<>();
        
        Node startNode = new Node(startX, startY, 0, 0, null);
        queue.add(startNode);
        visited.add(startX + "," + startY);
        
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}; // Up, Down, Left, Right
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            
            // Found the goal
            if (current.x == endX && current.y == endY) {
                // Reconstruct path
                Node node = current;
                while (node != null) {
                    path.add(0, new int[]{node.x, node.y});
                    node = node.parent;
                }
                return path;
            }
            
            // Explore neighbors
            for (int[] dir : directions) {
                int newX = current.x + dir[0];
                int newY = current.y + dir[1];
                String newKey = newX + "," + newY;
                
                // Skip if already visited or blocked
                if (visited.contains(newKey) || !isWalkable(newX, newY)) {
                    continue;
                }
                
                Node newNode = new Node(newX, newY, 0, 0, current);
                queue.add(newNode);
                visited.add(newKey);
            }
        }
        
        // No path found, return empty
        return path;
    }
    
    /**
     * Heuristic function for A* (Manhattan distance)
     */
    private double heuristic(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }
    
    /**
     * Check if a tile is walkable
     */
    private boolean isWalkable(int tileX, int tileY) {
        try {
            MazeMap mazeMap = gameScene.getMazeMap();
            if (mazeMap == null) return false;
            
            int[][] mapTileNum = mazeMap.getMapTileNum();
            if (tileX < 0 || tileY < 0 || 
                tileX >= mazeMap.getMapTileCol() || 
                tileY >= mazeMap.getMapTileRow()) {
                return false;
            }
            
            int tileNum = mapTileNum[tileX][tileY];
            TileType tileType = mazeMap.getTiles()[tileNum].getType();
            
            // Wall and Hole are not walkable
            return tileType != TileType.Wall && tileType != TileType.Hole;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Follow the calculated path
     */
    private void followPath() {
        if (currentPath.isEmpty() || pathIndex >= currentPath.size()) {
            return;
        }
        
        int[] targetTile = currentPath.get(pathIndex);
        int targetX = targetTile[0] * tileSize + tileSize / 2;
        int targetY = targetTile[1] * tileSize + tileSize / 2;
        
        int dx = targetX - worldX;
        int dy = targetY - worldY;
        
        // Move towards target
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < speed * 2) {
            // Reached current waypoint, move to next
            pathIndex++;
            worldX = targetX;
            worldY = targetY;
        } else {
            // Move towards waypoint
            worldX += (int) (speed * dx / distance);
            worldY += (int) (speed * dy / distance);
            
            // Update direction for animation
            if (Math.abs(dx) > Math.abs(dy)) {
                direction = dx > 0 ? "RIGHT" : "LEFT";
            } else {
                direction = dy > 0 ? "DOWN" : "UP";
            }
        }
    }
    
    /**
     * Inner class for pathfinding nodes
     */
    private static class Node {
        int x, y;
        double g; // Cost from start
        double f; // Total estimated cost (g + h)
        Node parent;
        
        Node(int x, int y, double g, double f, Node parent) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.f = f;
            this.parent = parent;
        }
    }
    
    /**
     * Take damage from bullet
     * @return true if enemy died
     */
    public boolean takeDamage(int damage) {
        if (!isAlive) return false;
        
        health -= damage;
        if (health <= 0) {
            health = 0;
            isAlive = false;
            return true;
        }
        return false;
    }
    
    /**
     * Check collision with player
     */
    public boolean checkPlayerCollision(Player player) {
        if (!isAlive) return false;
        
        Rectangle enemyRect = new Rectangle(worldX + hitBox.x, worldY + hitBox.y, 
                                            hitBox.width, hitBox.height);
        Rectangle playerRect = new Rectangle(player.getWorldX() + player.getHitBox().x,
                                             player.getWorldY() + player.getHitBox().y,
                                             player.getHitBox().width, player.getHitBox().height);
        
        return enemyRect.intersects(playerRect);
    }
    
    /**
     * Check collision with bullet
     */
    public boolean checkBulletCollision(Bullet bullet) {
        if (!isAlive || bullet.isStop()) return false;
        
        Rectangle enemyRect = new Rectangle(worldX + hitBox.x, worldY + hitBox.y,
                                            hitBox.width, hitBox.height);
        Rectangle bulletRect = new Rectangle(bullet.getPosiX(), bullet.getPosiY(), 10, 10);
        
        return enemyRect.intersects(bulletRect);
    }
    
    /**
     * Render enemy on screen
     */
    public void render(Graphics2D g2d, int screenX, int screenY, int tileSize) {
        if (!isAlive) return;
        
        // Draw sprite
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
        
        // Draw boss name
        if (type == EnemyType.DEMON) {
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.setColor(Color.RED);
            String name = "DEMON";
            int nameWidth = g2d.getFontMetrics().stringWidth(name);
            g2d.drawString(name, screenX + (tileSize - nameWidth) / 2, healthBarY - 5);
        }
        
        // Debug: Draw pathfinding mode indicator
        g2d.setFont(new Font("Arial", Font.PLAIN, 8));
        g2d.setColor(pathfindingMode == PathfindingMode.ASTAR ? Color.CYAN : Color.YELLOW);
        g2d.drawString(pathfindingMode.name(), screenX, screenY + tileSize + 10);
    }
    
    @Override
    public void update(float delta) {
        // Called from outside with target player
    }
    
    @Override
    public void render(Graphics graphics) {
        // Use specific render method with parameters
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public int getMaxHealth() { return maxHealth; }
    public int getDamage() { return damage; }
    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean alive) { isAlive = alive; }
    public EnemyType getType() { return type; }
    public int getAttackRange() { return attackRange; }
    public boolean canAttack() { return attackTimer <= 0 && isAlive; }
    public PathfindingMode getPathfindingMode() { return pathfindingMode; }
    
    /**
     * Set pathfinding mode (can be changed at runtime)
     */
    public void setPathfindingMode(PathfindingMode mode) {
        this.pathfindingMode = mode;
        currentPath.clear(); // Force path recalculation
        pathRecalculateTimer = PATH_RECALCULATE_INTERVAL;
    }
}
