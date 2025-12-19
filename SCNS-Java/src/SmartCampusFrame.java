import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public class SmartCampusFrame extends JFrame {
    private final NativeBridge nb;
    private final JLayeredPane layers = new JLayeredPane();

    private final AnimatedSwitcher switcher = new AnimatedSwitcher();
    private SidebarNav sidebarNav; // for syncing Home cards with sidebar selection

    private SidebarWrapper sidebarWrapper;

    private void openScreen(String key) {
        if (key == null) return;
        if ("home".equals(key)) {
            if (sidebarNav != null) sidebarNav.selectKey("home");
            // Switch first (stable size), then collapse sidebar AFTER transition to avoid glitches.
            switcher.switchTo("home");
            if (sidebarWrapper != null) {
                javax.swing.Timer t = new javax.swing.Timer(320, e -> {
                    ((javax.swing.Timer) e.getSource()).stop();
                    sidebarWrapper.setExpanded(false, true, null);
                });
                t.setRepeats(false);
                t.start();
            }
            return;
        }
        if (sidebarNav != null) sidebarNav.selectKey(key);
        // Expand sidebar FIRST (resize), then switch screen (stable during snapshot transition).
        if (sidebarWrapper != null && sidebarWrapper.isCollapsed()) {
            sidebarWrapper.setExpanded(true, true, () -> switcher.switchTo(key));
        } else {
            switcher.switchTo(key);
        }
    }

    public SmartCampusFrame(NativeBridge nb) {
        super("SmartCampus DSA Project");
        this.nb = nb;

        Theme.apply();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1160, 720));
        setSize(1160, 720);
        setLocationRelativeTo(null);

        layers.setLayout(null);
        setContentPane(layers);

        RootView root = new RootView();
        root.setBounds(0, 0, 1160, 720);
        layers.add(root, Integer.valueOf(JLayeredPane.DEFAULT_LAYER));

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                root.setBounds(0, 0, getWidth(), getHeight());
                root.doLayout();
            }
        });

        javax.swing.SwingUtilities.invokeLater(() -> Toast.show(layers, nb.testConnection(), Theme.OK));
    }

    private final class RootView extends JPanel {
        private final Random rng = new Random(7);
        private final float[] px = new float[34];
        private final float[] py = new float[34];
        private final float[] pv = new float[34];
        private float bgPhase = 0f;
        private final Timer bgTimer;

        RootView() {
            setLayout(new BorderLayout());
            setOpaque(false);

            // live background animation so the UI always feels “alive”
            for (int i = 0; i < px.length; i++) {
                px[i] = rng.nextFloat();
                py[i] = rng.nextFloat();
                pv[i] = 0.3f + rng.nextFloat() * 0.9f;
            }
            bgTimer = new Timer(16, e -> {
                bgPhase += 0.012f;
                repaint();
            });
            bgTimer.start();

            JPanel sidebar = buildSidebar();
            sidebarWrapper = new SidebarWrapper(sidebar);
            sidebarWrapper.setExpanded(false, false, null); // hidden on home until a card is clicked

            add(sidebarWrapper, BorderLayout.WEST);
            add(switcher, BorderLayout.CENTER);

            // screens
            HomePanel home = new HomePanel(nb, key -> {
                openScreen(key);
            });
            NavigatorUI nav = new NavigatorUI(nb, layers);
            StudentInfoUI sis = new StudentInfoUI(nb, layers);
            AttendanceUI att = new AttendanceUI(nb, layers);

            switcher.addScreen("home", home);
            switcher.addScreen("nav", nav);
            switcher.addScreen("sis", sis);
            switcher.addScreen("att", att);

            switcher.showFirst("home");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, Theme.BG_0, w, h, Theme.BG_1);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // animated glow blobs
            int a1 = 18 + (int) (8 * Math.sin(bgPhase));
            int a2 = 14 + (int) (10 * Math.cos(bgPhase * 0.8));
            g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), a1));
            g2.fillOval((int) (-220 + 40 * Math.sin(bgPhase * 0.9)), (int) (-140 + 30 * Math.cos(bgPhase)), 560, 560);
            g2.setColor(new Color(Theme.ACCENT_2.getRed(), Theme.ACCENT_2.getGreen(), Theme.ACCENT_2.getBlue(), a2));
            g2.fillOval((int) (w - 520 + 55 * Math.cos(bgPhase * 0.7)), (int) (h - 520 + 45 * Math.sin(bgPhase * 0.8)), 820, 820);

            // floating particles
            g2.setColor(new Color(255, 255, 255, 18));
            for (int i = 0; i < px.length; i++) {
                float x = (px[i] + bgPhase * 0.02f * pv[i]) % 1f;
                float y = (py[i] + bgPhase * 0.015f * pv[i]) % 1f;
                double sx = x * w;
                double sy = y * h;
                double r = 1.5 + 2.2 * (0.5 + 0.5 * Math.sin(bgPhase * 1.6 + i));
                g2.fill(new Ellipse2D.Double(sx, sy, r, r));
            }

            g2.dispose();
        }
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 18, 18, 18));

        JComponent brand = new BrandHeader();
        brand.setMaximumSize(new Dimension(1000, 92));
        side.add(brand);
        side.add(Box.createVerticalStrut(18));

        sidebarNav = new SidebarNav();
        sidebarNav.setMaximumSize(new Dimension(1000, 260));
        sidebarNav.setPreferredSize(new Dimension(220, 220));
        side.add(sidebarNav);

        side.add(Box.createVerticalGlue());

        JComponent foot = new SidebarFooter();
        foot.setMaximumSize(new Dimension(1000, 90));
        side.add(foot);

        return side;
    }

    private static final class SidebarWrapper extends JPanel {
        private final JComponent inner;
        private float w = 0f;
        private float target = 0f;

        SidebarWrapper(JComponent inner) {
            super(new BorderLayout());
            this.inner = inner;
            setOpaque(false);
            add(inner, BorderLayout.CENTER);
        }

        boolean isCollapsed() { return w < 2f; }

        void setExpanded(boolean expanded, boolean animate, Runnable done) {
            target = expanded ? 260f : 0f;
            if (!animate) {
                w = target;
                inner.setVisible(w > 1f);
                revalidate();
                repaint();
                if (done != null) done.run();
                return;
            }
            float start = w;
            float end = target;
            inner.setVisible(true);
            Anim.run(260, 60, t -> {
                w = (float) (start + (end - start) * Anim.easeInOutCubic(t));
                if (w < 2f && end == 0f) inner.setVisible(false);
                revalidate();
                repaint();
            }, () -> {
                w = end;
                inner.setVisible(w > 1f);
                revalidate();
                repaint();
                if (done != null) done.run();
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension((int) Math.max(0, w), 10);
        }
    }

    /** Sidebar navigation with animated active indicator. */
    private final class SidebarNav extends JPanel {
        private final ModernButton bHome = new ModernButton("Home", Theme.CARD, Theme.CARD_2);
        private final ModernButton bNav = new ModernButton("Campus Navigator", Theme.CARD, Theme.CARD_2);
        private final ModernButton bSIS = new ModernButton("Student Info", Theme.CARD, Theme.CARD_2);
        private final ModernButton bAtt = new ModernButton("Attendance", Theme.CARD, Theme.CARD_2);

        private ModernButton selected = bHome;
        private float indY = 0f;
        private float indTargetY = 0f;

        SidebarNav() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            Dimension btn = new Dimension(220, 44);
            for (ModernButton b : new ModernButton[] { bHome, bNav, bSIS, bAtt }) {
                b.setHorizontalAlignment(SwingConstants.LEFT);
                b.setMaximumSize(btn);
                b.setPreferredSize(btn);
                b.setMinimumSize(btn);
                b.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 14, 0, 14));
                add(b);
                add(Box.createVerticalStrut(10));
            }

            select(bHome, false);

            bHome.addActionListener(e -> openScreen("home"));
            bNav.addActionListener(e -> openScreen("nav"));
            bSIS.addActionListener(e -> openScreen("sis"));
            bAtt.addActionListener(e -> openScreen("att"));
        }

        void selectKey(String key) {
            if ("home".equals(key)) select(bHome, true);
            else if ("nav".equals(key)) select(bNav, true);
            else if ("sis".equals(key)) select(bSIS, true);
            else if ("att".equals(key)) select(bAtt, true);
        }

        private void select(ModernButton b, boolean animate) {
            if (selected != null) selected.setSelectedVisual(false);
            selected = b;
            if (selected != null) selected.setSelectedVisual(true);

            // update target indicator position (relative to this panel)
            indTargetY = selected.getY() + selected.getHeight() / 2f;
            if (!animate) indY = indTargetY;

            float start = indY;
            float target = indTargetY;
            if (animate) {
                Anim.run(260, 60, t -> {
                    indY = (float) (start + (target - start) * Anim.easeInOutCubic(t));
                    repaint();
                }, null);
            } else {
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (selected == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Ensure we have correct coordinates after first layout
            indTargetY = selected.getY() + selected.getHeight() / 2f;
            if (Math.abs(indY) < 0.001f) indY = indTargetY;

            int x = 2;
            int w = getWidth() - 4;
            int h = 44;
            int y = (int) (indY - h / 2f);

            // glow behind selected
            g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 18));
            g2.fillRoundRect(x, y - 6, w, h + 12, 18, 18);

            // left active bar
            g2.setColor(Theme.ACCENT_2);
            g2.fillRoundRect(6, y + 8, 6, h - 16, 10, 10);

            g2.dispose();
        }
    }

    private static final class BrandHeader extends JComponent {
        BrandHeader() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(0, 0, w, h, 22, 22);

            g2.setColor(Theme.ACCENT);
            g2.fillRoundRect(0, 0, 8, h, 22, 22);

            g2.setColor(Theme.TEXT);
            g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
            g2.drawString("SmartCampus", 16, 34);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            g2.setColor(Theme.MUTED);
            g2.drawString("DSA + JNI unified system", 16, 56);

            g2.dispose();
        }
    }

    private static final class SidebarFooter extends JComponent {
        SidebarFooter() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(0, 0, 0, 75));
            g2.fillRoundRect(0, 0, w, h, 22, 22);

            g2.setColor(Theme.MUTED);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
            g2.drawString("Java Swing (GUI)  •  C++ (DSA core)", 14, 28);
            g2.drawString("JNI bridge  •  One unified application", 14, 46);

            g2.dispose();
        }
    }
}

class AnimatedSwitcher extends JPanel {
    private final java.util.Map<String, JComponent> screens = new java.util.HashMap<>();
    private JComponent current;
    private JComponent next;
    private float t = 1f;
    private String currentKey;
    private java.awt.image.BufferedImage fromImg;
    private java.awt.image.BufferedImage toImg;
    private boolean transitioning = false;

    AnimatedSwitcher() {
        setOpaque(false);
        setLayout(null);
    }

    void addScreen(String key, JComponent c) {
        screens.put(key, c);
    }

    void showFirst(String key) {
        currentKey = key;
        current = screens.get(key);
        removeAll();
        if (current != null) {
            add(current);
            current.setBounds(0, 0, getWidth(), getHeight());
        }
        revalidate();
        repaint();
    }

    void switchTo(String key) {
        if (key == null || key.equals(currentKey)) return;
        next = screens.get(key);
        if (next == null) return;

        final JComponent from = current;
        final JComponent to = next;
        currentKey = key;

        // Snapshot-based transition: avoids flicker/glitches on Windows LAF.
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());

        if (to.getParent() != this) add(to);
        // Ensure both components are laid out at the correct size for the snapshot.
        if (from != null) {
            from.setBounds(0, 0, w, h);
            from.doLayout();
        }
        to.setBounds(0, 0, w, h);
        to.doLayout();

        fromImg = (from == null) ? null : snapshot(from, w, h);
        toImg = snapshot(to, w, h);

        // Hide real components during transition; we'll paint images.
        if (from != null) from.setVisible(false);
        to.setVisible(false);

        transitioning = true;
        t = 0f;

        // Faster, smoother transition (less sluggish)
        Anim.run(280, 60, tt -> {
            t = (float) Anim.easeInOutCubic(tt);
            repaint();
        }, () -> {
            transitioning = false;
            fromImg = null;
            toImg = null;

            if (from != null) remove(from);
            current = to;
            next = null;
            current.setVisible(true);
            current.setBounds(0, 0, getWidth(), getHeight());
            revalidate();
            repaint();
        });
    }

    @Override
    public void doLayout() {
        if (current != null) current.setBounds(0, 0, getWidth(), getHeight());
        if (next != null) next.setBounds(getWidth(), 0, getWidth(), getHeight());
    }

    @Override
    protected void paintChildren(Graphics g) {
        if (!transitioning) {
            super.paintChildren(g);
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int w = getWidth();
        int h = getHeight();
        int dx = (int) (w * t);

        float aTo = Math.max(0f, Math.min(1f, t));
        float aFrom = 1f - aTo;

        java.awt.Composite old = g2.getComposite();

        if (fromImg != null) {
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, aFrom));
            g2.drawImage(fromImg, -dx, 0, w, h, null);
        }
        if (toImg != null) {
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, aTo));
            g2.drawImage(toImg, w - dx, 0, w, h, null);
        }

        g2.setComposite(old);
        g2.dispose();
    }

    private static java.awt.image.BufferedImage snapshot(JComponent c, int w, int h) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // avoid ultra-heavy rendering during snapshot to keep transitions snappy
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        c.paint(g2);
        g2.dispose();
        return img;
    }
}

class HomePanel extends JPanel {
    HomePanel(NativeBridge nb, Consumer<String> onOpen) {
        setOpaque(false);
        setLayout(null);

        JComponent dash = new Dashboard(nb, onOpen);
        add(dash);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                dash.setBounds(24, 24, getWidth() - 48, getHeight() - 48);
            }
        });
    }

    private static final class Dashboard extends JComponent {
        private final String status;
        private final Consumer<String> onOpen;

        private float dim = 1f;
        private float t = 0f;
        private final javax.swing.Timer bg;

        private final HomeCard cNav;
        private final HomeCard cSis;
        private final HomeCard cAtt;

        Dashboard(NativeBridge nb, Consumer<String> onOpen) {
            setOpaque(false);
            this.status = nb.testConnection();
            this.onOpen = onOpen;

            setLayout(null);
            cNav = new HomeCard("Campus Navigator", "Algorithm race: BFS vs Dijkstra", "nav");
            cSis = new HomeCard("Student Records", "File cabinet + CSV persistence", "sis");
            cAtt = new HomeCard("Attendance", "Live ring + defaulters", "att");
            add(cNav);
            add(cSis);
            add(cAtt);

            // Pop-in sequence
            cNav.setAppear(0f);
            cSis.setAppear(0f);
            cAtt.setAppear(0f);
            javax.swing.Timer seq = new javax.swing.Timer(30, null);
            seq.addActionListener(e -> {
                seq.stop();
                Anim.run(520, 60, tt -> cNav.setAppear((float) Anim.easeOutBack(tt)), null);
                new javax.swing.Timer(120, e2 -> {
                    ((javax.swing.Timer) e2.getSource()).stop();
                    Anim.run(520, 60, tt -> cSis.setAppear((float) Anim.easeOutBack(tt)), null);
                }).start();
                new javax.swing.Timer(240, e3 -> {
                    ((javax.swing.Timer) e3.getSource()).stop();
                    Anim.run(520, 60, tt -> cAtt.setAppear((float) Anim.easeOutBack(tt)), null);
                }).start();
                Anim.run(480, 60, tt -> { dim = 1f - 0.25f * (float) Anim.easeOutCubic(tt); repaint(); }, null);
            });
            seq.setRepeats(false);
            seq.start();

            bg = new javax.swing.Timer(16, e -> { t += 0.010f; repaint(); });
            bg.start();
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            int top = 180;
            int gap = 18;
            int usable = Math.max(1, w - 64 - gap * 2);
            int cardW = Math.min(360, Math.max(260, usable / 3));
            int x0 = 32;
            int y = top;
            cNav.setBounds(x0, y, cardW, 170);
            cSis.setBounds(x0 + cardW + gap, y, cardW, 170);
            cAtt.setBounds(x0 + (cardW + gap) * 2, y, cardW, 170);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Dim overlay for launch vibe
            g2.setColor(new Color(0, 0, 0, (int) (90 * dim)));
            g2.fillRoundRect(0, 0, w, h, 28, 28);

            // Calm gradient drift (no orbits)
            int gx = (int) (w * (0.20 + 0.06 * Math.sin(t)));
            g2.setPaint(new GradientPaint(gx, 0, new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 26),
                    gx + 240, 0, new Color(Theme.ACCENT_2.getRed(), Theme.ACCENT_2.getGreen(), Theme.ACCENT_2.getBlue(), 14)));
            g2.fillRoundRect(0, 0, w, h, 28, 28);

            g2.setColor(Theme.TEXT);
            g2.setFont(getFont().deriveFont(Font.BOLD, 32f));
            g2.drawString("Campus Connect", 32, 78);

            g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
            g2.setColor(Theme.MUTED);
            g2.drawString("Modern DSA app • Java Swing + C++ (JNI) • Fun to demo", 32, 108);

            g2.setFont(getFont().deriveFont(Font.BOLD, 12.5f));
            g2.setColor(Theme.OK);
            g2.drawString(status, 32, 142);

            g2.dispose();
        }

        private final class HomeCard extends JComponent {
            private final String title;
            private final String sub;
            private final String key;

            private float hover = 0f;
            private float appear = 0f;
            private float mx = 0f, my = 0f;

            HomeCard(String title, String sub, String key) {
                this.title = title;
                this.sub = sub;
                this.key = key;
                setOpaque(false);

                MouseAdapter ma = new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        Anim.run(220, 60, t -> { hover = (float) Anim.easeOutCubic(t); repaint(); }, null);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        Anim.run(220, 60, t -> { hover = 1f - (float) Anim.easeOutCubic(t); mx = 0f; my = 0f; repaint(); }, null);
                    }

                    @Override
                    public void mouseMoved(MouseEvent e) {
                        float nx = (e.getX() - getWidth() / 2f) / Math.max(1f, getWidth() / 2f);
                        float ny = (e.getY() - getHeight() / 2f) / Math.max(1f, getHeight() / 2f);
                        mx = nx;
                        my = ny;
                        repaint();
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (onOpen != null) onOpen.accept(key);
                    }
                };
                addMouseListener(ma);
                addMouseMotionListener(ma);
            }

            void setAppear(float a) { appear = a; repaint(); }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                float a = Math.max(0f, Math.min(1f, appear));
                float s = 0.92f + 0.08f * a;
                float lift = (1f - a) * 24f;

                AffineTransform old = g2.getTransform();
                g2.translate(w / 2.0, h / 2.0 + lift);
                double rot = (mx) * (0.07 * hover);
                g2.rotate(rot);
                g2.scale(s, s);
                g2.translate(-w / 2.0, -h / 2.0);

                // shadow
                g2.setColor(new Color(0, 0, 0, (int) (90 * a)));
                g2.fillRoundRect(10, 14, w - 20, h - 18, 26, 26);

                // glass card
                int glowA = (int) (hover * 80);
                g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), glowA));
                g2.fillRoundRect(8, 10, w - 16, h - 18, 26, 26);
                g2.setColor(new Color(0, 0, 0, 95));
                g2.fillRoundRect(8, 10, w - 16, h - 18, 26, 26);
                g2.setColor(new Color(255, 255, 255, 22));
                g2.drawRoundRect(8, 10, w - 17, h - 19, 26, 26);

                // accent bar
                g2.setColor(new Color(Theme.ACCENT_2.getRed(), Theme.ACCENT_2.getGreen(), Theme.ACCENT_2.getBlue(), (int) (160 * a)));
                g2.fillRoundRect(12, 18, 6, h - 44, 10, 10);

                g2.setColor(Theme.TEXT);
                g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
                g2.drawString(title, 28, 52);
                g2.setColor(Theme.MUTED);
                g2.setFont(getFont().deriveFont(Font.PLAIN, 12.5f));
                g2.drawString(sub, 28, 78);

                g2.setColor(new Color(255, 255, 255, (int) (120 + 80 * hover)));
                g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
                g2.drawString("Open →", 28, h - 34);

                g2.setTransform(old);
                g2.dispose();
            }
        }
    }
}
