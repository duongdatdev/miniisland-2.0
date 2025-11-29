package collision;

import main.GameScene;
import maps.TileType;
import objects.entities.Entity;

public class Collision {
    private GameScene gameScene;

    public Collision(GameScene gameScene) {
        this.gameScene = gameScene;
    }

    /**
     * Check collision with tiles
     */
    public void checkTile(Entity entity, Runnable process) {
        int entityLeftWorldX = entity.getWorldX() + entity.getHitBox().x;
        int entityRightWorldX = entity.getWorldX() + entity.getHitBox().x + entity.getHitBox().width;
        int entityTopWorldY = entity.getWorldY() + entity.getHitBox().y;
        int entityBottomWorldY = entity.getWorldY() + entity.getHitBox().y + entity.getHitBox().height;

        int entityLeftCol = entityLeftWorldX / gameScene.getTileSize();
        int entityRightCol = entityRightWorldX / gameScene.getTileSize();
        int entityTopRow = entityTopWorldY / gameScene.getTileSize();
        int entityBottomRow = entityBottomWorldY / gameScene.getTileSize();

        int tileNum1, tileNum2;
        int col1, col2, row1, row2;

        // DEBUG: Uncomment to see collision checking info
        // System.out.println("[Collision] Direction: " + entity.getDirection() + 
        //     " | Pos: (" + entity.getWorldX() + "," + entity.getWorldY() + 
        //     ") | Hitbox: " + entity.getHitBox());
        
        switch (entity.getDirection()) {
            case "UP":
                entityTopRow = (entityTopWorldY - entity.getSpeed()) / gameScene.getTileSize();
                tileNum1 = gameScene.getMap().getMapTileNum()[entityLeftCol][entityTopRow];
                tileNum2 = gameScene.getMap().getMapTileNum()[entityRightCol][entityTopRow];
                col1 = entityLeftCol;
                col2 = entityRightCol;
                row1 = row2 = entityTopRow;
                break;
            case "DOWN":
                entityBottomRow = (entityBottomWorldY + entity.getSpeed()) / gameScene.getTileSize();
                tileNum1 = gameScene.getMap().getMapTileNum()[entityLeftCol][entityBottomRow];
                tileNum2 = gameScene.getMap().getMapTileNum()[entityRightCol][entityBottomRow];
                col1 = entityLeftCol;
                col2 = entityRightCol;
                row1 = row2 = entityBottomRow;
                break;
            case "LEFT":
                entityLeftCol = (entityLeftWorldX - entity.getSpeed()) / gameScene.getTileSize();
                tileNum1 = gameScene.getMap().getMapTileNum()[entityLeftCol][entityTopRow];
                tileNum2 = gameScene.getMap().getMapTileNum()[entityLeftCol][entityBottomRow];
                col1 = col2 = entityLeftCol;
                row1 = entityTopRow;
                row2 = entityBottomRow;
                break;
            case "RIGHT":
                entityRightCol = (entityRightWorldX + entity.getSpeed()) / gameScene.getTileSize();
                tileNum1 = gameScene.getMap().getMapTileNum()[entityRightCol][entityTopRow];
                tileNum2 = gameScene.getMap().getMapTileNum()[entityRightCol][entityBottomRow];
                col1 = col2 = entityRightCol;
                row1 = entityTopRow;
                row2 = entityBottomRow;
                break;
            default:
                throw new IllegalArgumentException("Invalid direction: " + entity.getDirection());
        }

        handleCollision(entity, tileNum1, tileNum2, col1, row1, col2, row2);
        process.run();
    }

    private void handleCollision(Entity entity, int tileNum1, int tileNum2, int col1, int row1, int col2, int row2) {
        // Kiểm tra Layer 2 - nếu có cầu thì cho phép đi qua cả Wall và Water
        boolean hasBridge1 = checkBridgeAtPosition(col1, row1);
        boolean hasBridge2 = checkBridgeAtPosition(col2, row2);
        
        // Nếu CẢ HAI vị trí đều có cầu hoặc không phải obstacle, cho phép đi qua
        // Nếu một trong hai vị trí là obstacle mà KHÔNG có cầu, chặn lại
        boolean tile1Blocked = isTileBlocking(tileNum1) && !hasBridge1;
        boolean tile2Blocked = isTileBlocking(tileNum2) && !hasBridge2;
        
        if (tile1Blocked || tile2Blocked) {
            // Kiểm tra nếu là Water (không có bridge) thì reset player
            boolean isWater1 = checkTileType(tileNum1, TileType.Water) && !hasBridge1;
            boolean isWater2 = checkTileType(tileNum2, TileType.Water) && !hasBridge2;
            
            if (isWater1 || isWater2) {
                handleCollisionWater(entity);
                entity.setFlagUpdate(false);
            } else {
                // Wall collision - chỉ chặn di chuyển
                entity.setCollision(true);
                entity.setFlagUpdate(false);
            }
        } else if (checkTile(tileNum1, tileNum2, TileType.FinishLine)) {
            gameScene.winMaze();
        } else if (checkTile(tileNum1, tileNum2, TileType.Hole)) {
            // gameScene.loseMaze();
        }
    }
    
    /**
     * Kiểm tra xem tile có chặn di chuyển không (Wall hoặc Water)
     */
    private boolean isTileBlocking(int tileNum) {
        TileType type = gameScene.getMap().getTiles()[tileNum].getType();
        return type == TileType.Wall || type == TileType.Water;
    }
    
    /**
     * Kiểm tra tile có phải loại cụ thể không
     */
    private boolean checkTileType(int tileNum, TileType tileType) {
        return gameScene.getMap().getTiles()[tileNum].getType() == tileType;
    }
    
    /**
     * Kiểm tra xem tại vị trí (col, row) có cầu ở Layer 2 không
     */
    private boolean checkBridgeAtPosition(int col, int row) {
        int[][] layer2 = gameScene.getMap().getMapTileNumLayer2();
        if (layer2 == null) {
            return false;
        }
        
        // Kiểm tra bounds
        if (col < 0 || col >= layer2.length || row < 0 || row >= layer2[0].length) {
            return false;
        }
        
        int layer2Tile = layer2[col][row];
        
        // -1 nghĩa là không có tile
        if (layer2Tile < 0) return false;
        
        // Kiểm tra tile có phải Bridge không
        if (layer2Tile >= gameScene.getMap().getTiles().length) return false;
        
        TileType type = gameScene.getMap().getTiles()[layer2Tile].getType();
        return type == TileType.Bridge;
    }

    private boolean checkTile(int tileNum1, int tileNum2, TileType tileType) {
        return gameScene.getMap().getTiles()[tileNum1].getType() == tileType || gameScene.getMap().getTiles()[tileNum2].getType() == tileType;
    }

    /*
     * Handle collision with water
     */
    private void handleCollisionWater(Entity entity) {
        gameScene.getPlayer().setDefaultPosition();

        gameScene.getPlayerMP().updatePlayerInServer();
    }

    public void checkCollision(Entity entity, Entity[] entities) {
        // Check if the entity collides with any other entity
        for (Entity otherEntity : entities) {

            if (entity.getHitBox().intersects(otherEntity.getHitBox())) {
                entity.setCollision(true);
            }

        }
    }
}
