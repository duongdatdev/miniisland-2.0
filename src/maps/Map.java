package maps;

import imageRender.ImageHandler;
import main.GameScene;
import network.entitiesNet.PlayerMP;
import objects.entities.Entity;
import objects.entities.NPC;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

public class Map {
    protected Tile[] tiles;
    protected int width = 16;
    protected int height = 16;
    protected BufferedImage[] tileSet;
    protected int[][] mapTileNum;
    protected int[][] mapTileNumLayer2; // Layer 2 for bridges
    protected GameScene gameScene;
    protected int mapTileCol = 70;
    protected int mapTileRow = 50;

    // Use CopyOnWriteArrayList to prevent ConcurrentModificationException during rendering
    public java.util.concurrent.CopyOnWriteArrayList<PlayerMP> players;
    public PlayerMP player;

    private Entity[] npcs;
    private NPC monsterHuntNPC; // Renamed from pvpNPC
    private NPC topNPC;
    private NPC mazeNPC;

    protected boolean render = true;

    public Map(GameScene gameScene) {
        this.gameScene = gameScene;
        players = new java.util.concurrent.CopyOnWriteArrayList<PlayerMP>();
        npcs = new Entity[2];
        mapTileNum = new int[mapTileCol][mapTileRow];
        mapTileNumLayer2 = new int[mapTileCol][mapTileRow]; // Initialize layer 2

        player = gameScene.getPlayerMP();

        try {
            monsterHuntNPC = new NPC("Monster Hunt", 1000, 1000, ImageIO.read(getClass().getResource("/Maps/Pvp/PvpNPC.png")), gameScene.getTileSize());
            topNPC = new NPC("Top 20", 1693, 535, ImageIO.read(getClass().getResource("/NPC/top20NPC.png")), gameScene.getTileSize());
            mazeNPC = new NPC("Maze", 2092, 1075, ImageIO.read(getClass().getResource("/Maps/Maze/mazeNPC.png")), gameScene.getTileSize());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        npcs[0] = topNPC;
        npcs[1] = mazeNPC;

        setHitBox();

//        loadMap("/Maps/Map_tiles.png");
        loadMap("/Maps/tileSet.png");
        readMap("/Maps/map_1.csv");
        readMapLayer2("/Maps/map_1_Layer 2.csv"); // Read layer 2
    }

    public void addPlayer(PlayerMP player) {
        // Prevent duplicates: Remove existing player with same username if present
        for (PlayerMP p : players) {
            if (p.getUsername().equals(player.getUsername())) {
                players.remove(p);
                break;
            }
        }
        players.add(player);
        System.out.println("Map: Added player " + player.getUsername() + " (Total: " + players.size() + ")");
    }

    public PlayerMP getPlayer(String username) {
        for (PlayerMP mp : players) {
            if (mp != null && mp.getUsername().equals(username)) {
                return mp;
            }
        }
        return null;
    }

    public void removePlayer(String username) {
        for (PlayerMP mp : players) {
            if (mp.getUsername().equals(username)) {
                players.remove(mp);
                System.out.println("Player " + mp.getUsername() + " has left the lobby.");
                break;
            }
        }
    }

    public void removeAllPlayers() {
        players.clear();
    }

    public void setHitBox() {
        for (Entity npc : npcs) {
            Rectangle hitBox = new Rectangle(0, 0, gameScene.getTileSize(), gameScene.getTileSize());
            npc.setHitBox(hitBox);
        }
    }

    public void loadMap(String mapPath) {
        tileSet = ImageHandler.loadAssets(mapPath, width, height);
        tiles = new Tile[tileSet.length];
        for (int i = 0; i < tileSet.length; i++) {

            tiles[i] = new Tile();

            tiles[i].setImage(tileSet[i]);

            /* Tile num
             * 9 -> water
             */
            setTileType(i);
        }
    }

    public void setTileType(int i) {
        // Bridge tiles - ONLY the middle part of bridges is walkable
        // Vertical bridge: 168, 185 (middle)
        // Horizontal bridge: 165, 182, 199 (middle)
        // Intersections: 113, 114
        if (i == 168 || i == 185 ||  // Vertical bridge middle
            i == 165 || i == 182 || i == 199 ||  // Horizontal bridge middle
            i == 113 || i == 114) {  // Bridge intersections
            tiles[i].setType(TileType.Bridge);
        } 
        // Bridge railings/edges - block movement
        else if (i == 167 || i == 169 ||  // Vertical bridge left/right rails
                 i == 184 || i == 186 ||  // Vertical bridge left/right rails (middle section)
                 i == 201 || i == 202 || i == 203 ||  // Vertical bridge bottom
                 i == 164 || i == 166 ||  // Horizontal bridge top corners
                 i == 181 || i == 183 ||  // Horizontal bridge middle rails
                 i == 198 || i == 200) {  // Horizontal bridge bottom corners
            tiles[i].setType(TileType.Wall);
        } else if (i == 3) {
            tiles[i].setType(TileType.Grass);
        } else if (i == 153) {
            tiles[i].setType(TileType.Water);
        } else if (i == 65 || i == 66 || i == 67 ||  // Top cliff edge corners and edge
                   i == 78 || 
                   i == 82 || i == 84 ||  // Left/right cliff edges
                   i == 96 || i == 97 || i == 98 ||  // Inner corners
                   i == 99 || i == 100 || i == 101 ||  // Bottom cliff edges
                   i == 107 || i == 108 ||  // More cliff edges
                   i == 115 ||  // Inner corner variants (removed 113, 114 - they are bridge)
                   i == 124 || i == 125 ||  // Cliff corners
                   i == 39 || i == 40 ||  // Grass-cliff transition edges
                   i == 62 || i == 63 || i == 64 ||  // Top grass edges
                   i == 79 || i == 81) {  // Left/right grass edges
            // Wall tiles - includes cliffs/brown edges that block movement
            tiles[i].setType(TileType.Wall);
        } else {
            // Set default type for all other tiles (safe to walk on)
            tiles[i].setType(TileType.Grass);
        }
    }

    public void draw(Graphics2D g2d, int tileSize) {
        if (!render) return;
        
        // Cache player position for performance
        int playerWorldX = gameScene.getPlayer().getWorldX();
        int playerWorldY = gameScene.getPlayer().getWorldY();
        int playerScreenX = gameScene.getPlayer().getScreenX();
        int playerScreenY = gameScene.getPlayer().getScreenY();
        
        // Calculate visible tile range (optimized culling)
        int startCol = Math.max(0, (playerWorldX - playerScreenX) / tileSize - 1);
        int endCol = Math.min(mapTileCol, (playerWorldX + playerScreenX) / tileSize + 2);
        int startRow = Math.max(0, (playerWorldY - playerScreenY) / tileSize - 1);
        int endRow = Math.min(mapTileRow, (playerWorldY + playerScreenY) / tileSize + 2);
        
        // Draw Layer 1 - Base tiles
        for (int worldRow = startRow; worldRow < endRow; worldRow++) {
            for (int worldCol = startCol; worldCol < endCol; worldCol++) {
                int tileNum = mapTileNum[worldCol][worldRow];
                
                int worldX = worldCol * tileSize;
                int worldY = worldRow * tileSize;
                
                int screenX = worldX - playerWorldX + playerScreenX;
                int screenY = worldY - playerWorldY + playerScreenY;
                
                g2d.drawImage(tiles[tileNum].getImage(), screenX, screenY, tileSize, tileSize, null);
            }
        }

        renderNPC(g2d);

        // Optimize player rendering
        Font usernameFont = new Font("Arial", Font.BOLD, 20);
        g2d.setFont(usernameFont);
        FontMetrics fm = g2d.getFontMetrics(usernameFont);
        
        // DEBUG: Print player count and positions periodically
        // if (gameScene.getFps() % 60 == 0 && render) {
        //      System.out.println("Map Draw: " + players.size() + " players. Local POS: " + playerWorldX + "," + playerWorldY);
        // }

        for (PlayerMP playerMP : players) {
            if (playerMP == null) continue;
            
            int worldX = playerMP.getX();
            int worldY = playerMP.getY();
            
            // if (gameScene.getFps() % 60 == 0 && render) {
            //      System.out.println(" - Render Player: " + playerMP.getUsername() + " at " + worldX + "," + worldY);
            // }

            // Quick bounds check - Expanded slightly to ensure not culled prematurely
            if (Math.abs(worldX - playerWorldX) > playerScreenX + tileSize * 4 ||
                Math.abs(worldY - playerWorldY) > playerScreenY + tileSize * 4) {
                continue;
            }

            int screenX = worldX - playerWorldX + playerScreenX;
            int screenY = worldY - playerWorldY + playerScreenY;

            // Draw player sprite
            g2d.drawImage(playerMP.getPlayer().currentSprite(), screenX, screenY, tileSize, tileSize, null);

            // Draw username (optimized)
            String username = playerMP.getUsername();
            int stringWidth = fm.stringWidth(username);
            int usernameX = screenX + (tileSize - stringWidth) / 2;
            g2d.drawString(username, usernameX, screenY);

            // Draw chat image if present
            BufferedImage chatImage = playerMP.getChatImage();
            if (chatImage != null) {
                g2d.drawImage(chatImage, screenX - 50, screenY - 20 - chatImage.getHeight(), null);
            }
        }
        
        // Draw Layer 2 - Overlay tiles (bridges, etc.) - render on top of players
        drawLayer2(g2d, tileSize, startCol, endCol, startRow, endRow, playerWorldX, playerWorldY, playerScreenX, playerScreenY);
    }
    
    /**
     * Draw Layer 2 tiles (bridges, decorations) on top of everything
     */
    protected void drawLayer2(Graphics2D g2d, int tileSize, int startCol, int endCol, int startRow, int endRow,
                            int playerWorldX, int playerWorldY, int playerScreenX, int playerScreenY) {
        for (int worldRow = startRow; worldRow < endRow; worldRow++) {
            for (int worldCol = startCol; worldCol < endCol; worldCol++) {
                int tileNum = mapTileNumLayer2[worldCol][worldRow];
                
                // Skip empty tiles (-1 or tiles that don't exist)
                if (tileNum < 0 || tileNum >= tiles.length) continue;
                
                int worldX = worldCol * tileSize;
                int worldY = worldRow * tileSize;
                
                int screenX = worldX - playerWorldX + playerScreenX;
                int screenY = worldY - playerWorldY + playerScreenY;
                
                g2d.drawImage(tiles[tileNum].getImage(), screenX, screenY, tileSize, tileSize, null);
            }
        }
    }

    public void setNPCLocation(){
        monsterHuntNPC.setWorldX(1000);
        monsterHuntNPC.setWorldY(1000);
        topNPC.setWorldX(1693);
        topNPC.setWorldY(535);
        mazeNPC.setWorldX(2092);
        mazeNPC.setWorldY(1075);
    }


    /*
     * This method is used to render the NPC on the map
     * @param g2d
     */
    protected void renderNPC(Graphics2D g2d) {
        monsterHuntNPC.checkDraw(player.getPlayer(), g2d);
        topNPC.checkDraw(player.getPlayer(), g2d);
        mazeNPC.checkDraw(player.getPlayer(), g2d);
    }

    public void stopRenderingMap() {
        render = false;
    }

    public void startRenderingMap() {
        render = true;
    }

    public void readMap(String mapPath) {
        InputStream ip = getClass().getResourceAsStream(mapPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(ip));

        int row = 0;
        int col = 0;

        try {
            while (col < mapTileCol && row < mapTileRow) {
                String line = br.readLine();
                while (col < mapTileCol) {
                    String[] numbers = line.split(",");
                    mapTileNum[col][row] = Integer.parseInt(numbers[col]);
                    // System.out.print(mapTileNum[col][row] + " ");
                    col++;
                }
                if (col == mapTileCol) {
                    // System.out.println();
                    col = 0;
                    row++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Read Layer 2 CSV (contains bridges, decorations on top)
     */
    public void readMapLayer2(String mapPath) {
        InputStream ip = getClass().getResourceAsStream(mapPath);
        if (ip == null) {
            System.out.println("Layer 2 not found: " + mapPath);
            // Initialize layer 2 with -1 (no tile)
            for (int col = 0; col < mapTileCol; col++) {
                for (int row = 0; row < mapTileRow; row++) {
                    mapTileNumLayer2[col][row] = -1;
                }
            }
            return;
        }
        
        BufferedReader br = new BufferedReader(new InputStreamReader(ip));

        int row = 0;
        int col = 0;

        try {
            while (row < mapTileRow) {
                String line = br.readLine();
                if (line == null) break;
                
                String[] numbers = line.split(",");
                col = 0;
                while (col < mapTileCol && col < numbers.length) {
                    int tileNum = Integer.parseInt(numbers[col].trim());
                    mapTileNumLayer2[col][row] = tileNum;
                    col++;
                }
                row++;
            }
            // System.out.println("Layer 2 loaded successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    public Tile[] getTiles() {
        return tiles;
    }

    public void setTiles(Tile[] tiles) {
        this.tiles = tiles;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public BufferedImage[] getTileSet() {
        return tileSet;
    }

    public void setTileSet(BufferedImage[] tileSet) {
        this.tileSet = tileSet;
    }

    public int[][] getMapTileNum() {
        return mapTileNum;
    }

    public void setMapTileNum(int[][] mapTileNum) {
        this.mapTileNum = mapTileNum;
    }
    
    public int[][] getMapTileNumLayer2() {
        return mapTileNumLayer2;
    }

    public void setMapTileNumLayer2(int[][] mapTileNumLayer2) {
        this.mapTileNumLayer2 = mapTileNumLayer2;
    }

    public GameScene getGameScene() {
        return gameScene;
    }

    public void setGameScene(GameScene gameScene) {
        this.gameScene = gameScene;
    }

    public int getMapTileCol() {
        return mapTileCol;
    }

    public void setMapTileCol(int mapTileCol) {
        this.mapTileCol = mapTileCol;
    }

    public int getMapTileRow() {
        return mapTileRow;
    }

    public void setMapTileRow(int mapTileRow) {
        this.mapTileRow = mapTileRow;
    }

    public NPC getTopNPC() {
        return topNPC;
    }

    public void setTopNPC(NPC topNPC) {
        this.topNPC = topNPC;
    }

    public NPC getMonsterHuntNPC() {
        return monsterHuntNPC;
    }

    public void setMonsterHuntNPC(NPC monsterHuntNPC) {
        this.monsterHuntNPC = monsterHuntNPC;
    }

    public Entity[] getNpcs() {
        return npcs;
    }

    public void setNpcs(Entity[] npcs) {
        this.npcs = npcs;
    }

    public NPC getMazeNPC() {
        return mazeNPC;
    }

    public void setMazeNPC(NPC mazeNPC) {
        this.mazeNPC = mazeNPC;
    }

    /**
     * DEBUG: Draw collision overlay for tiles
     * Shows different colors for different tile types:
     * - Wall: Red semi-transparent
     * - Water: Blue semi-transparent  
     * - FinishLine: Green semi-transparent
     * - Hole: Yellow semi-transparent
     * - Bridge: Cyan semi-transparent (walkable over water)
     * - Safe tiles: No overlay
     */
    private void drawDebugCollisionTiles(Graphics2D g2d, int tileNum, int screenX, int screenY, int tileSize) {
        // Safety checks to prevent crashes
        if (tiles == null || tileNum < 0 || tileNum >= tiles.length) {
            return;
        }
        
        if (tiles[tileNum] == null) {
            return;
        }
        
        TileType type = tiles[tileNum].getType();
        
        // If type is null, treat as safe tile (no overlay)
        if (type == null) {
            return;
        }
        
        Color debugColor = null;
        
        switch (type) {
            case Wall:
                debugColor = new Color(255, 0, 0, 80); // Red
                break;
            case Water:
                debugColor = new Color(0, 0, 255, 80); // Blue
                break;
            case FinishLine:
                debugColor = new Color(0, 255, 0, 80); // Green
                break;
            case Hole:
                debugColor = new Color(255, 255, 0, 80); // Yellow
                break;
            case Bridge:
                debugColor = new Color(0, 255, 255, 80); // Cyan - walkable bridge
                break;
            default:
                return; // No overlay for safe tiles (Grass, etc.)
        }
        
        if (debugColor != null) {
            g2d.setColor(debugColor);
            g2d.fillRect(screenX, screenY, tileSize, tileSize);
            
            // Draw border
            g2d.setColor(new Color(debugColor.getRed(), debugColor.getGreen(), debugColor.getBlue(), 200));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(screenX, screenY, tileSize, tileSize);
        }
    }
}
