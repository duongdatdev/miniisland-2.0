package imageRender;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class ImageHandler {
    // Image cache to avoid reloading same images
    private static final Map<String, BufferedImage[]> imageCache = new HashMap<>();

    public static BufferedImage[] loadAssets(String imagePath, int tileWidth, int tileHeight) {
        // Check cache first
        String cacheKey = imagePath + "_" + tileWidth + "_" + tileHeight;
        if (imageCache.containsKey(cacheKey)) {
            return imageCache.get(cacheKey);
        }

        try {
            BufferedImage tileMap = ImageIO.read(ImageHandler.class.getResource(imagePath));
            
            if (tileMap == null) {
                System.err.println("[ImageHandler] Failed to load image: " + imagePath);
                return null;
            }

            int rows = tileMap.getHeight() / tileHeight;
            int cols = tileMap.getWidth() / tileWidth;

            BufferedImage[] icons = new BufferedImage[rows * cols];

            // Optimize image loading with hardware acceleration
            GraphicsConfiguration gc = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    BufferedImage subImage = tileMap.getSubimage(
                        col * tileWidth, row * tileHeight, tileWidth, tileHeight
                    );
                    
                    // Convert to compatible image for better performance
                    BufferedImage compatibleImage = gc.createCompatibleImage(
                        tileWidth, tileHeight, subImage.getTransparency()
                    );
                    Graphics2D g2d = compatibleImage.createGraphics();
                    g2d.drawImage(subImage, 0, 0, null);
                    g2d.dispose();
                    
                    icons[row * cols + col] = compatibleImage;
                }
            }
            
            // Cache the result
            imageCache.put(cacheKey, icons);
            
            return icons;
        } catch (Exception e) {
            System.err.println("[ImageHandler] Error loading assets from: " + imagePath);
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Clear image cache to free memory if needed
     */
    public static void clearCache() {
        imageCache.clear();
        System.gc(); // Suggest garbage collection
    }
    
    /**
     * Get cache size for monitoring
     */
    public static int getCacheSize() {
        return imageCache.size();
    }
}
