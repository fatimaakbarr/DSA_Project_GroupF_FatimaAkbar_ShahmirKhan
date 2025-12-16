import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

public class TreeView extends JComponent {
    private List<int[]> edges = java.util.Collections.emptyList();
    private int highlightRoll = -1;
    private float alpha = 0f;
    private float pulse = 0f;
    private final javax.swing.Timer idle = new javax.swing.Timer(16, e -> {
        pulse += 0.03f;
        repaint();
    });

    public TreeView() {
        setOpaque(false);
        idle.start();
    }

    public void setSnapshot(List<int[]> edges, int highlightRoll) {
        this.edges = edges == null ? java.util.Collections.emptyList() : edges;
        this.highlightRoll = highlightRoll;
        this.alpha = 0f;
        Anim.run(420, 60, t -> {
            alpha = (float) Anim.easeOutCubic(t);
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

        if (edges.isEmpty()) {
            g2.setColor(Theme.MUTED);
            g2.drawString("AVL tree is empty.", 18, 28);
            g2.dispose();
            return;
        }

        // Build adjacency + root guess
        Map<Integer, int[]> map = new HashMap<>();
        java.util.Set<Integer> children = new java.util.HashSet<>();
        for (int[] e : edges) {
            map.put(e[0], e);
            if (e[1] != 0) children.add(e[1]);
            if (e[2] != 0) children.add(e[2]);
        }
        int root = edges.get(0)[0];
        for (int[] e : edges) {
            if (!children.contains(e[0])) { root = e[0]; break; }
        }

        Map<Integer, double[]> pos = computePositions(map, root, w, h);

        // edges
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(120, 130, 160, (int) (140 * alpha)));
        for (int[] e : edges) {
            double[] p = pos.get(e[0]);
            if (p == null) continue;
            if (e[1] != 0) {
                double[] c = pos.get(e[1]);
                if (c != null) g2.draw(new Line2D.Double(p[0], p[1], c[0], c[1]));
            }
            if (e[2] != 0) {
                double[] c = pos.get(e[2]);
                if (c != null) g2.draw(new Line2D.Double(p[0], p[1], c[0], c[1]));
            }
        }

        // nodes
        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        for (int[] e : edges) {
            int roll = e[0];
            double[] p = pos.get(roll);
            if (p == null) continue;

            boolean hl = (roll == highlightRoll);
            int glowA = hl ? (50 + (int) (55 * (0.5 + 0.5 * Math.sin(pulse * 1.3)))) : 0;
            if (glowA > 0) {
                g2.setColor(new Color(Theme.ACCENT_2.getRed(), Theme.ACCENT_2.getGreen(), Theme.ACCENT_2.getBlue(), glowA));
                g2.fill(new Ellipse2D.Double(p[0] - 22, p[1] - 22, 44, 44));
            }
            Color ring = hl ? Theme.ACCENT_2 : Theme.ACCENT;

            g2.setColor(new Color(12, 14, 24));
            g2.fill(new Ellipse2D.Double(p[0] - 14, p[1] - 14, 28, 28));
            g2.setStroke(new BasicStroke(2.4f));
            g2.setColor(ring);
            g2.draw(new Ellipse2D.Double(p[0] - 14, p[1] - 14, 28, 28));

            g2.setColor(Theme.TEXT);
            String s = String.valueOf(roll);
            g2.drawString(s, (int) p[0] - (s.length() * 3), (int) p[1] + 4);
        }

        g2.dispose();
    }

    private static Map<Integer, double[]> computePositions(Map<Integer, int[]> map, int root, int w, int h) {
        Map<Integer, double[]> pos = new HashMap<>();

        // BFS levels
        Map<Integer, Integer> depth = new HashMap<>();
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(root);
        depth.put(root, 0);
        int maxDepth = 0;

        while (!q.isEmpty()) {
            int u = q.removeFirst();
            int d = depth.get(u);
            maxDepth = Math.max(maxDepth, d);
            int[] e = map.get(u);
            if (e == null) continue;
            if (e[1] != 0 && !depth.containsKey(e[1])) {
                depth.put(e[1], d + 1);
                q.add(e[1]);
            }
            if (e[2] != 0 && !depth.containsKey(e[2])) {
                depth.put(e[2], d + 1);
                q.add(e[2]);
            }
        }

        // group by depth
        Map<Integer, java.util.List<Integer>> levels = new HashMap<>();
        for (Map.Entry<Integer, Integer> en : depth.entrySet()) {
            levels.computeIfAbsent(en.getValue(), k -> new java.util.ArrayList<>()).add(en.getKey());
        }

        int padX = 30;
        int padY = 36;
        int usableW = Math.max(1, w - padX * 2);
        int usableH = Math.max(1, h - padY * 2);

        for (int d = 0; d <= maxDepth; d++) {
            java.util.List<Integer> level = levels.getOrDefault(d, java.util.Collections.emptyList());
            level.sort(Integer::compareTo);
            double y = padY + (usableH * (d + 1.0) / (maxDepth + 2.0));
            for (int i = 0; i < level.size(); i++) {
                double x = padX + (usableW * (i + 1.0) / (level.size() + 1.0));
                pos.put(level.get(i), new double[] { x, y });
            }
        }

        return pos;
    }
}
