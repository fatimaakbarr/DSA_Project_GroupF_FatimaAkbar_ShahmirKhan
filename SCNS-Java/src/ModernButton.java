import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

public class ModernButton extends JButton {
    private boolean hover = false;
    private boolean pressed = false;
    private float anim = 0f;

    private final Color fill;
    private final Color fillHover;

    private float rippleT = 0f;
    private int rippleX = 0;
    private int rippleY = 0;

    private float selectedT = 0f;

    public ModernButton(String text, Color fill, Color fillHover) {
        super(text);
        this.fill = fill;
        this.fillHover = fillHover;

        setOpaque(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(Theme.TEXT);
        setFont(getFont().deriveFont(Font.BOLD));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                animateTo(1f);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                pressed = false;
                animateTo(0f);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                rippleX = e.getX();
                rippleY = e.getY();
                rippleT = 0f;
                Anim.run(420, 60, t -> {
                    rippleT = (float) Anim.easeOutCubic(t);
                    repaint();
                }, null);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    /** For sidebar/navigation use: visually mark a button as selected. */
    public void setSelectedVisual(boolean selected) {
        float start = selectedT;
        float target = selected ? 1f : 0f;
        Anim.run(240, 60, t -> {
            selectedT = (float) (start + (target - start) * Anim.easeInOutCubic(t));
            repaint();
        }, null);
    }

    private void animateTo(float target) {
        float start = anim;
        Anim.run(180, 60, t -> {
            double e = Anim.easeOutCubic(t);
            anim = (float) (start + (target - start) * e);
            repaint();
        }, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        float lift = pressed ? 0f : 1.5f * anim;
        float alpha = pressed ? 0.92f : (0.90f + 0.10f * anim);

        Color base = hover ? blend(fill, fillHover, anim) : fill;
        // Selected tint (used for sidebar items)
        if (selectedT > 0f) {
            base = blend(base, Theme.ACCENT, 0.55f * selectedT);
            alpha = Math.min(1f, alpha + 0.10f * selectedT);
        }

        g2.translate(0, -lift);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(base);
        g2.fillRoundRect(0, 0, w, h, 18, 18);

        // subtle top highlight
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.10f));
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(2, 2, w - 4, Math.max(6, h / 3), 16, 16);

        // border to add definition
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f + 0.22f * selectedT));
        g2.setColor(selectedT > 0f ? new Color(Theme.ACCENT_2.getRed(), Theme.ACCENT_2.getGreen(), Theme.ACCENT_2.getBlue(), 140) : Theme.BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);

        // ripple (more “alive”)
        if (rippleT > 0f && rippleT < 1f) {
            float rt = rippleT;
            float maxR = (float) Math.hypot(w, h);
            float r = 10f + maxR * rt;
            float a = 0.18f * (1f - rt);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(rippleX - r, rippleY - r, r * 2, r * 2));
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
