import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;

public final class Toast {
    private Toast() {}

    public static void show(JLayeredPane layers, String msg, Color color) {
        ToastView view = new ToastView(msg, color);
        int w = 420;
        int h = 54;
        int x = (layers.getWidth() - w) / 2;
        int y = 22;
        view.setBounds(x, y, w, h);
        layers.add(view, Integer.valueOf(JLayeredPane.POPUP_LAYER));
        layers.repaint();

        Anim.run(220, 60, t -> {
            view.alpha = (float) Anim.easeOutCubic(t);
            view.repaint();
        }, () -> {
            Anim.run(2200, 60, t -> {
                // hold
            }, () -> {
                Anim.run(260, 60, t -> {
                    view.alpha = 1f - (float) Anim.easeOutCubic(t);
                    view.repaint();
                }, () -> {
                    layers.remove(view);
                    layers.repaint();
                });
            });
        });
    }

    private static final class ToastView extends JComponent {
        private final String msg;
        private final Color color;
        private float alpha = 0f;

        ToastView(String msg, Color color) {
            this.msg = msg;
            this.color = color;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(0, 0, w, h, 20, 20);

            g2.setColor(color);
            g2.fillRoundRect(0, 0, 8, h, 20, 20);

            g2.setColor(Theme.TEXT);
            g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g2.drawString(msg, 18, 34);

            g2.dispose();
        }
    }
}
