package objects.entities;

import main.GameScene;
import objects.entities.Monster.MonsterType;

import java.util.ArrayList;
import java.util.Random;

/**
 * MonsterSpawner quản lý việc spawn quái vật trong chế độ Đối kháng điểm số.
 * Spawn quái theo wave với độ khó tăng dần.
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
     * Bắt đầu spawner
     */
    public void start() {
        isActive = true;
        waveNumber = 1;
        monstersKilledInWave = 0;
        spawnTimer = 0;
        monsters.clear();
        nextMonsterId = 0;
        
        // Spawn wave đầu tiên
        spawnWave();
    }
    
    /**
     * Dừng spawner và clear tất cả quái
     */
    public void stop() {
        isActive = false;
        monsters.clear();
    }
    
    /**
     * Update spawner - gọi mỗi frame
     */
    public void update(Player targetPlayer) {
        if (!isActive) return;
        
        // Update tất cả monsters
        for (Monster monster : monsters) {
            if (monster.isAlive()) {
                monster.updateAI(targetPlayer);
            }
        }
        
        // Xóa quái đã chết
        monsters.removeIf(m -> !m.isAlive());
        
        // Spawn timer
        spawnTimer++;
        if (spawnTimer >= spawnInterval && monsters.size() < maxMonsters) {
            spawnMonster();
            spawnTimer = 0;
        }
        
        // Kiểm tra chuyển wave
        if (monstersKilledInWave >= monstersPerWave && monsters.isEmpty()) {
            nextWave();
        }
    }
    
    /**
     * Spawn một quái mới
     */
    private void spawnMonster() {
        if (monsters.size() >= maxMonsters) return;
        
        // Chọn vị trí spawn ngẫu nhiên (tránh spawn gần player)
        int playerX = gameScene.getPlayer().getWorldX();
        int playerY = gameScene.getPlayer().getWorldY();
        
        int spawnX, spawnY;
        int attempts = 0;
        do {
            spawnX = mapMinX + random.nextInt(mapMaxX - mapMinX);
            spawnY = mapMinY + random.nextInt(mapMaxY - mapMinY);
            attempts++;
        } while (Math.sqrt(Math.pow(spawnX - playerX, 2) + Math.pow(spawnY - playerY, 2)) < 200 && attempts < 10);
        
        // Chọn loại quái dựa trên wave
        MonsterType type = selectMonsterType();
        
        Monster monster = new Monster(nextMonsterId++, spawnX, spawnY, type);
        monsters.add(monster);
    }
    
    /**
     * Spawn một wave quái
     */
    private void spawnWave() {
        int toSpawn = Math.min(monstersPerWave, maxMonsters - monsters.size());
        
        for (int i = 0; i < toSpawn; i++) {
            spawnMonster();
        }
        
        // Spawn boss mỗi 5 waves
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
     * Chuyển sang wave tiếp theo
     */
    private void nextWave() {
        waveNumber++;
        monstersKilledInWave = 0;
        
        // Tăng độ khó
        monstersPerWave = Math.min(monstersPerWave + 2, 15);
        spawnInterval = Math.max(spawnInterval - 10, 60); // Spawn nhanh hơn
        
        spawnWave();
    }
    
    /**
     * Chọn loại quái dựa trên wave hiện tại
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
     * Kiểm tra bullet collision với tất cả monsters
     * Trả về goldReward nếu monster bị tiêu diệt, 0 nếu không
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
     * Kiểm tra player collision với tất cả monsters
     * Trả về damage nếu bị tấn công, 0 nếu không
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
     * Lấy monster theo ID
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
     * Thêm monster từ server
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
     * Xóa monster theo ID
     */
    public void removeMonster(int id) {
        monsters.removeIf(m -> m.getId() == id);
    }
    
    /**
     * Cập nhật vị trí monster từ server
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
