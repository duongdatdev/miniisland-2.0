package objects.entities;

import main.GameScene;
import objects.entities.Monster.MonsterType;

import java.util.ArrayList;
import java.util.Random;

/**
 * MonsterSpawner manages monster spawning in Score Battle mode.
 * Spawns monsters in waves with increasing difficulty.
 */
public class MonsterSpawner {
    private ArrayList<Monster> monsters;
    private GameScene gameScene;
    private Random random;
    
    // Spawn settings
    private int maxMonsters = 10;
    private int spawnInterval = 180; // 3 seconds at 60fps
    private int spawnTimer = 0;
    private int waveNumber = 1;
    private int monstersPerWave = 5;
    private int monstersKilledInWave = 0;
    
    // Map bounds
    private int mapMinX = 100;
    private int mapMaxX = 2200;
    private int mapMinY = 100;
    private int mapMaxY = 2200;
    
    // Monster ID counter
    private int nextMonsterId = 0;
    
    // Game state
    private boolean isActive = false;
    
    public MonsterSpawner(GameScene gameScene) {
        this.gameScene = gameScene;
        this.monsters = new ArrayList<>();
        this.random = new Random();
    }
    
    /**
     * Start the spawner
     */
    public void start() {
        isActive = true;
        waveNumber = 1;
        monstersKilledInWave = 0;
        spawnTimer = 0;
        monsters.clear();
        nextMonsterId = 0;
        
        // Spawn first wave
        spawnWave();
    }
    
    /**
     * Stop spawner and clear all monsters
     */
    public void stop() {
        isActive = false;
        monsters.clear();
    }
    
    /**
     * Update spawner - called each frame
     */
    public void update(Player targetPlayer) {
        if (!isActive) return;
        
        // Update all monsters
        for (Monster monster : monsters) {
            if (monster.isAlive()) {
                monster.updateAI(targetPlayer);
            }
        }
        
        // Remove dead monsters
        monsters.removeIf(m -> !m.isAlive());
        
        // Spawn timer
        spawnTimer++;
        if (spawnTimer >= spawnInterval && monsters.size() < maxMonsters) {
            spawnMonster();
            spawnTimer = 0;
        }
        
        // Check wave transition
        if (monstersKilledInWave >= monstersPerWave && monsters.isEmpty()) {
            nextWave();
        }
    }
    
    /**
     * Spawn a new monster
     */
    private void spawnMonster() {
        if (monsters.size() >= maxMonsters) return;
        
        // Choose random spawn position (avoid spawning near player)
        int playerX = gameScene.getPlayer().getWorldX();
        int playerY = gameScene.getPlayer().getWorldY();
        
        int spawnX, spawnY;
        int attempts = 0;
        do {
            spawnX = mapMinX + random.nextInt(mapMaxX - mapMinX);
            spawnY = mapMinY + random.nextInt(mapMaxY - mapMinY);
            attempts++;
        } while (Math.sqrt(Math.pow(spawnX - playerX, 2) + Math.pow(spawnY - playerY, 2)) < 200 && attempts < 10);
        
        // Select monster type based on wave
        MonsterType type = selectMonsterType();
        
        Monster monster = new Monster(nextMonsterId++, spawnX, spawnY, type);
        monsters.add(monster);
    }
    
    /**
     * Spawn a wave of monsters
     */
    private void spawnWave() {
        int toSpawn = Math.min(monstersPerWave, maxMonsters - monsters.size());
        
        for (int i = 0; i < toSpawn; i++) {
            spawnMonster();
        }
        
        // Spawn boss every 5 waves
        if (waveNumber % 5 == 0 && monsters.size() < maxMonsters) {
            int playerX = gameScene.getPlayer().getWorldX();
            int playerY = gameScene.getPlayer().getWorldY();
            
            int spawnX = mapMinX + random.nextInt(mapMaxX - mapMinX);
            int spawnY = mapMinY + random.nextInt(mapMaxY - mapMinY);
            
            Monster boss = new Monster(nextMonsterId++, spawnX, spawnY, MonsterType.BOSS);
            monsters.add(boss);
        }
    }
    
    /**
     * Transition to next wave
     */
    private void nextWave() {
        waveNumber++;
        monstersKilledInWave = 0;
        
        // Increase difficulty
        monstersPerWave = Math.min(monstersPerWave + 2, 15);
        spawnInterval = Math.max(spawnInterval - 10, 60); // Spawn faster
        
        spawnWave();
    }
    
    /**
     * Select monster type based on current wave
     */
    private MonsterType selectMonsterType() {
        int roll = random.nextInt(100);
        
        if (waveNumber >= 10) {
            // Wave 10+: nhiều quái mạnh hơn
            if (roll < 20) return MonsterType.SLIME;
            if (roll < 50) return MonsterType.GOBLIN;
            return MonsterType.ORC;
        } else if (waveNumber >= 5) {
            // Wave 5-9: mix các loại
            if (roll < 40) return MonsterType.SLIME;
            if (roll < 80) return MonsterType.GOBLIN;
            return MonsterType.ORC;
        } else {
            // Wave 1-4: chủ yếu slime
            if (roll < 70) return MonsterType.SLIME;
            return MonsterType.GOBLIN;
        }
    }
    
    /**
     * Check bullet collision with all monsters
     * Returns goldReward if monster is killed, 0 otherwise
     */
    public int checkBulletCollision(Bullet bullet, String shooterUsername) {
        for (Monster monster : monsters) {
            if (monster.checkBulletCollision(bullet)) {
                bullet.setStop(true);
                
                // Monster nhận 25 damage (có thể điều chỉnh)
                if (monster.takeDamage(25)) {
                    monstersKilledInWave++;
                    return monster.getGoldReward();
                }
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * Check player collision with all monsters
     * Returns damage if attacked, 0 otherwise
     */
    public int checkPlayerCollision(Player player) {
        for (Monster monster : monsters) {
            if (monster.checkPlayerCollision(player) && monster.canAttack()) {
                return monster.getDamage();
            }
        }
        return 0;
    }
    
    /**
     * Get monster by ID
     */
    public Monster getMonster(int id) {
        for (Monster monster : monsters) {
            if (monster.getId() == id) {
                return monster;
            }
        }
        return null;
    }
    
    /**
     * Add monster from server
     */
    public void addMonster(int id, int x, int y, String typeStr) {
        MonsterType type;
        try {
            type = MonsterType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = MonsterType.SLIME;
        }
        
        Monster monster = new Monster(id, x, y, type);
        monsters.add(monster);
    }
    
    /**
     * Remove monster by ID
     */
    public void removeMonster(int id) {
        monsters.removeIf(m -> m.getId() == id);
    }
    
    /**
     * Update monster position from server
     */
    public void updateMonster(int id, int x, int y, int health) {
        Monster monster = getMonster(id);
        if (monster != null) {
            monster.setWorldX(x);
            monster.setWorldY(y);
            monster.setHealth(health);
        }
    }
    
    // Getters
    public ArrayList<Monster> getMonsters() {
        return monsters;
    }
    
    public int getWaveNumber() {
        return waveNumber;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public int getMonstersAlive() {
        return (int) monsters.stream().filter(Monster::isAlive).count();
    }
    
    public int getMonstersKilledInWave() {
        return monstersKilledInWave;
    }
    
    public int getMonstersPerWave() {
        return monstersPerWave;
    }
    
    // Setters
    public void setMapBounds(int minX, int maxX, int minY, int maxY) {
        this.mapMinX = minX;
        this.mapMaxX = maxX;
        this.mapMinY = minY;
        this.mapMaxY = maxY;
    }
    
    public void setMaxMonsters(int maxMonsters) {
        this.maxMonsters = maxMonsters;
    }
    
    public void setSpawnInterval(int spawnInterval) {
        this.spawnInterval = spawnInterval;
    }
}
