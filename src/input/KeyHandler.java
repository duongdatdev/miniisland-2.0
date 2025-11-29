package input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {
    private boolean up, down, left, right, space;
    private boolean shift; // For dash
    private boolean qKey;  // For cycling bullet types
    private boolean eKey;  // For interact/special action
    
    // Arrow keys for 8-directional aiming (separate from movement)
    private boolean aimUp, aimDown, aimLeft, aimRight;

    public KeyHandler() {
        up = false;
        down = false;
        left = false;
        right = false;
        space = false;
        shift = false;
        qKey = false;
        eKey = false;
        aimUp = false;
        aimDown = false;
        aimLeft = false;
        aimRight = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_W -> up = true;
            case KeyEvent.VK_A -> left = true;
            case KeyEvent.VK_S -> down = true;
            case KeyEvent.VK_D -> right = true;
            case KeyEvent.VK_SPACE -> space = true;
            case KeyEvent.VK_SHIFT -> shift = true;
            case KeyEvent.VK_Q -> qKey = true;
            case KeyEvent.VK_E -> eKey = true;
            // Arrow keys for aiming
            case KeyEvent.VK_UP -> aimUp = true;
            case KeyEvent.VK_DOWN -> aimDown = true;
            case KeyEvent.VK_LEFT -> aimLeft = true;
            case KeyEvent.VK_RIGHT -> aimRight = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_W -> up = false;
            case KeyEvent.VK_S -> down = false;
            case KeyEvent.VK_A -> left = false;
            case KeyEvent.VK_D -> right = false;
            case KeyEvent.VK_SPACE -> space = false;
            case KeyEvent.VK_SHIFT -> shift = false;
            case KeyEvent.VK_Q -> qKey = false;
            case KeyEvent.VK_E -> eKey = false;
            // Arrow keys for aiming
            case KeyEvent.VK_UP -> aimUp = false;
            case KeyEvent.VK_DOWN -> aimDown = false;
            case KeyEvent.VK_LEFT -> aimLeft = false;
            case KeyEvent.VK_RIGHT -> aimRight = false;
        }
    }

    public void reset() {
        up = false;
        down = false;
        left = false;
        right = false;
        space = false;
        shift = false;
        qKey = false;
        eKey = false;
        aimUp = false;
        aimDown = false;
        aimLeft = false;
        aimRight = false;
    }

    public boolean isUp() {
        return up;
    }

    public boolean isDown() {
        return down;
    }

    public boolean isLeft() {
        return left;
    }

    public boolean isRight() {
        return right;
    }

    public boolean isSpace() {
        return space;
    }
    
    public boolean isShift() {
        return shift;
    }
    
    public boolean isQKey() {
        return qKey;
    }
    
    public boolean isEKey() {
        return eKey;
    }
    
    // Aim direction getters
    public boolean isAimUp() {
        return aimUp;
    }
    
    public boolean isAimDown() {
        return aimDown;
    }
    
    public boolean isAimLeft() {
        return aimLeft;
    }
    
    public boolean isAimRight() {
        return aimRight;
    }
    
    /**
     * Check if any aim key is pressed
     */
    public boolean isAiming() {
        return aimUp || aimDown || aimLeft || aimRight;
    }
    
    /**
     * Get horizontal aim direction (-1, 0, 1)
     */
    public int getAimDirectionX() {
        int dir = 0;
        if (aimLeft) dir -= 1;
        if (aimRight) dir += 1;
        return dir;
    }
    
    /**
     * Get vertical aim direction (-1, 0, 1)
     */
    public int getAimDirectionY() {
        int dir = 0;
        if (aimUp) dir -= 1;
        if (aimDown) dir += 1;
        return dir;
    }

    public void setSpace(boolean space) {
        this.space = space;
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public void setDown(boolean down) {
        this.down = down;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public void setRight(boolean right) {
        this.right = right;
    }
    
    public void setShift(boolean shift) {
        this.shift = shift;
    }
    
    public void setQKey(boolean qKey) {
        this.qKey = qKey;
    }
}
