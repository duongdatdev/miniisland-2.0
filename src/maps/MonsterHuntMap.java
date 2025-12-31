package maps;

import main.GameScene;
import network.client.Client;
import network.client.Protocol;
import objects.PowerUp;
import objects.PowerUp.PowerUpType;
import objects.entities.Bullet;
import objects.entities.DamageNumber;
import objects.entities.Monster;
import objects.entities.MonsterSpawner;
import objects.entities.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

/**
 * MonsterHuntMap - Chế độ Monster Hunt (Săn quái kiếm điểm)
 * Nhiều người chơi cùng xuất hiện trong một bản đồ top-down.
 * Người chơi bắn quái để nhận vàng trong thời gian giới hạn.
 * Quái có thể di chuyển và tấn công người chơi.
 */
public class MonsterHuntMap extends Map {
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
    
    // === NEW: Power-up System ===
    private ArrayList<PowerUp> powerUps;
    private int powerUpSpawnTimer = 0;
    private int powerUpSpawnInterval = 600; // 10 seconds
    private Random random;
    
    // === NEW: Active Buffs ===
    private float speedMultiplier = 1.0f;
    private float damageMultiplier = 1.0f;
    private float goldMultiplier = 1.0f;
    private boolean hasShield = false;
    private int speedBuffTimer = 0;
    private int damageBuffTimer = 0;
    private int goldBuffTimer = 0;
    private int shieldTimer = 0;
    
    // === NEW: Combo System ===
    private int comboCount = 0;
    private int comboTimer = 0;
    private static final int COMBO_TIMEOUT = 180; // 3 seconds to maintain combo
    private int maxCombo = 0;
    
    // === NEW: Kill Streak Events ===
    private int totalKills = 0;
    private String lastEventMessage = "";
    private int eventMessageTimer = 0;
    
    // === NEW: Difficulty scaling ===
    private float difficultyMultiplier = 1.0f;
    
    // === NEW: Damage Numbers ===
    private ArrayList<DamageNumber> damageNumbers;

    public MonsterHuntMap(GameScene gameScene) {
        super(gameScene);
        mapTileCol = 50;
        mapTileRow = 50;
        mapTileNum = new int[mapTileCol][mapTileRow];
        loadMap("/Maps/Pvp/pvpMap.png");
        readMap("/Maps/Pvp/pvpMap.csv");
        
        // Initialize monster spawner
        monsterSpawner = new MonsterSpawner(gameScene);
        
        // Set spawn bounds to playable area (tiles 11-38, row 11-38)
        // Map is 50x50 tiles, each 48px
        // Playable area starts at tile 10 (col/row) = 10*48 = 480px
        // Playable area ends at tile 39 = 39*48 = 1872px
        int tileSize = 48;
        int minTile = 11;
        int maxTile = 38;
        int minBound = minTile * tileSize; // 528
        int maxBound = maxTile * tileSize; // 1824
        monsterSpawner.setMapBounds(minBound, maxBound, minBound, maxBound);
        
        // Initialize score system
        playerScores = new HashMap<>();
        
        // Initialize power-up system
        powerUps = new ArrayList<>();
        random = new Random();
        
        // Initialize damage numbers
        damageNumbers = new ArrayList<>();
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
        
        // Reset power-up system
        powerUps.clear();
        powerUpSpawnTimer = 0;
        resetBuffs();
        
        // Reset combo and kills
        comboCount = 0;
        comboTimer = 0;
        maxCombo = 0;
        totalKills = 0;
        lastEventMessage = "";
        eventMessageTimer = 0;
        difficultyMultiplier = 1.0f;
        
        // Clear damage numbers
        damageNumbers.clear();
        
        // Spawn player in center of playable area
        int centerX = 24 * 48; // Tile 24 = center of playable area
        int centerY = 24 * 48;
        gameScene.getPlayer().setWorldX(centerX);
        gameScene.getPlayer().setWorldY(centerY);
        
        monsterSpawner.start();
    }
    
    /**
     * Kết thúc game mode
     */
    public void endGame() {
        gameStarted = false;
        gameEnded = true;
        monsterSpawner.stop();
        powerUps.clear();
        
        // Gửi điểm và số kills lên server để cập nhật leaderboard và lưu database
        String username = gameScene.getPlayerMP().getUsername();
        Client.getGameClient().sendToServer(
            new Protocol().scoreBattleEndPacket(username, localPlayerScore, totalKills)
        );
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
        powerUps.clear();
        resetBuffs();
        comboCount = 0;
        totalKills = 0;
        damageNumbers.clear();
    }
    
    /**
     * Reset tất cả buffs
     */
    private void resetBuffs() {
        speedMultiplier = 1.0f;
        damageMultiplier = 1.0f;
        goldMultiplier = 1.0f;
        hasShield = false;
        speedBuffTimer = 0;
        damageBuffTimer = 0;
        goldBuffTimer = 0;
        shieldTimer = 0;
    }
    
    /**
     * Update game logic - gọi mỗi frame
     */
    public void update() {
        if (!gameStarted || gameEnded) return;
        
        Player player = gameScene.getPlayer();
        
        // HANDLED BY SERVER
        // long currentTime = System.currentTimeMillis();
        // if (currentTime - lastTimeUpdate >= 1000) {
        //     remainingTime--;
        //     lastTimeUpdate = currentTime;
        //     
        //     // Increase difficulty over time
        //     if (remainingTime % 30 == 0 && remainingTime > 0) {
        //         difficultyMultiplier += 0.1f;
        //     }
        //     
        //     if (remainingTime <= 0) {
        //         endGame();
        //         return;
        //     }
        // }
        
        // Update combo timer
        if (comboTimer > 0) {
            comboTimer--;
            if (comboTimer <= 0) {
                comboCount = 0;
            }
        }
        
        // Update buff timers
        updateBuffTimers();
        
        // Update event message timer
        if (eventMessageTimer > 0) {
            eventMessageTimer--;
        }
        
        // Update monsters
        monsterSpawner.update(player);
        
        // Update power-ups
        updatePowerUps(player);
        
        // Check bullet-monster collisions
        checkBulletMonsterCollisions();
        
        // Check player-monster collisions
        checkPlayerMonsterCollision();
        
        // Spawn power-ups periodically
        powerUpSpawnTimer++;
        if (powerUpSpawnTimer >= powerUpSpawnInterval) {
            spawnPowerUp();
            powerUpSpawnTimer = 0;
        }
        
        // Update damage numbers
        for (DamageNumber dmgNum : damageNumbers) {
            dmgNum.update();
        }
        damageNumbers.removeIf(DamageNumber::shouldRemove);
    }

    public void setRemainingTime(int time) {
        this.remainingTime = time;
    }

    public void updatePlayerScore(String username, int score) {
        playerScores.put(username, score);
    }

    public HashMap<String, Integer> getPlayerScores() {
        return playerScores;
    }

    public MonsterSpawner getMonsterSpawner() {
        return monsterSpawner;
    }

    public void setPlayerHealth(int health) {
        this.playerHealth = health;
    }
    
    /**
     * Update buff timers
     */
    private void updateBuffTimers() {
        if (speedBuffTimer > 0) {
            speedBuffTimer--;
            if (speedBuffTimer <= 0) speedMultiplier = 1.0f;
        }
        if (damageBuffTimer > 0) {
            damageBuffTimer--;
            if (damageBuffTimer <= 0) damageMultiplier = 1.0f;
        }
        if (goldBuffTimer > 0) {
            goldBuffTimer--;
            if (goldBuffTimer <= 0) goldMultiplier = 1.0f;
        }
        if (shieldTimer > 0) {
            shieldTimer--;
            if (shieldTimer <= 0) hasShield = false;
        }
    }
    
    /**
     * Update power-ups và check collection
     */
    private void updatePowerUps(Player player) {
        for (PowerUp powerUp : powerUps) {
            powerUp.update();
            
            if (powerUp.checkCollision(player)) {
                collectPowerUp(powerUp);
            }
        }
        
        // Remove collected/expired power-ups
        powerUps.removeIf(PowerUp::shouldRemove);
    }
    
    /**
     * Collect a power-up and apply its effect
     */
    private void collectPowerUp(PowerUp powerUp) {
        powerUp.collect();
        PowerUpType type = powerUp.getType();
        
        switch (type) {
            case SPEED_BOOST:
                speedMultiplier = type.speedMultiplier;
                speedBuffTimer = type.duration;
                showEventMessage("SPEED BOOST!");
                break;
            case DOUBLE_DAMAGE:
                damageMultiplier = type.damageMultiplier;
                damageBuffTimer = type.duration;
                showEventMessage("DOUBLE DAMAGE!");
                break;
            case SHIELD:
                hasShield = true;
                shieldTimer = type.duration;
                showEventMessage("SHIELD ACTIVATED!");
                break;
            case GOLD_MAGNET:
                goldMultiplier = 1.5f;
                goldBuffTimer = type.duration;
                showEventMessage("GOLD BONUS!");
                break;
            case HEALTH_PACK:
                playerHealth = Math.min(playerHealth + 30, maxPlayerHealth);
                showEventMessage("+30 HP!");
                break;
        }
    }
    
    /**
     * Spawn a random power-up
     */
    private void spawnPowerUp() {
        if (powerUps.size() >= 3) return; // Max 3 power-ups on map
        
        // Spawn in playable area (tiles 11-38)
        int minBound = 11 * 48;
        int maxBound = 38 * 48;
        int spawnX = minBound + random.nextInt(maxBound - minBound);
        int spawnY = minBound + random.nextInt(maxBound - minBound);
        
        // Avoid spawning near player
        Player player = gameScene.getPlayer();
        int attempts = 0;
        while (Math.sqrt(Math.pow(spawnX - player.getWorldX(), 2) + 
                        Math.pow(spawnY - player.getWorldY(), 2)) < 200 && attempts < 10) {
            spawnX = 200 + random.nextInt(2000);
            spawnY = 200 + random.nextInt(2000);
            attempts++;
        }
        
        // Select random power-up type (weighted)
        PowerUpType type = selectRandomPowerUpType();
        powerUps.add(new PowerUp(spawnX, spawnY, type));
    }
    
    /**
     * Select power-up type with weighted randomness
     */
    private PowerUpType selectRandomPowerUpType() {
        int roll = random.nextInt(100);
        if (roll < 25) return PowerUpType.SPEED_BOOST;
        if (roll < 45) return PowerUpType.DOUBLE_DAMAGE;
        if (roll < 60) return PowerUpType.SHIELD;
        if (roll < 80) return PowerUpType.GOLD_MAGNET;
        return PowerUpType.HEALTH_PACK;
    }
    
    /**
     * Show event message on screen
     */
    private void showEventMessage(String message) {
        lastEventMessage = message;
        eventMessageTimer = 120; // 2 seconds
    }
    
    /**
     * Kiểm tra va chạm đạn với quái - gửi hit event lên server để xử lý
     */
    private void checkBulletMonsterCollisions() {
        String username = gameScene.getPlayerMP().getUsername();
        
        // Apply damage multiplier to bullet damage
        int bulletDamage = (int) (25 * damageMultiplier);
        
        // Check bullets from local player
        for (Bullet bullet : gameScene.getPlayerMP().getBullets()) {
            if (bullet != null && !bullet.isStop()) {
                // Check collision with each monster
                for (Monster monster : monsterSpawner.getMonsters()) {
                    if (monster.isAlive() && !monster.isDying() && monster.checkBulletCollision(bullet)) {
                        bullet.setStop(true);
                        
                        // Send hit to server for authoritative damage processing
                        Client.getGameClient().sendToServer(
                            new Protocol().monsterHitPacket(monster.getId(), bulletDamage, username)
                        );
                        
                        // Show damage number locally for instant feedback
                        boolean isCritical = damageMultiplier > 1.0f;
                        damageNumbers.add(new DamageNumber(monster.getWorldX() + 24, monster.getWorldY(), bulletDamage, isCritical));
                        
                        break; // Bullet can only hit one monster
                    }
                }
            }
        }
    }
    
    /**
     * Check for kill streak events
     */
    private void checkKillStreak() {
        switch (totalKills) {
            case 5:
                showEventMessage("KILLING SPREE! (5 kills)");
                break;
            case 10:
                showEventMessage("RAMPAGE! (10 kills)");
                break;
            case 15:
                showEventMessage("UNSTOPPABLE! (15 kills)");
                break;
            case 25:
                showEventMessage("GODLIKE! (25 kills)");
                break;
            case 50:
                showEventMessage("LEGENDARY! (50 kills)");
                break;
        }
    }
    
    /**
     * Kiểm tra va chạm người chơi với quái
     */
    private void checkPlayerMonsterCollision() {
        // Shield protects from damage
        if (hasShield) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDamageTime < damageCooldown) return;
        
        int damage = monsterSpawner.checkPlayerCollision(gameScene.getPlayer());
        if (damage > 0) {
            playerHealth -= damage;
            lastDamageTime = currentTime;
            
            // Reset combo on taking damage
            comboCount = 0;
            comboTimer = 0;
            
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
        
        // Reset combo and buffs on death
        comboCount = 0;
        comboTimer = 0;
        resetBuffs();
        
        showEventMessage("You died! Lost " + goldLost + " gold");
        
        // Teleport player to center of playable area
        int centerX = 24 * 48;
        int centerY = 24 * 48;
        gameScene.getPlayer().setWorldX(centerX);
        gameScene.getPlayer().setWorldY(centerY);
        
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
     * Xử lý khi quái chết và người chơi này là kẻ giết
     * Được gọi khi nhận MonsterDead packet từ server
     */
    public void handleLocalKill(int monsterId, int baseGold) {
        Monster monster = monsterSpawner.getMonster(monsterId);
        
        // Get monster position for effects before removing
        int mx = monster != null ? monster.getWorldX() : 0;
        int my = monster != null ? monster.getWorldY() : 0;
        
        // Update combo
        comboCount++;
        comboTimer = COMBO_TIMEOUT;
        maxCombo = Math.max(maxCombo, comboCount);
        
        // Calculate final gold with multipliers
        float comboBonus = 1.0f + (comboCount * 0.1f); // +10% per combo
        int finalGold = (int) (baseGold * goldMultiplier * comboBonus);
        
        // Show gold number
        if (monster != null) {
            damageNumbers.add(new DamageNumber(mx + 24, my - 20, finalGold));
        }
        
        // Add score
        localPlayerScore += finalGold;
        totalKills++;
        
        // Check kill streak
        checkKillStreak();
        
        // Remove monster
        monsterSpawner.removeMonster(monsterId);
        
        // Send score update to server for leaderboard
        String username = gameScene.getPlayerMP().getUsername();
        Client.getGameClient().sendToServer(
            new Protocol().scoreUpdatePacket(username, localPlayerScore)
        );
    }
    
    /**
     * Cập nhật điểm của người chơi khác
     */
    // public void updatePlayerScore(String username, int score) {
    //     playerScores.put(username, score);
    // }

    @Override
    public void draw(Graphics2D g2d, int tileSize) {
        super.draw(g2d, tileSize);
        
        // Draw monsters
        drawMonsters(g2d, tileSize);
        
        // Draw power-ups
        drawPowerUps(g2d, tileSize);
        
        // Draw damage numbers
        drawDamageNumbers(g2d);
        
        // Draw UI overlay
        drawUI(g2d);
    }
    
    /**
     * Draw power-ups on the map
     */
    private void drawPowerUps(Graphics2D g2d, int tileSize) {
        Player player = gameScene.getPlayer();
        int playerWorldX = player.getWorldX();
        int playerWorldY = player.getWorldY();
        int playerScreenX = player.getScreenX();
        int playerScreenY = player.getScreenY();
        
        for (PowerUp powerUp : powerUps) {
            int worldX = powerUp.getX();
            int worldY = powerUp.getY();
            
            // Culling
            if (Math.abs(worldX - playerWorldX) > playerScreenX + tileSize * 2 ||
                Math.abs(worldY - playerWorldY) > playerScreenY + tileSize * 2) {
                continue;
            }
            
            int screenX = worldX - playerWorldX + playerScreenX;
            int screenY = worldY - playerWorldY + playerScreenY;
            
            powerUp.render(g2d, screenX, screenY, tileSize);
        }
    }
    
    /**
     * Draw floating damage numbers
     */
    private void drawDamageNumbers(Graphics2D g2d) {
        Player player = gameScene.getPlayer();
        int playerWorldX = player.getWorldX();
        int playerWorldY = player.getWorldY();
        int playerScreenX = player.getScreenX();
        int playerScreenY = player.getScreenY();
        
        for (DamageNumber dmgNum : damageNumbers) {
            dmgNum.render(g2d, playerWorldX, playerWorldY, playerScreenX, playerScreenY);
        }
    }
    
    /**
     * Draw all monsters on the map
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
            
            // Culling - only draw monsters within screen bounds
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
     * Draw UI overlay (score, timer, health)
     */
    private void drawUI(Graphics2D g2d) {
        // Save original state
        Color originalColor = g2d.getColor();
        Font originalFont = g2d.getFont();
        
        int screenWidth = gameScene.getScreenWidth();
        int screenHeight = gameScene.getScreenHeight();
        
        // === TOP LEFT: Stats Panel ===
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(10, 10, 180, 130, 10, 10);
        g2d.setColor(new Color(80, 80, 80));
        g2d.drawRoundRect(10, 10, 180, 130, 10, 10);
        
        // Gold/Score
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(Color.ORANGE);
        g2d.drawString("Gold: " + localPlayerScore, 20, 35);
        
        // Wave - Prominent display with background highlight
        int waveNum = monsterSpawner.getWaveNumber();
        g2d.setColor(new Color(100, 50, 150, 200)); // Purple background
        g2d.fillRoundRect(15, 40, 90, 24, 8, 8);
        
        // Wave border - changes color based on boss wave
        if (waveNum % 5 == 0) {
            g2d.setColor(new Color(255, 50, 50)); // Red border for boss wave
            g2d.setStroke(new BasicStroke(2));
        } else {
            g2d.setColor(new Color(255, 215, 0)); // Gold border
            g2d.setStroke(new BasicStroke(1));
        }
        g2d.drawRoundRect(15, 40, 90, 24, 8, 8);
        
        // Wave text
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.WHITE);
        String waveText = "WAVE " + waveNum;
        if (waveNum % 5 == 0) {
            waveText = "BOSS " + waveNum; // Boss wave indicator
        }
        g2d.drawString(waveText, 20, 58);
        
        // Wave progress (monsters killed / total)
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.setColor(Color.LIGHT_GRAY);
        String progressText = monsterSpawner.getMonstersKilledInWave() + "/" + monsterSpawner.getMonstersPerWave();
        g2d.drawString(progressText, 110, 58);
        
        // Monsters alive
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.CYAN);
        g2d.drawString("Monsters: " + monsterSpawner.getMonstersAlive(), 20, 82);
        
        // Kills
        g2d.setColor(Color.WHITE);
        g2d.drawString("Kills: " + totalKills, 20, 100);
        
        // Difficulty level
        float currentDifficulty = monsterSpawner.getDifficultyMultiplier();
        if (currentDifficulty > 1.0f) {
            g2d.setColor(new Color(255, 100, 100));
            g2d.drawString("Diff: x" + String.format("%.1f", currentDifficulty), 100, 82);
        }
        
        // Combo (if active)
        if (comboCount > 1) {
            g2d.setColor(new Color(255, 100, 0));
            g2d.drawString("COMBO x" + comboCount, 20, 118);
        }
        
        // === TOP CENTER: Timer + Health ===
        int centerPanelW = 250;
        int centerPanelX = (screenWidth - centerPanelW) / 2;
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(centerPanelX, 10, centerPanelW, 55, 10, 10);
        
        // Timer
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.setColor(remainingTime <= 30 ? Color.RED : Color.WHITE);
        String timeStr = String.format("%02d:%02d", remainingTime / 60, remainingTime % 60);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(timeStr, centerPanelX + (centerPanelW - fm.stringWidth(timeStr)) / 2, 35);
        
        // Health bar (below timer)
        int barWidth = centerPanelW - 20;
        int barHeight = 12;
        int barX = centerPanelX + 10;
        int barY = 45;
        
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRoundRect(barX, barY, barWidth, barHeight, 5, 5);
        
        float healthPercent = (float) playerHealth / maxPlayerHealth;
        Color healthColor = new Color(
            (int) (255 * (1 - healthPercent)),
            (int) (255 * healthPercent),
            0
        );
        g2d.setColor(healthColor);
        g2d.fillRoundRect(barX, barY, (int) (barWidth * healthPercent), barHeight, 5, 5);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        String hpText = playerHealth + "/" + maxPlayerHealth;
        g2d.drawString(hpText, barX + (barWidth - g2d.getFontMetrics().stringWidth(hpText)) / 2, barY + 10);
        
        // === TOP RIGHT: Leaderboard ===
        drawLeaderboard(g2d, screenWidth);
        
        // === CENTER TOP: Active Buffs ===
        drawActiveBuffs(g2d, screenWidth);
        
        // === BOTTOM CENTER: Weapon & Controls ===
        drawWeaponAndDashUI(g2d, screenWidth);
        
        // === CENTER: Event message ===
        if (eventMessageTimer > 0) {
            drawEventMessage(g2d, screenWidth);
        }
        
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
     * Draw active buff icons
     */
    private void drawActiveBuffs(Graphics2D g2d, int screenWidth) {
        // Count active buffs to center them
        int activeCount = 0;
        if (speedBuffTimer > 0) activeCount++;
        if (damageBuffTimer > 0) activeCount++;
        if (shieldTimer > 0) activeCount++;
        if (goldBuffTimer > 0) activeCount++;
        
        if (activeCount == 0) return;
        
        int gap = 38;
        int buffX = (screenWidth - (activeCount * gap - 8)) / 2;
        int buffY = 75; // Below the timer/health panel
        
        // Speed buff
        if (speedBuffTimer > 0) {
            drawBuffIcon(g2d, buffX, buffY, PowerUpType.SPEED_BOOST.color, "SPD", speedBuffTimer);
            buffX += gap;
        }
        
        // Damage buff
        if (damageBuffTimer > 0) {
            drawBuffIcon(g2d, buffX, buffY, PowerUpType.DOUBLE_DAMAGE.color, "DMG", damageBuffTimer);
            buffX += gap;
        }
        
        // Shield buff
        if (shieldTimer > 0) {
            drawBuffIcon(g2d, buffX, buffY, PowerUpType.SHIELD.color, "DEF", shieldTimer);
            buffX += gap;
        }
        
        // Gold buff
        if (goldBuffTimer > 0) {
            drawBuffIcon(g2d, buffX, buffY, PowerUpType.GOLD_MAGNET.color, "$$$", goldBuffTimer);
        }
    }
    
    /**
     * Draw a buff icon with timer
     */
    private void drawBuffIcon(Graphics2D g2d, int x, int y, Color color, String icon, int timer) {
        // Background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(x, y, 30, 35, 5, 5);
        
        // Colored border
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x, y, 30, 35, 5, 5);
        
        // Icon
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString(icon, x + 8, y + 18);
        
        // Timer
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.setColor(Color.WHITE);
        String timerStr = String.valueOf(timer / 60);
        g2d.drawString(timerStr + "s", x + 5, y + 32);
    }
    
    /**
     * Draw event message overlay
     */
    private void drawEventMessage(Graphics2D g2d, int screenWidth) {
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        
        // Fade effect
        int alpha = Math.min(255, eventMessageTimer * 3);
        g2d.setColor(new Color(255, 200, 0, alpha));
        
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(lastEventMessage);
        int x = (screenWidth - textWidth) / 2;
        int y = 150;
        
        // Drop shadow
        g2d.setColor(new Color(0, 0, 0, alpha / 2));
        g2d.drawString(lastEventMessage, x + 2, y + 2);
        
        // Main text
        g2d.setColor(new Color(255, 200, 0, alpha));
        g2d.drawString(lastEventMessage, x, y);
    }
    
    /**
     * Draw weapon type and dash cooldown UI (bottom left corner, above teleport buttons)
     */
    private void drawWeaponAndDashUI(Graphics2D g2d, int screenWidth) {
        int screenHeight = gameScene.getScreenHeight();
        int panelWidth = 160;
        int panelHeight = 70;
        int panelX = 10;
        int panelY = screenHeight - panelHeight - 120; // Above teleport buttons (buttons at screenHeight-100)
        
        // Background panel
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);
        g2d.setColor(new Color(80, 80, 80));
        g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);
        
        // Get current bullet type from PlayerMP
        Bullet.BulletType bulletType = gameScene.getPlayerMP().getCurrentBulletType();
        
        // Weapon indicator (left side)
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.setColor(Color.GRAY);
        g2d.drawString("[Q] WEAPON", panelX + 8, panelY + 14);
        
        g2d.setColor(bulletType.color);
        g2d.fillRoundRect(panelX + 8, panelY + 18, 65, 22, 5, 5);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString(bulletType.name(), panelX + 14, panelY + 33);
        
        // Dash indicator (right side)
        Player player = gameScene.getPlayer();
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.setColor(Color.GRAY);
        g2d.drawString("[RMB] DASH", panelX + 85, panelY + 14);
        
        int dashBarWidth = 65;
        int dashBarHeight = 22;
        int dashBarX = panelX + 85;
        int dashBarY = panelY + 18;
        
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRoundRect(dashBarX, dashBarY, dashBarWidth, dashBarHeight, 5, 5);
        
        float dashCooldownPercent = player.getDashCooldownPercent();
        if (dashCooldownPercent > 0) {
            int fillWidth = (int)((1 - dashCooldownPercent) * dashBarWidth);
            g2d.setColor(new Color(80, 80, 80));
            g2d.fillRoundRect(dashBarX, dashBarY, fillWidth, dashBarHeight, 5, 5);
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Arial", Font.BOLD, 9));
            g2d.drawString("WAIT", dashBarX + 20, dashBarY + 15);
        } else if (player.isDashing()) {
            g2d.setColor(new Color(0, 200, 255));
            g2d.fillRoundRect(dashBarX, dashBarY, dashBarWidth, dashBarHeight, 5, 5);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 9));
            g2d.drawString("DASH!", dashBarX + 17, dashBarY + 15);
        } else {
            g2d.setColor(new Color(0, 200, 100));
            g2d.fillRoundRect(dashBarX, dashBarY, dashBarWidth, dashBarHeight, 5, 5);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 9));
            g2d.drawString("READY", dashBarX + 15, dashBarY + 15);
        }
        
        // Controls hint (above the panel)
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        g2d.setColor(Color.GRAY);
        g2d.drawString("LMB: Shoot | RMB: Dash | Q/E: Weapons", panelX + 8, panelY - 5);
        
        // Draw crosshair
        drawAimCrosshair(g2d);
    }
    
    /**
     * Draw crosshair for mouse aiming
     */
    private void drawAimCrosshair(Graphics2D g2d) {
        input.MouseHandler mouse = gameScene.getMouseHandler();
        if (mouse == null) return;
        
        int mouseX = mouse.getMouseX();
        int mouseY = mouse.getMouseY();
        
        // Crosshair size
        int size = 12;
        int gap = 4;
        int thickness = 2;
        
        // Get current bullet type color
        Bullet.BulletType bulletType = gameScene.getPlayerMP().getCurrentBulletType();
        Color crosshairColor = bulletType.color;
        
        // Set composite for slight transparency
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g2d.setColor(crosshairColor);
        g2d.setStroke(new BasicStroke(thickness));
        
        // Draw crosshair lines
        // Top
        g2d.drawLine(mouseX, mouseY - gap - size, mouseX, mouseY - gap);
        // Bottom
        g2d.drawLine(mouseX, mouseY + gap, mouseX, mouseY + gap + size);
        // Left
        g2d.drawLine(mouseX - gap - size, mouseY, mouseX - gap, mouseY);
        // Right
        g2d.drawLine(mouseX + gap, mouseY, mouseX + gap + size, mouseY);
        
        // Center dot
        g2d.fillOval(mouseX - 2, mouseY - 2, 4, 4);
        
        // Outer circle (optional, shows aim direction)
        g2d.setStroke(new BasicStroke(1));
        g2d.drawOval(mouseX - size - gap, mouseY - size - gap, (size + gap) * 2, (size + gap) * 2);
        
        // Reset composite
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
    
    /**
     * Draw leaderboard panel (top right, below chat button)
     */
    private void drawLeaderboard(Graphics2D g2d, int screenWidth) {
        // Collect all players including local player for proper ranking
        java.util.TreeMap<Integer, java.util.List<String>> sortedScores = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
        
        // Add local player
        String localName = gameScene.getPlayerMP().getUsername();
        sortedScores.computeIfAbsent(localPlayerScore, k -> new java.util.ArrayList<>()).add(localName);
        
        // Add other players (avoid duplicates)
        for (java.util.Map.Entry<String, Integer> entry : playerScores.entrySet()) {
            if (!entry.getKey().equals(localName)) {
                sortedScores.computeIfAbsent(entry.getValue(), k -> new java.util.ArrayList<>()).add(entry.getKey());
            }
        }
        
        // Flatten to list of entries for display
        java.util.List<java.util.AbstractMap.SimpleEntry<String, Integer>> rankedPlayers = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Integer, java.util.List<String>> entry : sortedScores.entrySet()) {
            for (String name : entry.getValue()) {
                rankedPlayers.add(new java.util.AbstractMap.SimpleEntry<>(name, entry.getKey()));
            }
        }
        
        int maxPlayers = Math.min(rankedPlayers.size(), 5);
        int lbWidth = 180;
        int lbHeight = 35 + maxPlayers * 24;
        int lbX = screenWidth - lbWidth - 10;
        int lbY = 80;
        
        // Gradient background
        GradientPaint gradient = new GradientPaint(lbX, lbY, new Color(30, 30, 50, 220), 
                                                    lbX, lbY + lbHeight, new Color(20, 20, 35, 220));
        g2d.setPaint(gradient);
        g2d.fillRoundRect(lbX, lbY, lbWidth, lbHeight, 12, 12);
        
        // Golden border
        g2d.setColor(new Color(180, 150, 50));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(lbX, lbY, lbWidth, lbHeight, 12, 12);
        g2d.setStroke(new BasicStroke(1));
        
        // Title with trophy icon
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(new Color(255, 215, 0)); // Gold color
        g2d.drawString("🏆 RANKING", lbX + 12, lbY + 20);
        
        // Divider line
        g2d.setColor(new Color(100, 100, 120));
        g2d.drawLine(lbX + 10, lbY + 28, lbX + lbWidth - 10, lbY + 28);
        
        // Draw players with rank
        int yOffset = 48;
        for (int i = 0; i < maxPlayers; i++) {
            java.util.AbstractMap.SimpleEntry<String, Integer> entry = rankedPlayers.get(i);
            String name = entry.getKey();
            int score = entry.getValue();
            boolean isLocal = name.equals(localName);
            
            // Truncate name
            if (name.length() > 12) name = name.substring(0, 12) + "..";
            
            // Rank medal/number
            String rankIcon;
            Color rankColor;
            switch (i) {
                case 0: rankIcon = "🥇"; rankColor = new Color(255, 215, 0); break;   // Gold
                case 1: rankIcon = "🥈"; rankColor = new Color(192, 192, 192); break; // Silver
                case 2: rankIcon = "🥉"; rankColor = new Color(205, 127, 50); break;  // Bronze
                default: rankIcon = (i + 1) + "."; rankColor = new Color(150, 150, 170); break;
            }
            
            // Highlight local player row
            if (isLocal) {
                g2d.setColor(new Color(0, 100, 50, 80));
                g2d.fillRoundRect(lbX + 5, lbY + yOffset - 14, lbWidth - 10, 20, 5, 5);
            }
            
            // Draw rank
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2d.setColor(rankColor);
            g2d.drawString(rankIcon, lbX + 10, lbY + yOffset);
            
            // Draw name
            g2d.setFont(new Font("Arial", isLocal ? Font.BOLD : Font.PLAIN, 11));
            g2d.setColor(isLocal ? Color.GREEN : Color.WHITE);
            g2d.drawString(name, lbX + 35, lbY + yOffset);
            
            // Draw score (right-aligned)
            String scoreStr = String.valueOf(score);
            int scoreWidth = g2d.getFontMetrics().stringWidth(scoreStr);
            g2d.setColor(isLocal ? new Color(100, 255, 100) : new Color(255, 230, 150));
            g2d.drawString(scoreStr, lbX + lbWidth - scoreWidth - 12, lbY + yOffset);
            
            yOffset += 24;
        }
    }
    
    /**
     * Draw game over screen
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
        g2d.drawString(gameOverText, (screenWidth - textWidth) / 2, screenHeight / 2 - 80);
        
        // Final score
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.setColor(Color.YELLOW);
        String scoreText = "Your Score: " + localPlayerScore + " Gold";
        textWidth = g2d.getFontMetrics().stringWidth(scoreText);
        g2d.drawString(scoreText, (screenWidth - textWidth) / 2, screenHeight / 2 - 30);
        
        // Stats
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        g2d.setColor(Color.WHITE);
        
        String killsText = "Total Kills: " + totalKills;
        textWidth = g2d.getFontMetrics().stringWidth(killsText);
        g2d.drawString(killsText, (screenWidth - textWidth) / 2, screenHeight / 2 + 10);
        
        String comboText = "Max Combo: x" + maxCombo;
        textWidth = g2d.getFontMetrics().stringWidth(comboText);
        g2d.drawString(comboText, (screenWidth - textWidth) / 2, screenHeight / 2 + 35);
        
        String waveText = "Waves Survived: " + monsterSpawner.getWaveNumber();
        textWidth = g2d.getFontMetrics().stringWidth(waveText);
        g2d.drawString(waveText, (screenWidth - textWidth) / 2, screenHeight / 2 + 60);
        
        // Instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.setColor(Color.CYAN);
        String instrText = "Press SPACE to play again or ESC to exit";
        textWidth = g2d.getFontMetrics().stringWidth(instrText);
        g2d.drawString(instrText, (screenWidth - textWidth) / 2, screenHeight / 2 + 100);
    }
    
    /**
     * Draw waiting/start screen
     */
    private void drawWaitingScreen(Graphics2D g2d, int screenWidth) {
        int screenHeight = gameScene.getScreenHeight();
        
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, screenWidth, screenHeight);
        
        // Title
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        g2d.setColor(Color.YELLOW);
        String title = "== MONSTER HUNT ==";
        int textWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, (screenWidth - textWidth) / 2, screenHeight / 2 - 80);
        
        // Instructions
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.setColor(Color.WHITE);
        String text = "Press SPACE to Start!";
        textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, (screenWidth - textWidth) / 2, screenHeight / 2 - 30);
        
        // Game description
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        g2d.setColor(Color.CYAN);
        String[] descriptions = {
            "> Defeat monsters to earn gold!",
            "> Collect power-ups for buffs!",
            "> Build combos for bonus gold!",
            "> Avoid taking damage to stay alive!",
            "> Monsters get stronger each wave!",
            "> Time limit: 3 minutes"
        };
        
        int y = screenHeight / 2 + 20;
        for (String desc : descriptions) {
            textWidth = g2d.getFontMetrics().stringWidth(desc);
            g2d.drawString(desc, (screenWidth - textWidth) / 2, y);
            y += 28;
        }
        
        // Controls hint
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.setColor(Color.GRAY);
        String controls = "WASD: Move | SPACE: Shoot | ESC: Exit";
        textWidth = g2d.getFontMetrics().stringWidth(controls);
        g2d.drawString(controls, (screenWidth - textWidth) / 2, screenHeight - 50);
    }

    @Override
    protected void renderNPC(Graphics2D g2d) {
        gameScene.getLobbyMap().getMonsterHuntNPC().checkDraw(gameScene.getPlayer(), g2d);
    }

    @Override
    public void setTileType(int i) {
        if(i == 13) {
            tiles[i].setType(TileType.Wall);
        }
    }
    
    // Getters and Setters
    // public MonsterSpawner getMonsterSpawner() {
    //     return monsterSpawner;
    // }
    
    public int getLocalPlayerScore() {
        return localPlayerScore;
    }
    
    public void setLocalPlayerScore(int score) {
        this.localPlayerScore = score;
    }
    
    public int getRemainingTime() {
        return remainingTime;
    }
    
    // public void setRemainingTime(int time) {
    //     this.remainingTime = time;
    // }
    
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
    
    // public void setPlayerHealth(int health) {
    //     this.playerHealth = health;
    // }
    
    public int getMaxPlayerHealth() {
        return maxPlayerHealth;
    }
    
    // public HashMap<String, Integer> getPlayerScores() {
    //     return playerScores;
    // }
    
    public int getGameTimeLimit() {
        return gameTimeLimit;
    }
    
    public void setGameTimeLimit(int timeLimit) {
        this.gameTimeLimit = timeLimit;
    }
    
    // === NEW Getters ===
    public int getComboCount() { return comboCount; }
    public int getMaxCombo() { return maxCombo; }
    public int getTotalKills() { return totalKills; }
    public float getSpeedMultiplier() { return speedMultiplier; }
    public float getDamageMultiplier() { return damageMultiplier; }
    public boolean hasShield() { return hasShield; }
}
