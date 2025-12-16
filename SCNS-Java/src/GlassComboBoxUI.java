import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;

@SuppressWarnings("serial")
public class GlassComboBoxUI extends BasicComboBoxUI {

    public static ComponentUI createUI(JComponent c) {
        return new GlassComboBoxUI();
    }

    @Override
    protected JButton createArrowButton() {
        JButton b = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // subtle separator
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                g2.setColor(Theme.BORDER);
                g2.drawLine(0, 6, 0, h - 6);

                // chevron
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
                g2.setColor(Theme.TEXT);
                int cx = w / 2;
                int cy = h / 2;
                Path2D p = new Path2D.Double();
                p.moveTo(cx - 5, cy - 2);
                p.lineTo(cx, cy + 3);
                p.lineTo(cx + 5, cy - 2);
                g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g2.draw(p);

                g2.dispose();
            }
        };

        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setPreferredSize(new Dimension(34, 10));
        return b;
    }

    @Override
    protected BasicComboPopup createPopup() {
        BasicComboPopup popup = new BasicComboPopup(comboBox) {
            @Override
            protected JScrollPane createScroller() {
                JScrollPane sp = super.createScroller();
                sp.setOpaque(false);
                sp.getViewport().setOpaque(false);
                sp.setBorder(BorderFactory.createEmptyBorder());
                return sp;
            }

            @Override
            protected void configurePopup() {
                super.configurePopup();
                setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

                if (list != null) {
                    list.setOpaque(false);
                    list.setForeground(Theme.TEXT);
                    list.setSelectionForeground(Theme.TEXT);
                    list.setSelectionBackground(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 140));
                }
            }

            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // glass background
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                g2.setColor(Theme.GLASS);
                g2.fillRoundRect(0, 0, w, h, 18, 18);

                // border
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);

                // top sheen
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.10f));
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(2, 2, w - 4, Math.max(10, h / 4), 16, 16);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        return popup;
    }
}
