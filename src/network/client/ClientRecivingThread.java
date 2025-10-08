package network.client;

import main.GameScene;
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

                } else if (sentence.startsWith("Shot")) {

                    String username = sentence.substring(4);

                    if (username.equals(clientPlayer.getUsername())) {
//                        gameScene.getPlayerMP().shot();
                    } else {
                        System.out.println("Player " + username + " shot");

                        if (gameScene.getCurrentMap().equals("pvp")){
                                gameScene.getPvpMap().getPlayer(username).Shot();
                        }
                    }
                } else if (sentence.startsWith("BulletCollision")) {
                    String[] parts = sentence.split(",");

                    String playerShot = parts[1];
                    String playerHit = parts[2];

                    if (playerHit.equals(clientPlayer.getUsername())) {
                        int response = JOptionPane.showConfirmDialog(null, "Sorry, You are loss. Do you want to try again ?", "2D Multiplayer Game", JOptionPane.OK_CANCEL_OPTION);
                        if (response == JOptionPane.OK_OPTION) {
                            gameScene.setPlayerAlive(true);
                            gameScene.getPlayer().setWorldX(1000);
                            gameScene.getPlayer().setWorldY(1000);

                            gameScene.getPlayerMP().setAlive(true);
                            gameScene.sendRespawnPacket();
                            gameScene.getPvpMap().removeAllPlayers();
                            gameScene.sendTeleportPacket(gameScene.getPlayerMP().getUsername(), "pvp", gameScene.getPlayer().getWorldX(), gameScene.getPlayer().getWorldY());
                        } else {
                            gameScene.setPlayerAlive(true);
                            gameScene.getPlayer().setDefaultPosition();
                            gameScene.sendTeleportPacket(gameScene.getPlayerMP().getUsername(), "lobby", gameScene.getPlayer().getWorldX(), gameScene.getPlayer().getWorldY());
                            gameScene.changeToLobby(gameScene.getPvpMap());
                        }
                    } else {
                        for (PlayerMP player : gameScene.getMap().players) {
                            if (player.getUsername().equals(playerHit)) {
                                gameScene.getMap().removePlayer(playerHit);
                                break;
                            }
                        }
                    }

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

                        BufferedImage chatImage = gameScene.getMap().getPlayer(username).getDialogText().loadImage(message);
                        gameScene.getMap().getPlayer(username).setChatImage(chatImage);

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    public void stopThread() {
        isRunning = false;
    }
}
