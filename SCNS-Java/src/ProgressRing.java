import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;

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

        g2.setColor(Theme.CARD);
        g2.fillRoundRect(0, 0, w, h, 22, 22);

        int size = Math.min(w, h) - 40;
        int x = (w - size) / 2;
        int y = (h - size) / 2;

        g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(100, 110, 140, 60));
        g2.draw(new Arc2D.Double(x, y, size, size, 90, -360, Arc2D.OPEN));

        Color accent = anim >= 75 ? Theme.OK : (anim >= 50 ? Theme.ACCENT_2 : Theme.DANGER);
        g2.setColor(accent);
        g2.draw(new Arc2D.Double(x, y, size, size, 90, -360 * (anim / 100f), Arc2D.OPEN));

        g2.setColor(Theme.TEXT);
        g2.setFont(getFont().deriveFont(Font.BOLD, 22f));
        String s = ((int) anim) + "%";
        int sw = g2.getFontMetrics().stringWidth(s);
        g2.drawString(s, (w - sw) / 2, h / 2 + 8);

        g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        g2.setColor(Theme.MUTED);
        String t = "Attendance";
        int tw = g2.getFontMetrics().stringWidth(t);
        g2.drawString(t, (w - tw) / 2, h / 2 + 28);

        g2.dispose();
    }
}
