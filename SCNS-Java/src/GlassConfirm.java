import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public final class GlassConfirm {
    private GlassConfirm() {}

    public static boolean confirm(Component parent, String title, String message, String okText, String cancelText) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        ConfirmDialog d = new ConfirmDialog(owner, title, message, okText, cancelText);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
        return d.result;
    }

    private static final class ConfirmDialog extends JDialog {
        private boolean result = false;
        private float alpha = 0f;
        private float scale = 0.98f;

        ConfirmDialog(Window owner, String title, String message, String okText, String cancelText) {
            super(owner);
            setModal(true);
            setUndecorated(true);
            setAlwaysOnTop(true);
            setBackground(new Color(0, 0, 0, 0));

            View view = new View(title, message, okText, cancelText);
            setContentPane(view);
            setSize(new Dimension(740, 520));

            view.onOk = () -> close(true);
            view.onCancel = () -> close(false);

            getRootPane().registerKeyboardAction(e -> close(false),
                    javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);

            // animate in
            try { setOpacity(0f); } catch (Throwable ignored) {}
            Anim.run(180, 60, t -> {
                alpha = (float) Anim.easeOutCubic(t);
                scale = (float) (0.98 + 0.02 * Anim.easeOutCubic(t));
                repaint();
                try { setOpacity(Math.min(1f, alpha)); } catch (Throwable ignored) {}
            }, null);
        }

        private void close(boolean ok) {
            float startA = alpha;
            float startS = scale;
            Anim.run(140, 60, t -> {
                float e = (float) Anim.easeOutCubic(t);
                alpha = startA * (1f - e);
                scale = (float) (startS - 0.02 * e);
                repaint();
                try { setOpacity(Math.max(0f, alpha)); } catch (Throwable ignored) {}
            }, () -> {
                result = ok;
                setVisible(false);
                dispose();
            });
        }

        private final class View extends JPanel {
            private Runnable onOk;
            private Runnable onCancel;

            private final String title;
            private final String message;
            private final String okText;
            private final String cancelText;

            View(String title, String message, String okText, String cancelText) {
                this.title = title;
                this.message = message;
                this.okText = okText;
                this.cancelText = cancelText;
                setOpaque(false);
                setLayout(null);

                JLabel t = new JLabel(title);
                t.setForeground(Theme.TEXT);
                t.setFont(t.getFont().deriveFont(Font.BOLD, 18f));

                JLabel m = new JLabel("<html>" + escapeHtml(message).replace("\n", "<br/>") + "</html>");
                m.setForeground(Theme.MUTED);
                m.setFont(m.getFont().deriveFont(Font.PLAIN, 13f));
                m.setVerticalAlignment(SwingConstants.TOP);

                ModernButton ok = new ModernButton(okText, Theme.ACCENT, Theme.ACCENT_2);
                ModernButton cancel = new ModernButton(cancelText, Theme.CARD, Theme.CARD_2);

                ok.addActionListener(e -> { if (onOk != null) onOk.run(); });
                cancel.addActionListener(e -> { if (onCancel != null) onCancel.run(); });

                add(t);
                add(m);
                add(ok);
                add(cancel);

                addComponentListener(new java.awt.event.ComponentAdapter() {
                    @Override
                    public void componentResized(java.awt.event.ComponentEvent e) {
                        int w = getWidth();
                        int pad = 56;
                        t.setBounds(pad, pad, w - pad * 2, 28);
                        m.setBounds(pad, pad + 38, w - pad * 2, 260);
                        int by = getHeight() - 110;
                        cancel.setBounds(w - pad - 120 - 150, by, 120, 40);
                        ok.setBounds(w - pad - 150, by, 150, 40);
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Dim background
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.58f * alpha));
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, w, h);

                // Card
                int cw = 620;
                int ch = 360;
                int cx = (w - cw) / 2;
                int cy = (h - ch) / 2;

                g2.translate(cx + cw / 2.0, cy + ch / 2.0);
                g2.scale(scale, scale);
                g2.translate(-cw / 2.0, -ch / 2.0);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(Theme.GLASS);
                g2.fillRoundRect(0, 0, cw, ch, 26, 26);

                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, cw - 1, ch - 1, 26, 26);

                // subtle top sheen
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.10f * alpha));
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(2, 2, cw - 4, 90, 24, 24);

                g2.dispose();
                super.paintComponent(g);
            }

            private String escapeHtml(String s) {
                if (s == null) return "";
                return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            }
        }
    }
}
