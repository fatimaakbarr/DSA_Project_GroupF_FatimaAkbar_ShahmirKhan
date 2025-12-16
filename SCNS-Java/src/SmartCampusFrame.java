import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class SmartCampusFrame extends JFrame {
    private final NativeBridge nb;
    private final JLayeredPane layers = new JLayeredPane();

    private final AnimatedSwitcher switcher = new AnimatedSwitcher();

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
        RootView() {
            setLayout(new BorderLayout());
            setOpaque(false);

            JPanel sidebar = buildSidebar();
            sidebar.setPreferredSize(new Dimension(260, 10));

            add(sidebar, BorderLayout.WEST);
            add(switcher, BorderLayout.CENTER);

            // screens
            HomePanel home = new HomePanel(nb);
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

            // subtle glow
            g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 18));
            g2.fillOval(-200, -120, 520, 520);
            g2.setColor(new Color(Theme.ACCENT_2.getRed(), Theme.ACCENT_2.getGreen(), Theme.ACCENT_2.getBlue(), 14));
            g2.fillOval(w - 420, h - 420, 700, 700);

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

        ModernButton bHome = new ModernButton("Home", Theme.CARD, Theme.CARD_2);
        ModernButton bNav = new ModernButton("Campus Navigator", Theme.CARD, Theme.CARD_2);
        ModernButton bSIS = new ModernButton("Student Info", Theme.CARD, Theme.CARD_2);
        ModernButton bAtt = new ModernButton("Attendance", Theme.CARD, Theme.CARD_2);

        Dimension btn = new Dimension(220, 44);
        for (ModernButton b : new ModernButton[] { bHome, bNav, bSIS, bAtt }) {
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setMaximumSize(btn);
            b.setPreferredSize(btn);
            b.setMinimumSize(btn);
            b.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 14, 0, 14));
            side.add(b);
            side.add(Box.createVerticalStrut(10));
        }

        bHome.addActionListener(e -> switcher.switchTo("home"));
        bNav.addActionListener(e -> switcher.switchTo("nav"));
        bSIS.addActionListener(e -> switcher.switchTo("sis"));
        bAtt.addActionListener(e -> switcher.switchTo("att"));

        side.add(Box.createVerticalGlue());

        JComponent foot = new SidebarFooter();
        foot.setMaximumSize(new Dimension(1000, 90));
        side.add(foot);

        return side;
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

        if (to.getParent() != this) add(to);
        to.setBounds(getWidth(), 0, getWidth(), getHeight());
        t = 0f;

        Anim.run(360, 60, tt -> {
            t = (float) Anim.easeOutCubic(tt);
            int dx = (int) (getWidth() * t);
            if (from != null) from.setBounds(-dx, 0, getWidth(), getHeight());
            to.setBounds(getWidth() - dx, 0, getWidth(), getHeight());
            repaint();
        }, () -> {
            if (from != null) remove(from);
            current = to;
            next = null;
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
}

class HomePanel extends JPanel {
    HomePanel(NativeBridge nb) {
        setOpaque(false);
        setLayout(null);

        JComponent hero = new Hero(nb);
        add(hero);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                hero.setBounds(24, 24, getWidth() - 48, getHeight() - 48);
            }
        });
    }

    private static final class Hero extends JComponent {
        private final String status;

        Hero(NativeBridge nb) {
            setOpaque(false);
            this.status = nb.testConnection();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRoundRect(0, 0, w, h, 28, 28);

            g2.setColor(Theme.TEXT);
            g2.setFont(getFont().deriveFont(Font.BOLD, 32f));
            g2.drawString("SmartCampus DSA System", 32, 78);

            g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
            g2.setColor(Theme.MUTED);
            g2.drawString("Unified Java GUI + C++ DSAs connected via JNI", 32, 108);

            g2.setFont(getFont().deriveFont(Font.BOLD, 12.5f));
            g2.setColor(Theme.OK);
            g2.drawString(status, 32, 142);

            g2.setFont(getFont().deriveFont(Font.PLAIN, 13f));
            g2.setColor(Theme.TEXT);
            int y = 190;
            g2.drawString("Modules:", 32, y);
            y += 26;
            g2.setColor(Theme.MUTED);
            g2.drawString("• Campus Navigator (Graph + BFS / Dijkstra)", 48, y);
            y += 20;
            g2.drawString("• Student Information System (AVL Tree + searching/sorting)", 48, y);
            y += 20;
            g2.drawString("• Attendance Management (Queue/Array + Min-Heap)", 48, y);

            g2.dispose();
        }
    }
}
