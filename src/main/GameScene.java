package main;

import collision.Collision;
import maps.MazeMap;
import maps.MonsterHuntMap;
import network.client.Client;
import network.client.Protocol;
import network.entitiesNet.PlayerMP;
import network.leaderBoard.LeaderBoard;
import objects.entities.Player;
import input.KeyHandler;
import input.MouseHandler;
import maps.Map;
import panes.chat.ChatPane;
import panes.loading.LoadingPane;
import panes.shop.ShopPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * GameScene class is the main class that handles the game logic and rendering of the game.
 * It extends JPanel and implements Runnable.
 */
public class GameScene extends JPanel implements Runnable {
    //Screen settings
    private final int originalTileSize = 16;
    private final int scale = 3;
    private final int tileSize = originalTileSize * scale;
    private final int maxTilesX = 22;
    private final int maxTilesY = 16;
    private final int screenWidth = maxTilesX * tileSize;
    private final int screenHeight = maxTilesY * tileSize;

    //Map
    public String currentMap = "lobby";
    private MonsterHuntMap monsterHuntMap;
    private Map map;
    private MazeMap mazeMap;

    //Player
    private boolean isPlayerAlive = true;

    //Scene
    private final JPanel loadingPanel;

    //NPC
    CustomButton teleportButtonMonsterHunt;
    CustomButton topButton;
    CustomButton teleportButtonMazeNPC;
    CustomButton shopButton;
    
    // Shop
    private ShopPane shopPane;

    private int fps = 0;

    private Thread gameThread;

    private KeyHandler keyHandler;
    private MouseHandler mouseHandler;
    private Player player;

    //Network
    private PlayerMP playerMP;
    private ArrayList<PlayerMP> players;

    //Leaderboard
    private LeaderBoard leaderBoard;

    //Game settings
    private boolean isRunning;

    //Handler for the collision
    private Collision collision;

    //Handler for the game scene

    //Chat
    private ChatPane chatPanel;

    private static GameScene instance;

    public static GameScene getInstance() {
        if (instance == null) {
            instance = new GameScene(true);
        }
        return instance;
    }

    public GameScene(boolean isRunning) {
        keyHandler = new KeyHandler();
        mouseHandler = new MouseHandler();
        this.addKeyListener(keyHandler);
        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);

        collision = new Collision(this);

        this.isRunning = isRunning;

        player = new Player(this, keyHandler);

        playerMP = new PlayerMP(player);

        //Maps
        map = new Map(this);
        monsterHuntMap = new MonsterHuntMap(this);
        mazeMap = new MazeMap(this);

        //Loading
        loadingPanel = new LoadingPane();

        players = map.players;

        repaint();

        init();

        //Chat
        chatPanel = new ChatPane(this);

        chatPanel.setBackground(Color.WHITE);
        chatPanel.setPreferredSize(new Dimension(200, 100));

        CustomButton chatButton = new CustomButton("Chat");
        chatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chatPanel.setVisible(!chatPanel.isVisible());
                requestFocusInWindow();
            }
        });

        //Leaderboard

        leaderBoard = new LeaderBoard();

        topButton = new CustomButton("Top");
        topButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leaderBoard.setVisible(!leaderBoard.isVisible());
                requestFocusInWindow();
            }
        });

        teleportButtonMonsterHunt = new CustomButton("Hunt!");

        teleportButtonMonsterHunt.setBounds((screenWidth + 100) / 2, screenHeight - 100, 100, 50);
        teleportButtonMonsterHunt.setVisible(false);
        teleportButtonMonsterHunt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (currentMap) {
                    case "lobby":
                        // Spawn player in center of Monster Hunt playable area
                        int centerX = 24 * 48; // Tile 24
                        int centerY = 24 * 48;
                        player.setWorldX(centerX);
                        player.setWorldY(centerY);
                        currentMap = "hunt"; // Changed from "pvp" to "hunt"
                        map.removeAllPlayers();

                        playerMP.setAlive(true);
                        sendRespawnPacket();
                        Client.getGameClient().sendToServer(new Protocol().teleportPacket(playerMP.getUsername(), currentMap, player.getWorldX(), player.getWorldY()));
                        
                        // Reset game state - wait for SPACE to start
                        monsterHuntMap.resetGame();
                        break;
                    case "hunt": // Changed from "pvp" to "hunt"
                        player.setDefaultPosition();
                        isPlayerAlive = true;
                        currentMap = "lobby";
                        
                        // End Score Battle and send final score with kills
                        int finalScore = monsterHuntMap.getLocalPlayerScore();
                        int totalKills = monsterHuntMap.getTotalKills();
                        Client.getGameClient().sendToServer(new Protocol().scoreBattleEndPacket(playerMP.getUsername(), finalScore, totalKills));
                        monsterHuntMap.resetGame();
                        monsterHuntMap.removeAllPlayers();

                        Client.getGameClient().sendToServer(new Protocol().teleportPacket(playerMP.getUsername(), currentMap, player.getWorldX(), player.getWorldY()));
                        break;
                }
                requestFocusInWindow();
            }
        });

        teleportButtonMazeNPC = new CustomButton("Teleport");

        teleportButtonMonsterHunt.setVisible(true);

        teleportButtonMazeNPC.setBounds((screenWidth + 100) / 2, screenHeight - 100, 100, 50);
        teleportButtonMazeNPC.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (currentMap) {
                    case "lobby":
                        changeToLoadingScene();
                        currentMap = "loading";
                        map.removeAllPlayers(); // Clear lobby players so we don't update them instead of maze players

                        Client.getGameClient().sendToServer(new Protocol().enterMazePacket(playerMP.getUsername()));
                        break;
                    case "maze":
                        player.setWorldX(1693);
                        player.setWorldY(535);
                        currentMap = "lobby";

                        map.getMazeNPC().setWorldX(2092);
                        map.getMazeNPC().setWorldY(1075);
                        mazeMap.removeAllPlayers();

                        Client.getGameClient().sendToServer(new Protocol().teleportPacket(playerMP.getUsername(), currentMap, player.getWorldX(), player.getWorldY()));
                        break;
                }
                requestFocusInWindow();
            }
        });

        add(teleportButtonMonsterHunt);

        setLayout(null);

        chatPanel.setSize(400, 100);

        int centerXChat = (screenWidth - chatPanel.getWidth()) / 2;
        int centerYChat = (screenHeight - chatPanel.getHeight()) / 2;

        chatPanel.setLocation(centerXChat, screenHeight - 120);

        leaderBoard.setSize(300, 400);
        int centerXLeaderBoard = (screenWidth - leaderBoard.getWidth()) / 2;
        int centerYLeaderBoard = (screenHeight - leaderBoard.getHeight()) / 2;
        leaderBoard.setLocation(centerXLeaderBoard, centerYLeaderBoard);

        leaderBoard.setVisible(false);

        add(leaderBoard);

        chatButton.setBounds(screenWidth - 70, 20, 60, 35);

        topButton.setBounds((screenWidth - 100) / 2, screenHeight - 100, 100, 50);

        add(topButton);

        chatPanel.setVisible(false);

        add(chatPanel);

        add(chatButton);

        add(teleportButtonMazeNPC);
        
        // Shop button and panel
        shopButton = new CustomButton("Shop");
        shopButton.setBounds(screenWidth - 140, 20, 60, 35);
        shopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (shopPane == null) {
                    shopPane = new ShopPane(Client.getGameClient().getWebSocketClient());
                    shopPane.setSize(550, 450);
                    int centerX = (screenWidth - shopPane.getWidth()) / 2;
                    int centerY = (screenHeight - shopPane.getHeight()) / 2;
                    shopPane.setLocation(centerX, centerY);
                    shopPane.setVisible(false);
                    
                    // Listener for skin change
                    shopPane.setSkinChangeListener(skinFolder -> {
                        player.changeSkin(skinFolder);
                    });
                    
                    add(shopPane, 0);
                }
                shopPane.setVisible(!shopPane.isVisible());
                if (shopPane.isVisible()) {
                    if (Client.getGameClient() != null) {
                        Client.getGameClient().sendToServer(new Protocol().getSkinsPacket());
                        Client.getGameClient().sendToServer(new Protocol().getCoinsPacket());
                    }
                }
                requestFocusInWindow();
            }
        });
        add(shopButton);

        init();
    }

    private void init() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setFocusable(true);
        this.requestFocus();
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.setFocusable(true);
    }

    public synchronized void start() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        //FPS - Optimized
        final double TARGET_FPS = 60.0; // Reduced from 90 for better performance
        final double TARGET_TIME = 1000000000.0 / TARGET_FPS;
        long lastTime = System.nanoTime();
        long currentTime;
        double delta = 0.0;
        
        // FPS counter
        long fpsTimer = 0;
        int frameCount = 0;

        System.out.println("Game started");
        while (gameThread != null) {
            currentTime = System.nanoTime();
            long elapsed = currentTime - lastTime;
            
            delta += elapsed / TARGET_TIME;
            fpsTimer += elapsed;
            lastTime = currentTime;

            if (delta >= 1.0) {
                update();
                repaint();
                delta--;
                frameCount++;
            }
            
            // Update FPS every second
            if (fpsTimer >= 1000000000) {
                fps = frameCount;
                frameCount = 0;
                fpsTimer = 0;
            }
            
            // Small sleep to prevent CPU spinning
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void update() {
        player.update();
        playerMP.update();

        // Update remote players for interpolation
        if (getMap() != null && getMap().players != null) {
            // Use a copy or index loop to avoid ConcurrentModificationException
            for (int i = 0; i < getMap().players.size(); i++) {
                PlayerMP p = getMap().players.get(i);
                if (p != null) {
                    p.update();
                }
            }
        }

        // Update Monster Hunt map game logic if in hunt mode
        if (currentMap.equals("hunt")) {
            // Press SPACE to start/restart game when not started or ended
            if (!monsterHuntMap.isGameStarted() || monsterHuntMap.isGameEnded()) {
                if (keyHandler.isSpace()) {
                    monsterHuntMap.startGame();
                    Client.getGameClient().sendToServer(new Protocol().startScoreBattlePacket(playerMP.getUsername()));
                }
            } else {
                monsterHuntMap.update();
            }
        }
        
        // Update Maze map game logic if in maze mode
        if (currentMap.equals("maze")) {
            mazeMap.update(player);
            
            // Handle game over or win - press SPACE to return to lobby
            if (mazeMap.canRestart() && keyHandler.isSpace()) {
                if (mazeMap.isGameWon()) {
                    returnToLobbyFromMaze();
                } else {
                    // Game over - return to lobby without bonus points
                    map.getMazeNPC().setWorldX(2092);
                    map.getMazeNPC().setWorldY(1075);
                    mazeMap.removeAllPlayers();
                    changeToLobby(mazeMap);
                }
            }
        }

        // Optimize NPC button visibility checks
        boolean monsterHuntNPCNear = map.getMonsterHuntNPC().isPlayerNear(player);
        if (teleportButtonMonsterHunt.isVisible() != monsterHuntNPCNear) {
            teleportButtonMonsterHunt.setVisible(monsterHuntNPCNear);
        }

        boolean topNPCNear = map.getTopNPC().isPlayerNear(player);
        if (topButton.isVisible() != topNPCNear) {
            topButton.setVisible(topNPCNear);
            if (!topNPCNear && leaderBoard.isVisible()) {
                leaderBoard.setVisible(false);
            }
        }

        boolean mazeNPCNear = map.getMazeNPC().isPlayerNear(player);
        if (teleportButtonMazeNPC.isVisible() != mazeNPCNear) {
            teleportButtonMazeNPC.setVisible(mazeNPCNear);
        }
    }

    public int drawChat = 0;

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        isRunning = true;

        if (isRunning) {
            switch (currentMap) {
                case "lobby":
                    map.draw(g2d, tileSize);
                    break;
                case "hunt": // Changed from "pvp" to "hunt"
                    monsterHuntMap.draw(g2d, tileSize);
                    break;
                case "maze":
                    mazeMap.draw(g2d, tileSize);
            }

            playerMP.render(g2d, tileSize);

            // Render bullets for current player with new rendering
            for (objects.entities.Bullet bullet : playerMP.getBullets()) {
                if (bullet != null && !bullet.stop) {
                    int bombScreenX = bullet.getPosiX() - player.getWorldX() + player.getScreenX();
                    int bombScreenY = bullet.getPosiY() - player.getWorldY() + player.getScreenY();
                    // Use new render method for better visuals
                    bullet.render(g2d, bombScreenX, bombScreenY);
                }
            }

            // Render bullets for all players in Monster Hunt (optimized)
            if (currentMap.equals("hunt")) {
                for (PlayerMP otherPlayer : getMap().players) {
                    if (otherPlayer == null) continue;
                    
                    for (objects.entities.Bullet bullet : otherPlayer.getBullets()) {
                        if (bullet != null && !bullet.stop) {
                            int bulletScreenX = bullet.getPosiX() - player.getWorldX() + player.getScreenX();
                            int bulletScreenY = bullet.getPosiY() - player.getWorldY() + player.getScreenY();
                            // Use new render method for better visuals
                            bullet.render(g2d, bulletScreenX, bulletScreenY);
                        }
                    }
                }
            }

        }
    }

    public void sendTeleportPacket(String username, String map, int x, int y) {
        Client.getGameClient().sendToServer(new Protocol().teleportPacket(username, map, x, y));
    }

    public void changeToLoadingScene() {
        loadingPanel.setSize(screenWidth, screenHeight);
        add(loadingPanel);

    }

    public void changeToMazeMap() {
        remove(loadingPanel);
        currentMap = "maze";
        
        // Start maze mode with enemies and traps
        mazeMap.startMazeMode();
    }

    public void changeToLobby(Map map) {
        // Stop maze mode if leaving from maze
        if (currentMap.equals("maze")) {
            mazeMap.stopMazeMode();
        }
        
        player.setDefaultPosition();

        currentMap = "lobby";

        map.removeAllPlayers();
    }

    public void registerNewPlayer(PlayerMP newPlayer) {
        getMap().addPlayer(newPlayer);
    }

    public void teleportPlayer(String username, String map, int x, int y) {
        System.out.println(username);

        PlayerMP player = getMap().getPlayer(username);
        if (player != null) {
            player.setX(x);
            player.setY(y);
            player.getPlayer().setWorldX(x);
            player.getPlayer().setWorldY(y);

            if (!currentMap.equals(map)) {
                System.out.println("Teleporting player" + map);
                getMap().removePlayer(username);
            }
        }
    }

    public void winMaze() {
        // Calculate and display win screen with score
        mazeMap.handleWin();
        
        // Wait for win screen to show before returning to lobby
        // The actual return to lobby will be triggered by SPACE key
    }
    
    /**
     * Called when player presses SPACE after winning maze
     */
    public void returnToLobbyFromMaze() {
        int finalScore = mazeMap.getTotalScore();
        
        map.getMazeNPC().setWorldX(2092);
        map.getMazeNPC().setWorldY(1075);
        mazeMap.removeAllPlayers();
        changeToLobby(mazeMap);

        // Win packet is already sent by MazeMap.handleWin() -> sendScoreToServer()
        // Client.getGameClient().sendToServer(new Protocol().winMazePacket(playerMP.getUsername()));
    }

    public void sendRespawnPacket() {
        Client.getGameClient().sendToServer(new Protocol().respawnPacket(playerMP.getUsername()));
    }

    public void removePlayer(String username) {
        map.removePlayer(username);
    }

    public PlayerMP getPlayer(String username) {
        for (PlayerMP mp : players) {
            if (mp != null && mp.getUsername().equals(username)) {
                return mp;
            }
        }
        return null;
    }

    //Getters and Setters
    public int getMaxTilesX() {
        return maxTilesX;
    }

    public int getMaxTilesY() {
        return maxTilesY;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getTileSize() {
        return tileSize;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerMP getPlayerMP() {
        return playerMP;
    }
    
    public MouseHandler getMouseHandler() {
        return mouseHandler;
    }

    public void setPlayerMP(PlayerMP playerMP) {
        this.playerMP = playerMP;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public Map getMap() {
        return switch (currentMap) {
            case "hunt" -> monsterHuntMap; // Changed from "pvp" to "hunt"
            case "maze" -> mazeMap;
            default -> map;
        };
    }

    public Map getLobbyMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public Collision getCollisionChecker() {
        return collision;
    }

    public void setCollisionChecker(Collision collision) {
        this.collision = collision;
    }

    public ChatPane getChatPanel() {
        return chatPanel;
    }

    public void setChatPanel(ChatPane chatPanel) {
        this.chatPanel = chatPanel;
    }

    public LeaderBoard getLeaderBoard() {
        return leaderBoard;
    }

    public void setLeaderBoard(LeaderBoard leaderBoard) {
        this.leaderBoard = leaderBoard;
    }

    public String getCurrentMap() {
        return currentMap;
    }

    public void setCurrentMap(String currentMap) {
        this.currentMap = currentMap;
    }

    public ArrayList<PlayerMP> getPlayers() {
        return monsterHuntMap.players;
    }

    public MazeMap getMazeMap() {
        return mazeMap;
    }

    public void setMazeMap(MazeMap mazeMap) {
        this.mazeMap = mazeMap;
    }

    public MonsterHuntMap getMonsterHuntMap() {
        return monsterHuntMap;
    }

    public void setMonsterHuntMap(MonsterHuntMap monsterHuntMap) {
        this.monsterHuntMap = monsterHuntMap;
    }

    public boolean isPlayerAlive() {
        return isPlayerAlive;
    }

    public void setPlayerAlive(boolean isPlayerAlive) {
        this.isPlayerAlive = isPlayerAlive;
    }
    
    public ShopPane getShopPane() {
        return shopPane;
    }
    
    public void setShopPane(ShopPane shopPane) {
        this.shopPane = shopPane;
    }
}
