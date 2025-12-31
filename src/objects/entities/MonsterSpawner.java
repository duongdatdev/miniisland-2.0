package objects.entities;

import main.GameScene;
import objects.entities.Monster.MonsterType;

import java.util.ArrayList;
import java.util.Random;

/**
 * MonsterSpawner manages monster spawning in Monster Hunt (Score Battle) mode.
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
    
    // Difficulty scaling
    private float difficultyMultiplier = 1.0f;
    private static final float DIFFICULTY_INCREMENT_PER_WAVE = 0.1f; // +10% per wave
    
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
     * NOTE: In multiplayer mode, monsters are managed by server.
     * We don't clear the monster list here because server may have already sent monsters.
     */
    public void start() {
        isActive = true;
        waveNumber = 1;
        monstersKilledInWave = 0;
        spawnTimer = 0;
        // Don't clear monsters - server manages spawning/despawning
        // monsters.clear();
        difficultyMultiplier = 1.0f; // Reset difficulty
        
        // Spawn first wave - DISABLED for multiplayer sync
        // spawnWave();
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
            if (monster.isAlive() || monster.isDying()) {
                monster.updateAI(targetPlayer);
            }
        }
        
        // Remove dead monsters (not dying, completely dead)
        monsters.removeIf(m -> !m.isAlive() && !m.isDying());
        
        // Check wave transition - transition when killed enough monsters
        // if (monstersKilledInWave >= monstersPerWave) {
        //     nextWave();
        //     return; // Skip spawning this frame to let new wave initialize
        // }
        
        // Spawn timer - NOW HANDLED BY SERVER
        // spawnTimer++;
        // if (spawnTimer >= spawnInterval && monsters.size() < maxMonsters) {
        //     spawnMonster();
        //     spawnTimer = 0;
        // }
    }

    public void addMonster(int id, int x, int y, String type) {
        MonsterType mType = MonsterType.SLIME;
        if (type.equals("GOBLIN")) mType = MonsterType.GOBLIN;
        if (type.equals("ORC")) mType = MonsterType.ORC;
        if (type.equals("BOSS")) mType = MonsterType.BOSS;
        
        // Use the constructor that takes ID
        Monster monster = new Monster(id, x, y, mType);
        monsters.add(monster);
    }

    public void removeMonster(int id) {
        monsters.removeIf(m -> m.getId() == id);
    }
    
    public void updateMonster(int id, int x, int y, int health) {
        for (Monster m : monsters) {
            if (m.getId() == id) {
                // Debug: Check if position is actually changing
                if (m.getWorldX() != x || m.getWorldY() != y) {
                    System.out.println("[MonsterSync] Monster #" + id + " moved: (" + m.getWorldX() + "," + m.getWorldY() + ") -> (" + x + "," + y + ")");
                }
                m.setWorldX(x);
                m.setWorldY(y);
                m.setHealth(health); 
                break;
            }
        }
    }
    
    /**
     * Spawn a new monster with difficulty scaling
     */
    private void spawnMonster() {
        if (monsters.size() >= maxMonsters) return;
        
        // Choose random spawn position (avoid spawning near player)
        int playerX = gameScene.getPlayer().getWorldX();
        int playerY = gameScene.getPlayer().getWorldY();
        
        int spawnX, spawnY;
        int attempts = 0;
        int minSpawnDistance = 300; // Minimum distance from player
        
        do {
            spawnX = mapMinX + random.nextInt(Math.max(1, mapMaxX - mapMinX));
            spawnY = mapMinY + random.nextInt(Math.max(1, mapMaxY - mapMinY));
            attempts++;
        } while (Math.sqrt(Math.pow(spawnX - playerX, 2) + Math.pow(spawnY - playerY, 2)) < minSpawnDistance && attempts < 20);
        
        // Select monster type based on wave
        MonsterType type = selectMonsterType();
        
        // Create monster with difficulty scaling
        Monster monster = new Monster(nextMonsterId++, spawnX, spawnY, type, difficultyMultiplier);
        // Set map bounds for this monster
        monster.setMapBounds(mapMinX, mapMaxX, mapMinY, mapMaxY);
        monsters.add(monster);
    }
    
    /**
     * Spawn a wave of monsters with scaled difficulty
     */
    private void spawnWave() {
        int toSpawn = Math.min(monstersPerWave, maxMonsters - monsters.size());
        
        for (int i = 0; i < toSpawn; i++) {
            spawnMonster();
        }
        
        // Spawn boss every 5 waves (with extra difficulty scaling)
        if (waveNumber % 5 == 0 && monsters.size() < maxMonsters) {
            int playerX = gameScene.getPlayer().getWorldX();
            int playerY = gameScene.getPlayer().getWorldY();
            
            int spawnX = mapMinX + random.nextInt(mapMaxX - mapMinX);
            int spawnY = mapMinY + random.nextInt(mapMaxY - mapMinY);
            
            // Boss has extra difficulty scaling
            float bossMultiplier = difficultyMultiplier * 1.2f;
            Monster boss = new Monster(nextMonsterId++, spawnX, spawnY, MonsterType.BOSS, bossMultiplier);
            // Set map bounds for boss
            boss.setMapBounds(mapMinX, mapMaxX, mapMinY, mapMaxY);
            monsters.add(boss);
        }
    }
    
    /**
     * Transition to next wave with increased difficulty
     */
    private void nextWave() {
        waveNumber++;
        monstersKilledInWave = 0;
        
        // Increase difficulty
        monstersPerWave = Math.min(monstersPerWave + 2, 15);
        spawnInterval = Math.max(spawnInterval - 10, 60); // Spawn faster
        
        // Scale monster stats
        difficultyMultiplier = 1.0f + (waveNumber - 1) * DIFFICULTY_INCREMENT_PER_WAVE;
        
        spawnWave();
    }
    
    /**
     * Select monster type based on current wave
     */
    private MonsterType selectMonsterType() {
        int roll = random.nextInt(100);
        
        if (waveNumber >= 10) {
            // Wave 10+: more strong monsters
            if (roll < 20) return MonsterType.SLIME;
            if (roll < 50) return MonsterType.GOBLIN;
            return MonsterType.ORC;
        } else if (waveNumber >= 5) {
            // Wave 5-9: mix of all types
            if (roll < 40) return MonsterType.SLIME;
            if (roll < 80) return MonsterType.GOBLIN;
            return MonsterType.ORC;
        } else {
            // Wave 1-4: mostly slimes
            if (roll < 70) return MonsterType.SLIME;
            return MonsterType.GOBLIN;
        }
    }
    
    /**
     * Check bullet collision with all monsters
     * Returns array: [goldReward, damage, monsterWorldX, monsterWorldY, killed] or null
     */
    public int[] checkBulletCollisionDetailed(Bullet bullet, String shooterUsername, int damage) {
        if (bullet.isStop()) return null;
        
        // Create a copy to avoid ConcurrentModificationException
        ArrayList<Monster> monstersCopy = new ArrayList<>(monsters);
        
        for (Monster monster : monstersCopy) {
            // Skip dead or dying monsters
            if (!monster.isAlive() || monster.isDying()) continue;
            
            if (monster.checkBulletCollision(bullet)) {
                bullet.setStop(true);
                
                int monsterX = monster.getWorldX();
                int monsterY = monster.getWorldY();
                
                // Apply damage and get gold if killed
                int goldEarned = monster.takeDamage(damage);
                if (goldEarned > 0) {
                    monstersKilledInWave++;
                    // System.out.println("[KILL] Monster killed! Wave kills: " + monstersKilledInWave + "/" + monstersPerWave);
                    return new int[] { goldEarned, damage, monsterX, monsterY, 1, monster.getId() }; // 1 = killed
                }
                return new int[] { 0, damage, monsterX, monsterY, 0, monster.getId() }; // 0 = hit but not killed
            }
        }
        return null;
    }
    
    /**
     * Check bullet collision with all monsters (legacy method)
     * Returns goldReward if monster is killed, 0 otherwise
     */
    public int checkBulletCollision(Bullet bullet, String shooterUsername) {
        int[] result = checkBulletCollisionDetailed(bullet, shooterUsername, 25);
        return result != null ? result[0] : 0;
    }
    
    /**
     * Check player collision with all monsters
     * Returns damage if attacked, 0 otherwise
     */
    public int checkPlayerCollision(Player player) {
        for (Monster monster : monsters) {
            if (monster.checkPlayerCollision(player) && monster.canAttack()) {
                // Reset cooldown after dealing damage
                monster.resetAttackCooldown();
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
    
    // Getters
    public ArrayList<Monster> getMonsters() {
        return monsters;
    }
    
    public int getWaveNumber() {
        return waveNumber;
    }

    public void setWaveNumber(int waveNumber) {
        this.waveNumber = waveNumber;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public int getMonstersAlive() {
        return (int) monsters.stream().filter(m -> m.isAlive() && !m.isDying()).count();
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
    
    public float getDifficultyMultiplier() {
        return difficultyMultiplier;
    }
}
