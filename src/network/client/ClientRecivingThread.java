package network.client;

import main.GameScene;
import maps.MonsterHuntMap;
import network.entitiesNet.PlayerMP;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * This class is responsible for receiving messages from the server and updating the game state accordingly.
 * Updated to use WebSocket instead of TCP Socket
 * It is a thread that runs in the background and listens for messages from the server.
 * It updates the game state based on the messages received from the server.
 *
 * @author DuongDat
 */

public class ClientRecivingThread extends Thread {

    private PlayerMP clientPlayer;
    private GameScene gameScene;
    boolean isRunning = true;

    private WebSocketGameClient webSocketClient;

    /**
     * Creates a new instance of ClientReceivingThread
     *
     * @param clientPlayer the player
     * @param gameScene    the game scene
     */

    public ClientRecivingThread(WebSocketGameClient webSocketClient, PlayerMP clientPlayer, GameScene gameScene) {
        this.clientPlayer = clientPlayer;
        this.gameScene = gameScene;
        this.webSocketClient = webSocketClient;

        // Set up message listener for WebSocket
        webSocketClient.setMessageListener(new WebSocketGameClient.MessageListener() {
            @Override
            public void onMessageReceived(String message) {
                handleMessage(message);
            }
        });
    }

    @Override
    public void run() {
        // The WebSocket client handles messages through the listener
        // Keep this thread alive while the game is running
        while (isRunning) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                isRunning = false;
                break;
            }
        }
    }


    private void handleMessage(String sentence) {
        try {

                if (sentence.startsWith("ID")) {
                    int pos1 = sentence.indexOf(',');

                    int id = Integer.parseInt(sentence.substring(2, pos1));
                    String username = sentence.substring(pos1 + 1, sentence.length());

                    clientPlayer.setID(id);
                    clientPlayer.setUsername(username);

                    System.out.println("My ID= " + id);
                    System.out.println("My Username= " + clientPlayer.getUsername());
                } else if (sentence.startsWith("NewClient")) {
                    int pos1 = sentence.indexOf(',');
                    int pos2 = sentence.indexOf('-');
                    int pos3 = sentence.indexOf('|');
                    int pos4 = sentence.indexOf('!');
                    int pos5 = sentence.indexOf('#');

                    String username = sentence.substring(9, pos1);
                    int x = Integer.parseInt(sentence.substring(pos1 + 1, pos2));
                    int y = Integer.parseInt(sentence.substring(pos2 + 1, pos3));
                    int dir = Integer.parseInt(sentence.substring(pos3 + 1, pos4));
                    int id = Integer.parseInt(sentence.substring(pos4 + 1, pos5));
                    String map = sentence.substring(pos5 + 1, sentence.length());

                    if (!username.equals(gameScene.getPlayerMP().getUsername())) {
                        if (gameScene.getCurrentMap().equals(map))
                            gameScene.registerNewPlayer(new PlayerMP(username, x, y, dir, id));
                    }

                    System.out.println("New Client ID= " + id);
                    System.out.println("New Client Username= " + username);

                    PlayerMP player = gameScene.getMap().getPlayer(username);
                    if (player != null) {
                        BufferedImage chatImage = player.getDialogText().loadImage("Toi vua vao");
                        player.setChatImage(chatImage);
                    }

                } else if (sentence.startsWith("Update")) {
                    String[] parts = sentence.split(",");

                    String username = parts[1];
                    int x = Integer.parseInt(parts[2]);
                    int y = Integer.parseInt(parts[3]);
                    int dir = Integer.parseInt(parts[4]);

                    if (!username.equals(clientPlayer.getUsername())) {
                        PlayerMP player = gameScene.getMap().getPlayer(username);

                        if (player != null) {
                            player.setX(x);
                            player.setY(y);
                            if (dir == 0) {
                                player.setLastDirection(player.getDirection());
                            }
                            player.setDirection(dir);
                        } else {
                            System.out.println("Player with username " + username + " not found in the map.");
                        }
                    }

                } else if (sentence.startsWith("ShotDir")) {
                    // New format: ShotDir,username,x,y,dirX,dirY
                    String[] parts = sentence.split(",");
                    String username = parts[1];
                    
                    if (!username.equals(clientPlayer.getUsername())) {
                        System.out.println("Player " + username + " shot (with direction)");
                        
                        if (gameScene.getCurrentMap().equals("hunt")) {
                            PlayerMP player = gameScene.getMonsterHuntMap().getPlayer(username);
                            if (player != null) {
                                int x = Integer.parseInt(parts[2]);
                                int y = Integer.parseInt(parts[3]);
                                float dirX = Float.parseFloat(parts[4]);
                                float dirY = Float.parseFloat(parts[5]);
                                
                                player.ShotWithDirection(x, y, dirX, dirY);
                            }
                        }
                    }
                } else if (sentence.startsWith("Shot")) {
                    // Old format: Shot<username> - kept for backward compatibility
                    String username = sentence.substring(4);

                    if (username.equals(clientPlayer.getUsername())) {
//                        gameScene.getPlayerMP().shot();
                    } else {
                        System.out.println("Player " + username + " shot");

                        if (gameScene.getCurrentMap().equals("hunt")){
                            PlayerMP player = gameScene.getMonsterHuntMap().getPlayer(username);
                            if (player != null) {
                                player.Shot();
                            }
                        }
                    }
                // === PVP DISABLED - Comment out player-vs-player bullet collision ===
                // } else if (sentence.startsWith("BulletCollision")) {
                //     String[] parts = sentence.split(",");
                //
                //     String playerShot = parts[1];
                //     String playerHit = parts[2];
                //
                //     if (playerHit.equals(clientPlayer.getUsername())) {
                //         int response = JOptionPane.showConfirmDialog(null, "Sorry, You are loss. Do you want to try again ?", "2D Multiplayer Game", JOptionPane.OK_CANCEL_OPTION);
                //         if (response == JOptionPane.OK_OPTION) {
                //             gameScene.setPlayerAlive(true);
                //             gameScene.getPlayer().setWorldX(1000);
                //             gameScene.getPlayer().setWorldY(1000);
                //
                //             gameScene.getPlayerMP().setAlive(true);
                //             gameScene.sendRespawnPacket();
                //             gameScene.getMonsterHuntMap().removeAllPlayers();
                //             gameScene.sendTeleportPacket(gameScene.getPlayerMP().getUsername(), "hunt", gameScene.getPlayer().getWorldX(), gameScene.getPlayer().getWorldY());
                //         } else {
                //             gameScene.setPlayerAlive(true);
                //             gameScene.getPlayer().setDefaultPosition();
                //             gameScene.sendTeleportPacket(gameScene.getPlayerMP().getUsername(), "lobby", gameScene.getPlayer().getWorldX(), gameScene.getPlayer().getWorldY());
                //             gameScene.changeToLobby(gameScene.getMonsterHuntMap());
                //         }
                //     } else {
                //         for (PlayerMP player : gameScene.getMap().players) {
                //             if (player.getUsername().equals(playerHit)) {
                //                 gameScene.getMap().removePlayer(playerHit);
                //                 break;
                //             }
                //         }
                //     }
                // === END PVP DISABLED ===

                } else if (sentence.startsWith("Remove")) {
                    int id = Integer.parseInt(sentence.substring(6));

                    if (id == clientPlayer.getID()) {
                        int response = JOptionPane.showConfirmDialog(null, "Sorry, You are loss. Do you want to try again ?", "2D Multiplayer Game", JOptionPane.OK_CANCEL_OPTION);
                        if (response == JOptionPane.OK_OPTION) {
                            //han

                        } else {
                            System.exit(0);
                        }
                    } else {
                        String username = gameScene.getPlayerMP().getUsername();

                        gameScene.removePlayer(username);
                    }

                } else if (sentence.startsWith("TeleportToMap")) {
                    String[] parts = sentence.split(",");

                    String username = parts[1];
                    String mapName = parts[2];
                    int x = Integer.parseInt(parts[3]);
                    int y = Integer.parseInt(parts[4]);

                    gameScene.teleportPlayer(username, mapName, x, y);

                } else if (sentence.startsWith("EnterMaze")) {

                    String username = sentence.substring(9);

                    if (!username.equals(clientPlayer.getUsername()) && gameScene.getCurrentMap().equals("Lobby")) {

                        gameScene.getMap().removePlayer(username);

                    }
                } else if (sentence.startsWith("MazeWin")) {
                    String[] parts = sentence.split(",");
                    String username = parts[1];
                    gameScene.showSystemMessage("Player " + username + " has won the Maze!");
                } else if (sentence.startsWith("TeleportMap")) {

                    String[] parts = sentence.split(",");
                    String username = parts[1];
                    String map = parts[2];
                    int x = Integer.parseInt(parts[3]);
                    int y = Integer.parseInt(parts[4]);

                    if (username.equals(clientPlayer.getUsername())) {
                        gameScene.getPlayerMP().setX(x);
                        gameScene.getPlayerMP().setY(y);

                        gameScene.getLobbyMap().getMazeNPC().setWorldX(2092);
                        gameScene.getLobbyMap().getMazeNPC().setWorldY(1075);

                        gameScene.changeToLobby(gameScene.getMazeMap());
                    }

                } else if (sentence.startsWith("Chat")) {
                    String[] parts = sentence.split(",");

                    String username = parts[1];
                    String message = parts[2];

                    if (!username.equals(clientPlayer.getUsername())) {
                        PlayerMP player = gameScene.getMap().getPlayer(username);
                        if (player != null) {
                            BufferedImage chatImage = player.getDialogText().loadImage(message);
                            player.setChatImage(chatImage);
                        }
                    }
                } else if (sentence.startsWith("Leaderboard")) {

                    System.out.println(sentence);
                    String[] parts = sentence.split(",");

                    gameScene.getLeaderBoard().clear();
                    for (int i = 1; i < parts.length; i++) {
                        String[] playerInfo = parts[i].split(" ");

                        String username = playerInfo[0];
                        int score = Integer.parseInt(playerInfo[1]);

                        gameScene.getLeaderBoard().add(username, score);
                    }

                    gameScene.requestFocusInWindow();
                } else if (sentence.startsWith("Maze")) {
                    String map = sentence.substring(4);

                    gameScene.getMazeMap().clear();
                    gameScene.getPlayerMP().setX(0);
                    gameScene.getPlayerMP().setY(0);

                    //handle readMap from the server
                    gameScene.getMazeMap().readMap(map, () -> {

                        gameScene.changeToMazeMap();

                        gameScene.getPlayerMP().setX(50);
                        gameScene.getPlayerMP().setY(50);

                        gameScene.getLobbyMap().getMazeNPC().setWorldX(50);
                        gameScene.getLobbyMap().getMazeNPC().setWorldY(50);

                        Client.getGameClient().sendToServer(new Protocol().teleportPacket(gameScene.getPlayerMP().getUsername(), gameScene.currentMap, gameScene.getPlayerMP().getX(), gameScene.getPlayerMP().getY()));
                    });

                } else if (sentence.startsWith("Exit")) {
                    String username = sentence.substring(4, sentence.length());

                    if (!username.equals(clientPlayer.getUsername())) {
                        gameScene.removePlayer(username);
                    }
                }
                // ============== Score Battle Mode Messages ==============
                else if (sentence.startsWith("ScoreBattleStart")) {
                    // Server thông báo bắt đầu game
                    String[] parts = sentence.split(",");
                    int timeLimit = parts.length > 1 ? Integer.parseInt(parts[1]) : 180;
                    
                    gameScene.getMonsterHuntMap().setGameTimeLimit(timeLimit);
                    gameScene.getMonsterHuntMap().startGame();
                    System.out.println("Score Battle started! Time limit: " + timeLimit + "s");
                    
                } else if (sentence.startsWith("HuntWave")) {
                    String[] parts = sentence.split(",");
                    int wave = Integer.parseInt(parts[1]);
                    gameScene.getMonsterHuntMap().getMonsterSpawner().setWaveNumber(wave);
                    
                } else if (sentence.startsWith("HuntTime")) {
                    // Server đồng bộ thời gian
                    String[] parts = sentence.split(",");
                    int remainingTime = Integer.parseInt(parts[1]);
                    
                    // Auto-start game if receiving time sync (means game is already running)
                    if (!gameScene.getMonsterHuntMap().isGameStarted()) {
                        gameScene.getMonsterHuntMap().startGame();
                        System.out.println("Auto-started game due to HuntTime sync");
                    }
                    
                    gameScene.getMonsterHuntMap().setRemainingTime(remainingTime);
                    
                } else if (sentence.startsWith("HuntEnd")) {
                    // Server thông báo kết thúc game
                    gameScene.getMonsterHuntMap().endGame();
                    System.out.println("Score Battle ended!");
                    
                } else if (sentence.startsWith("SpawnMonster")) {
                    // Server spawn quái mới: SpawnMonster,id,type,x,y
                    String[] parts = sentence.split(",");
                    int monsterId = Integer.parseInt(parts[1]);
                    int type = Integer.parseInt(parts[2]);
                    int x = Integer.parseInt(parts[3]);
                    int y = Integer.parseInt(parts[4]);
                    
                    // Convert int type to String type for existing method or update method
                    String monsterType = "SLIME";
                    if (type == 1) monsterType = "GOBLIN";
                    if (type == 2) monsterType = "ORC";
                    
                    gameScene.getMonsterHuntMap().getMonsterSpawner().addMonster(monsterId, x, y, monsterType);
                    
                } else if (sentence.startsWith("MonsterUpdate")) {
                    // Server cập nhật vị trí/health quái: MonsterUpdate,id,x,y,health
                    try {
                        String[] parts = sentence.split(",");
                        int monsterId = Integer.parseInt(parts[1]);
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        int health = Integer.parseInt(parts[4]);
                        
                        if (gameScene.getMonsterHuntMap() != null && 
                            gameScene.getMonsterHuntMap().getMonsterSpawner() != null) {
                            gameScene.getMonsterHuntMap().getMonsterSpawner().updateMonster(monsterId, x, y, health);
                        }
                    } catch (Exception e) {
                        System.err.println("[MonsterUpdate] Error: " + e.getMessage());
                    }
                    
                } else if (sentence.startsWith("HuntLeaderboard")) {
                    // Cập nhật bảng xếp hạng Score Battle: HuntLeaderboard,user1:score1,user2:score2...
                    String[] parts = sentence.split(",");
                    
                    gameScene.getMonsterHuntMap().getPlayerScores().clear();
                    for (int i = 1; i < parts.length; i++) {
                        String[] playerInfo = parts[i].split(":");
                        if (playerInfo.length >= 2) {
                            String username = playerInfo[0];
                            int score = Integer.parseInt(playerInfo[1]);
                            
                            gameScene.getMonsterHuntMap().updatePlayerScore(username, score);
                            
                            // Sync local player score
                            if (username.equals(clientPlayer.getUsername())) {
                                gameScene.getMonsterHuntMap().setLocalPlayerScore(score);
                            }
                        }
                    }
                    
                } else if (sentence.startsWith("MonsterDead")) {
                    // MonsterDead,monsterId,killerName,points
                    String[] parts = sentence.split(",");
                    int monsterId = Integer.parseInt(parts[1]);
                    String killer = parts[2];
                    int baseGold = Integer.parseInt(parts[3]);
                    
                    // If this client is the killer, process score/combo
                    if (killer.equals(clientPlayer.getUsername())) {
                        gameScene.getMonsterHuntMap().handleLocalKill(monsterId, baseGold);
                    } else {
                        // Just remove the monster for other players
                        gameScene.getMonsterHuntMap().getMonsterSpawner().removeMonster(monsterId);
                    }
                    
                } else if (sentence.startsWith("PlayerDamaged")) {
                    // Thông báo người chơi bị damage
                    String[] parts = sentence.split(",");
                    String username = parts[1];
                    int damage = Integer.parseInt(parts[2]);
                    int remainingHealth = Integer.parseInt(parts[3]);
                    
                    if (username.equals(clientPlayer.getUsername())) {
                        gameScene.getMonsterHuntMap().setPlayerHealth(remainingHealth);
                    }
                }
                // ============== Skin Shop Messages ==============
                else if (sentence.startsWith("SkinsList")) {
                    if (gameScene.getShopPane() != null) {
                        gameScene.getShopPane().parseSkinsList(sentence);
                    }
                } else if (sentence.startsWith("PlayerCoins")) {
                    if (gameScene.getShopPane() != null) {
                        gameScene.getShopPane().parseCoins(sentence);
                    }
                } else if (sentence.startsWith("BuyResult")) {
                    if (gameScene.getShopPane() != null) {
                        gameScene.getShopPane().parseBuyResult(sentence);
                    }
                } else if (sentence.startsWith("PlayerSkins")) {
                    if (gameScene.getShopPane() != null) {
                        gameScene.getShopPane().parsePlayerSkins(sentence);
                    }
                } else if (sentence.startsWith("EquippedSkin")) {
                    // Parse skin folder and apply to player
                    String[] parts = sentence.split(",");
                    if (parts.length >= 2) {
                        String skinFolder = parts[1];
                        // Đổi skin cho chính player này
                        gameScene.getPlayer().changeSkin(skinFolder);
                        System.out.println("Loaded equipped skin: " + skinFolder);
                    }
                    
                    if (gameScene.getShopPane() != null) {
                        gameScene.getShopPane().parseEquippedSkin(sentence);
                    }
                } else if (sentence.startsWith("ChangeSkin")) {
                    // Player khác đổi skin: ChangeSkin,username,skinFolder
                    String[] parts = sentence.split(",");
                    if (parts.length >= 3) {
                        String username = parts[1];
                        String skinFolder = parts[2];
                        
                        // Đổi skin cho player khác
                        if (!username.equals(clientPlayer.getUsername())) {
                            // PRIORITIZE CURRENT MAP
                            // Check the current map first to ensure visible players get updated
                            PlayerMP targetPlayer = gameScene.getMap().getPlayer(username);
                            
                            // If not found in current map, check specific maps as fallback
                            if (targetPlayer == null) {
                                targetPlayer = gameScene.getLobbyMap().getPlayer(username);
                            }
                            if (targetPlayer == null) {
                                targetPlayer = gameScene.getMonsterHuntMap().getPlayer(username);
                            }
                            if (targetPlayer == null) {
                                targetPlayer = gameScene.getMazeMap().getPlayer(username);
                            }
                            
                            if (targetPlayer != null) {
                                targetPlayer.changeSkin(skinFolder);
                                System.out.println("Changed skin for " + username + " to folder " + skinFolder);
                            }
                        } else {
                            // Đổi skin cho chính mình
                            gameScene.getPlayer().changeSkin(skinFolder);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    public void stopThread() {
        isRunning = false;
    }
}
