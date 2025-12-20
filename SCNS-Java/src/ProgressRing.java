import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import javax.swing.JComponent;

public class ProgressRing extends JComponent {
    private int percent = 0;
    private float anim = 0f;

    public ProgressRing() {
        setOpaque(false);
    }

    public void setPercent(int percent) {
        int p = Math.max(0, Math.min(100, percent));
        int start = this.percent;
        this.percent = p;
        Anim.run(520, 60, t -> {
            anim = (float) (start + (p - start) * Anim.easeOutCubic(t));
            repaint();
        }, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Paper background
        g2.setColor(Theme.CARD);
        g2.fillRoundRect(0, 0, w, h, 0, 0);

        int size = Math.min(w, h) - 40;
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        int centerX = w / 2;
        int centerY = h / 2;

        // Wax seal base (circle)
        Color sealColor = anim >= 75 ? new Color(40, 120, 60) : (anim >= 50 ? new Color(180, 140, 60) : new Color(180, 40, 30));
        g2.setColor(new Color(sealColor.getRed(), sealColor.getGreen(), sealColor.getBlue(), 180));
        g2.fill(new Ellipse2D.Double(x, y, size, size));
        
        // Seal border
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(sealColor.getRed(), sealColor.getGreen(), sealColor.getBlue(), 255));
        g2.draw(new Ellipse2D.Double(x, y, size, size));

        // Filled portion (wax filling)
        double fillAngle = 360 * (anim / 100f);
        if (fillAngle > 0) {
            g2.setColor(new Color(sealColor.getRed(), sealColor.getGreen(), sealColor.getBlue(), 220));
            g2.fill(new Arc2D.Double(x, y, size, size, 90, -fillAngle, Arc2D.PIE));
        }

        // Cracks for low attendance (cracked seal)
        if (anim < 75) {
            int crackCount = (int)((75 - anim) / 15);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(60, 50, 40, 180));
            for (int i = 0; i < crackCount; i++) {
                double angle = Math.PI * 2 * i / crackCount;
                double startX = centerX + Math.cos(angle) * (size * 0.2);
                double startY = centerY + Math.sin(angle) * (size * 0.2);
                double endX = centerX + Math.cos(angle) * (size * 0.45);
                double endY = centerY + Math.sin(angle) * (size * 0.45);
                g2.draw(new Line2D.Double(startX, startY, endX, endY));
            }
        }

        // Center text
        g2.setColor(Theme.CARD);
        g2.setFont(getFont().deriveFont(Font.BOLD, 20f));
        String s = ((int) anim) + "%";
        int sw = g2.getFontMetrics().stringWidth(s);
        g2.drawString(s, (w - sw) / 2, centerY + 6);

        g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        g2.setColor(Theme.MUTED);
        String t = "Attendance";
        int tw = g2.getFontMetrics().stringWidth(t);
        g2.drawString(t, (w - tw) / 2, centerY + 24);

        g2.dispose();
    }
}
