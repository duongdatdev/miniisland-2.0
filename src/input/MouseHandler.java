package input;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * MouseHandler - Xử lý input chuột cho game
 * Dùng để nhắm bắn và tương tác trong PvP mode
 */
public class MouseHandler implements MouseListener, MouseMotionListener {
    
    // Mouse position (screen coordinates)
    private int mouseX = 0;
    private int mouseY = 0;
    
    // Mouse buttons
    private boolean leftClick = false;
    private boolean rightClick = false;
    private boolean middleClick = false;
    
    // Hold state for continuous firing
    private boolean leftHeld = false;
    private boolean rightHeld = false;
    
    // For calculating aim angle
    private double aimAngle = 0;
    
    // Screen center (player position on screen)
    private int screenCenterX = 0;
    private int screenCenterY = 0;
    
    public MouseHandler() {
    }
    
    /**
     * Set screen center for angle calculation
     */
    public void setScreenCenter(int x, int y) {
        this.screenCenterX = x;
        this.screenCenterY = y;
    }
    
    /**
     * Update aim angle based on mouse position relative to screen center
     */
    private void updateAimAngle() {
        int dx = mouseX - screenCenterX;
        int dy = mouseY - screenCenterY;
        aimAngle = Math.atan2(dy, dx);
    }
    
    /**
     * Get aim angle in radians
     */
    public double getAimAngle() {
        return aimAngle;
    }
    
    /**
     * Get aim angle in degrees
     */
    public double getAimAngleDegrees() {
        return Math.toDegrees(aimAngle);
    }
    
    /**
     * Get normalized aim direction X (-1 to 1)
     */
    public float getAimDirectionX() {
        return (float) Math.cos(aimAngle);
    }
    
    /**
     * Get normalized aim direction Y (-1 to 1)
     */
    public float getAimDirectionY() {
        return (float) Math.sin(aimAngle);
    }
    
    /**
     * Get 8-direction based on mouse angle
     * Returns: 1=DOWN, 2=UP, 3=LEFT, 4=RIGHT, 5=UP_LEFT, 6=UP_RIGHT, 7=DOWN_LEFT, 8=DOWN_RIGHT
     */
    public int get8Direction() {
        double degrees = getAimAngleDegrees();
        
        // Normalize to 0-360
        if (degrees < 0) degrees += 360;
        
        // 8 directions, each covers 45 degrees
        // Right: -22.5 to 22.5 (or 337.5 to 360 and 0 to 22.5)
        if (degrees >= 337.5 || degrees < 22.5) return 4;  // RIGHT
        if (degrees >= 22.5 && degrees < 67.5) return 8;   // DOWN_RIGHT
        if (degrees >= 67.5 && degrees < 112.5) return 1;  // DOWN
        if (degrees >= 112.5 && degrees < 157.5) return 7; // DOWN_LEFT
        if (degrees >= 157.5 && degrees < 202.5) return 3; // LEFT
        if (degrees >= 202.5 && degrees < 247.5) return 5; // UP_LEFT
        if (degrees >= 247.5 && degrees < 292.5) return 2; // UP
        if (degrees >= 292.5 && degrees < 337.5) return 6; // UP_RIGHT
        
        return 1; // Default DOWN
    }
    
    /**
     * Get 4-direction (cardinal only) based on mouse angle
     */
    public int get4Direction() {
        double degrees = getAimAngleDegrees();
        if (degrees < 0) degrees += 360;
        
        if (degrees >= 315 || degrees < 45) return 4;  // RIGHT
        if (degrees >= 45 && degrees < 135) return 1;  // DOWN
        if (degrees >= 135 && degrees < 225) return 3; // LEFT
        return 2; // UP
    }
    
    // === MouseListener Implementation ===
    
    @Override
    public void mouseClicked(MouseEvent e) {
        // Single click events if needed
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        switch (e.getButton()) {
            case MouseEvent.BUTTON1 -> {
                leftClick = true;
                leftHeld = true;
            }
            case MouseEvent.BUTTON2 -> {
                middleClick = true;
            }
            case MouseEvent.BUTTON3 -> {
                rightClick = true;
                rightHeld = true;
            }
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        switch (e.getButton()) {
            case MouseEvent.BUTTON1 -> {
                leftClick = false;
                leftHeld = false;
            }
            case MouseEvent.BUTTON2 -> {
                middleClick = false;
            }
            case MouseEvent.BUTTON3 -> {
                rightClick = false;
                rightHeld = false;
            }
        }
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
    }
    
    // === MouseMotionListener Implementation ===
    
    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        updateAimAngle();
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        updateAimAngle();
    }
    
    // === Getters ===
    
    public int getMouseX() {
        return mouseX;
    }
    
    public int getMouseY() {
        return mouseY;
    }
    
    public boolean isLeftClick() {
        return leftClick;
    }
    
    public boolean isRightClick() {
        return rightClick;
    }
    
    public boolean isMiddleClick() {
        return middleClick;
    }
    
    public boolean isLeftHeld() {
        return leftHeld;
    }
    
    public boolean isRightHeld() {
        return rightHeld;
    }
    
    /**
     * Consume left click (reset after reading)
     */
    public boolean consumeLeftClick() {
        boolean was = leftClick;
        leftClick = false;
        return was;
    }
    
    /**
     * Consume right click (reset after reading)
     */
    public boolean consumeRightClick() {
        boolean was = rightClick;
        rightClick = false;
        return was;
    }
    
    /**
     * Reset all states
     */
    public void reset() {
        leftClick = false;
        rightClick = false;
        middleClick = false;
        leftHeld = false;
        rightHeld = false;
    }
}
