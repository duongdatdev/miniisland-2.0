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
    protected GameScene gameScene;
    protected int mapTileCol = 70;
    protected int mapTileRow = 50;

    public ArrayList<PlayerMP> players;
    public PlayerMP player;

    private Entity[] npcs;
    private NPC pvpNPC;
    private NPC topNPC;
    private NPC mazeNPC;

    protected boolean render = true;

    public Map(GameScene gameScene) {
        this.gameScene = gameScene;
        players = new ArrayList<PlayerMP>();
        npcs = new Entity[2];
        mapTileNum = new int[mapTileCol][mapTileRow];

        player = gameScene.getPlayerMP();

        try {
            pvpNPC = new NPC("PvP", 1000, 1000, ImageIO.read(getClass().getResource("/Maps/Pvp/PvpNPC.png")), gameScene.getTileSize());
            topNPC = new NPC("Top 20", 1693, 535, ImageIO.read(getClass().getResource("/NPC/top20NPC.png")), gameScene.getTileSize());
            mazeNPC = new NPC("Maze", 2092, 1075, ImageIO.read(getClass().getResource("/Maps/Maze/mazeNPC.png")), gameScene.getTileSize());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        npcs[0] = pvpNPC;
        npcs[0] = topNPC;
        npcs[1] = mazeNPC;

        setHitBox();

//        loadMap("/Maps/Map_tiles.png");
        loadMap("/Maps/tileSet.png");
        readMap("/Maps/map_1.csv");
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
        if (i == 3) {
            tiles[i].setType(TileType.Grass);
        } else if (i == 153) {
            tiles[i].setType(TileType.Water);
        } else if (i == 78) {
            tiles[i].setType(TileType.Wall);
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
        
        // Draw only visible tiles
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
        
        for (PlayerMP playerMP : players) {
            if (playerMP == null) continue;
            
            int worldX = playerMP.getX();
            int worldY = playerMP.getY();
            
            // Quick bounds check
            if (Math.abs(worldX - playerWorldX) > playerScreenX + tileSize * 2 ||
                Math.abs(worldY - playerWorldY) > playerScreenY + tileSize * 2) {
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
    }

    public void setNPCLocation(){
        pvpNPC.setWorldX(1000);
        pvpNPC.setWorldY(1000);
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
        pvpNPC.checkDraw(player.getPlayer(), g2d);
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
                    System.out.print(mapTileNum[col][row] + " ");
                    col++;
                }
                if (col == mapTileCol) {
                    System.out.println();
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

    public void addPlayer(PlayerMP player) {
        players.add(player);
    }

    public PlayerMP getPlayer(String username) {
        try {
            for (PlayerMP mp : players) {
                if (mp != null && mp.getUsername().equals(username)) {
                    return mp;
                }
            }
        } catch (Exception e) {
            System.out.println("Player not found");
        }
        return null;
    }

    public void removePlayer(String username) {
        for (PlayerMP mp : players) {
            if (mp.getUsername().equals(username)) {
                System.out.println("Player " + mp.getUsername() + " has left the lobby.");
                players.remove(mp);
                break;
            }
        }
    }

    public void removeAllPlayers() {
        players.clear();
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

    public NPC getPvpNPC() {
        return pvpNPC;
    }

    public void setPvpNPC(NPC pvpNPC) {
        this.pvpNPC = pvpNPC;
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
}
