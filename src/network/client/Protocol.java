package network.client;

public class Protocol {


    /**
     * Creates a new instance of Protocol
     */
    private String message = "";

    public Protocol() {

    }

    public String UpdatePacket(String username, int x, int y, int dir) {
        message = "Update," + username + "," + x + "," + y + "," + dir;
        return message;
    }

    public String chatPacket(String username, String message) {
        message = "Chat," + username + "," + message;
        return message;
    }

    public String RegisterPacket(String username, String password, String email) {
        message = "player/1/Register" + username + "," + password + "-" + email;
        return message;
    }

    public String LoginPacket(String username, String password) {
        message = "Login" + username + "," + password;
        return message;
    }

    public String HelloPacket(String username) {
        message = "Hello" + username;
        return message;
    }

    public String ShotPacket(String username) {
        message = "Shot" + username;
        return message;
    }
    
    /**
     * Shot packet with position and direction for accurate bullet display on other clients
     */
    public String ShotPacketWithDirection(String username, int x, int y, float dirX, float dirY) {
        message = "ShotDir," + username + "," + x + "," + y + "," + dirX + "," + dirY;
        return message;
    }

    public String teleportPacket(String username, String map, int x, int y) {
        message = "TeleportToMap," + username + "," + map + "," + x + "," + y;
        return message;
    }

   public String enterMazePacket(String username) {
        message = "EnterMaze" + username;
        return message;
    }

    public String winMazePacket(String username) {
        message = "WinMaze" + username;
        return message;
    }

    public String PlayerExitMapPacket(String username, String map) {
        message = "ExitMap" + username + "," + map;
        return message;
    }

    public String bulletCollisionPacket(String playerShot, String playerHit) {
        message = "BulletCollision," + playerShot + "," + playerHit;
        return message;
    }

    public String respawnPacket(String username) {
        message = "Respawn" + username;
        return message;
    }

    public String RemoveClientPacket(String username) {
        message = "Remove" + username;
        return message;
    }

    public String ExitMessagePacket(String username) {
        message = "Exit" + username;
        return message;
    }
    
    // ============== Score Battle Mode Protocols ==============
    
    /**
     * Send request to start Score Battle game
     */
    public String startScoreBattlePacket(String username) {
        message = "StartScoreBattle," + username;
        return message;
    }
    
    /**
     * Send player point update
     */
    public String scoreUpdatePacket(String username, int score) {
        message = "ScoreUpdate," + username + "," + score;
        return message;
    }
    
    public String monsterDeadPacket(int monsterId, String killer, int points) {
        message = "MonsterDead," + monsterId + "," + killer + "," + points;
        return message;
    }
    
    /**
     * Send notification that monster was hit to server to handle damage
     */
    public String monsterHitPacket(int monsterId, int damage, String shooterUsername) {
        message = "MonsterHit," + monsterId + "," + damage + "," + shooterUsername;
        return message;
    }
    
    /**
     * Send notification of monster kill
     */
    public String monsterKillPacket(String username, int monsterId, int goldEarned) {
        message = "MonsterKill," + username + "," + monsterId + "," + goldEarned;
        return message;
    }
    
    /**
     * Send notification that player was damaged
     */
    public String playerDamagedPacket(String username, int damage, int remainingHealth) {
        message = "PlayerDamaged," + username + "," + damage + "," + remainingHealth;
        return message;
    }
    
    /**
     * Send request to update Score Battle leaderboard
     */
    public String scoreBattleLeaderboardPacket() {
        message = "ScoreBattleLeaderboard";
        return message;
    }
    
    /**
     * Send game end notification with score and kills to update leaderboard
     */
    public String scoreBattleEndPacket(String username, int finalScore, int kills) {
        message = "ScoreBattleEnd," + username + "," + finalScore + "," + kills;
        return message;
    }
    
    /**
     * Send maze points when game ends
     */
    public String mazeEndPacket(String username, int score, int coinsCollected, boolean won) {
        message = "MazeEnd," + username + "," + score + "," + coinsCollected + "," + (won ? "1" : "0");
        return message;
    }
    
    /**
     * Send notification of monster spawn from server
     */
    public String spawnMonsterPacket(int monsterId, int x, int y, String monsterType) {
        message = "SpawnMonster," + monsterId + "," + x + "," + y + "," + monsterType;
        return message;
    }
    
    /**
     * Send notification of monster position update
     */
    public String updateMonsterPacket(int monsterId, int x, int y, int health) {
        message = "UpdateMonster," + monsterId + "," + x + "," + y + "," + health;
        return message;
    }
    
    /**
     * Send notification of monster removal
     */
    public String removeMonsterPacket(int monsterId) {
        message = "RemoveMonster," + monsterId;
        return message;
    }
    
    /**
     * Send time sync notification
     */
    public String syncTimePacket(int remainingTime) {
        message = "SyncTime," + remainingTime;
        return message;
    }
    
    /**
     * Send new wave notification
     */
    public String newWavePacket(int waveNumber) {
        message = "NewWave," + waveNumber;
        return message;
    }
    
    // ============== Skin Shop Protocols ==============
    
    /**
     * Get list of skins in shop
     */
    public String getSkinsPacket() {
        return "Shop,GetSkins";
    }
    
    /**
     * Get coins
     */
    public String getCoinsPacket() {
        return "Shop,GetCoins";
    }
    
    /**
     * Buy skin
     */
    public String buySkinPacket(int skinId) {
        return "Shop,Buy," + skinId;
    }
    
    /**
     * Get owned skins
     */
    public String getMySkinsPacket() {
        return "Shop,GetMySkins";
    }
    
    /**
     * Equip skin
     */
    public String equipSkinPacket(int skinId) {
        return "Shop,Equip," + skinId;
    }
    
    /**
     * Get equipped skin
     */
    public String getEquippedSkinPacket() {
        return "Shop,GetEquipped";
    }
}