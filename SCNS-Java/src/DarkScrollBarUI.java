import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;

// Dark, thin, rounded scrollbars for consistent theme.
public class DarkScrollBarUI extends BasicScrollBarUI {
    @Override
    protected void configureScrollBarColors() {
        // handled in paint
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroButton();
    }

    private static JButton zeroButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 70));
        int arc = 999;
        g2.fillRoundRect(trackBounds.x + 2, trackBounds.y + 2, trackBounds.width - 4, trackBounds.height - 4, arc, arc);
        g2.dispose();
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean hover = isThumbRollover();
        Color base = hover ? Theme.ACCENT_2 : new Color(170, 180, 205);
        int a = hover ? 165 : 120;
        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), a));

        int arc = 999;
        int pad = 2;
        g2.fillRoundRect(thumbBounds.x + pad, thumbBounds.y + pad, thumbBounds.width - pad * 2, thumbBounds.height - pad * 2, arc, arc);

        // subtle outline
        g2.setColor(new Color(255, 255, 255, hover ? 55 : 35));
        g2.drawRoundRect(thumbBounds.x + pad, thumbBounds.y + pad, thumbBounds.width - pad * 2 - 1, thumbBounds.height - pad * 2 - 1, arc, arc);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        // thin
        if (scrollbar.getOrientation() == javax.swing.JScrollBar.VERTICAL) return new Dimension(10, super.getPreferredSize(c).height);
        return new Dimension(super.getPreferredSize(c).width, 10);
    }
}
