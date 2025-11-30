package main;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class CustomButton extends JButton {
    public CustomButton(String label) {
        super(label);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setFont(new Font("Arial", Font.BOLD, 12));
        setForeground(new Color(50, 50, 50));
        setBackground(new Color(230, 230, 230));
    }
    
    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int textWidth = fm.stringWidth(getText());
        int textHeight = fm.getHeight();
        // Add padding for text
        return new Dimension(textWidth + 24, textHeight + 16);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Background color based on state
        Color bgColor;
        if (getModel().isPressed()) {
            bgColor = new Color(180, 180, 180);
        } else if (getModel().isRollover()) {
            bgColor = new Color(200, 200, 200);
        } else {
            bgColor = getBackground();
        }
        
        // Draw shadow
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRoundRect(2, 2, getWidth() - 2, getHeight() - 2, 15, 15);
        
        // Draw background
        g2d.setColor(bgColor);
        g2d.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 15, 15);
        
        // Draw border
        g2d.setColor(new Color(100, 100, 100));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 15, 15);
        
        // Draw text centered
        g2d.setFont(getFont());
        g2d.setColor(getForeground());
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(getText())) / 2;
        int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2 - 1;
        g2d.drawString(getText(), textX, textY);
    }

    @Override
    protected void paintBorder(Graphics g) {
        // Border is drawn in paintComponent
    }

    Shape shape;

    @Override
    public boolean contains(int x, int y) {
        if (shape == null || !shape.getBounds().equals(getBounds())) {
            shape = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15);
        }
        return shape.contains(x, y);
    }
}
