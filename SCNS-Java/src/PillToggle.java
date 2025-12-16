import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;

public class PillToggle extends JButton {
    private boolean hover = false;
    private boolean active = false;
    private float hoverT = 0f;
    private float activeT = 0f;

    public PillToggle(String text) {
        super(text);
        setOpaque(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(Theme.TEXT);
        setFont(getFont().deriveFont(Font.BOLD, 13f));

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                hover = true;
                animateHover(1f);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hover = false;
                animateHover(0f);
            }
        });
    }

    public void setActive(boolean active) {
        if (this.active == active) return;
        this.active = active;
        float start = activeT;
        float target = active ? 1f : 0f;
        Anim.run(220, 60, t -> {
            activeT = (float) (start + (target - start) * Anim.easeInOutCubic(t));
            repaint();
        }, null);
    }

    public boolean isActive() {
        return active;
    }

    private void animateHover(float target) {
        float start = hoverT;
        Anim.run(180, 60, t -> {
            hoverT = (float) (start + (target - start) * Anim.easeOutCubic(t));
            repaint();
        }, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        Color base = Theme.CARD;
        Color on = Theme.ACCENT;

        Color fill = blend(base, on, activeT);
        float alpha = 0.92f + 0.06f * hoverT;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, w, h, 999, 999);

        // subtle border
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f));
        g2.setColor(active ? new Color(Theme.ACCENT_2.getRed(), Theme.ACCENT_2.getGreen(), Theme.ACCENT_2.getBlue(), 110) : Theme.BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 999, 999);

        // inner glow when active
        if (activeT > 0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.16f * activeT));
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(2, 2, w - 4, Math.max(8, h / 2), 999, 999);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    private static Color blend(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }
}
