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
     * Gửi yêu cầu bắt đầu game Score Battle
     */
    public String startScoreBattlePacket(String username) {
        message = "StartScoreBattle," + username;
        return message;
    }
    
    /**
     * Gửi cập nhật điểm số của người chơi
     */
    public String scoreUpdatePacket(String username, int score) {
        message = "ScoreUpdate," + username + "," + score;
        return message;
    }
    
    /**
     * Gửi thông báo tiêu diệt quái
     */
    public String monsterKillPacket(String username, int monsterId, int goldEarned) {
        message = "MonsterKill," + username + "," + monsterId + "," + goldEarned;
        return message;
    }
    
    /**
     * Gửi thông báo người chơi bị quái tấn công
     */
    public String playerDamagedPacket(String username, int damage, int remainingHealth) {
        message = "PlayerDamaged," + username + "," + damage + "," + remainingHealth;
        return message;
    }
    
    /**
     * Gửi yêu cầu cập nhật bảng xếp hạng Score Battle
     */
    public String scoreBattleLeaderboardPacket() {
        message = "ScoreBattleLeaderboard";
        return message;
    }
    
    /**
     * Gửi thông báo game kết thúc với điểm và kills để cập nhật leaderboard
     */
    public String scoreBattleEndPacket(String username, int finalScore, int kills) {
        message = "ScoreBattleEnd," + username + "," + finalScore + "," + kills;
        return message;
    }
    
    /**
     * Gửi điểm maze khi kết thúc game
     */
    public String mazeEndPacket(String username, int score, int coinsCollected, boolean won) {
        message = "MazeEnd," + username + "," + score + "," + coinsCollected + "," + (won ? "1" : "0");
        return message;
    }
    
    /**
     * Gửi thông báo spawn quái từ server
     */
    public String spawnMonsterPacket(int monsterId, int x, int y, String monsterType) {
        message = "SpawnMonster," + monsterId + "," + x + "," + y + "," + monsterType;
        return message;
    }
    
    /**
     * Gửi thông báo cập nhật vị trí quái
     */
    public String updateMonsterPacket(int monsterId, int x, int y, int health) {
        message = "UpdateMonster," + monsterId + "," + x + "," + y + "," + health;
        return message;
    }
    
    /**
     * Gửi thông báo xóa quái
     */
    public String removeMonsterPacket(int monsterId) {
        message = "RemoveMonster," + monsterId;
        return message;
    }
    
    /**
     * Gửi thông báo đồng bộ thời gian còn lại
     */
    public String syncTimePacket(int remainingTime) {
        message = "SyncTime," + remainingTime;
        return message;
    }
    
    /**
     * Gửi thông báo wave mới bắt đầu
     */
    public String newWavePacket(int waveNumber) {
        message = "NewWave," + waveNumber;
        return message;
    }
    
    // ============== Skin Shop Protocols ==============
    
    /**
     * Lấy danh sách skins trong shop
     */
    public String getSkinsPacket() {
        return "Shop,GetSkins";
    }
    
    /**
     * Lấy coins
     */
    public String getCoinsPacket() {
        return "Shop,GetCoins";
    }
    
    /**
     * Mua skin
     */
    public String buySkinPacket(int skinId) {
        return "Shop,Buy," + skinId;
    }
    
    /**
     * Lấy skins đã sở hữu
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
     * Lấy skin đang equip
     */
    public String getEquippedSkinPacket() {
        return "Shop,GetEquipped";
    }
}