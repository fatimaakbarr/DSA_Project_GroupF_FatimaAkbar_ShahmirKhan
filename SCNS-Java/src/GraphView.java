import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

public class GraphView extends JComponent {
    private String[] nodes = new String[0];
    private final List<String> path = new ArrayList<>();
    private final List<String> visited = new ArrayList<>();

    private float progress = 0f;
    private float pulse = 0f;
    private final javax.swing.Timer idle;

    public GraphView() {
        setOpaque(false);
        idle = new javax.swing.Timer(16, e -> {
            pulse += 0.035f;
            repaint();
        });
        idle.start();
    }

    public void setNodes(String[] nodes) {
        this.nodes = nodes == null ? new String[0] : nodes;
        repaint();
    }

    public void animateResult(List<String> path, List<String> visited) {
        this.path.clear();
        if (path != null) this.path.addAll(path);
        this.visited.clear();
        if (visited != null) this.visited.addAll(visited);
        progress = 0f;
        Anim.run(700, 60, t -> {
            progress = (float) Anim.easeOutCubic(t);
            repaint();
        }, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // soft background
        g2.setColor(Theme.CARD);
        g2.fillRoundRect(0, 0, w, h, 22, 22);

        if (nodes.length == 0) {
            g2.setColor(Theme.MUTED);
            g2.drawString("No campus nodes loaded.", 18, 28);
            g2.dispose();
            return;
        }

        Map<String, double[]> pos = layout(nodes, w, h);

        // Visited glow
        int visCount = (int) Math.floor(visited.size() * progress);
        for (int i = 0; i < visCount; i++) {
            String v = visited.get(i);
            double[] p = pos.get(v);
            if (p == null) continue;
            int a = 28 + (int) (18 * (0.5 + 0.5 * Math.sin(pulse + i)));
            g2.setColor(new Color(Theme.ACCENT_2.getRed(), Theme.ACCENT_2.getGreen(), Theme.ACCENT_2.getBlue(), a));
            double rr = 18 + 6 * (0.5 + 0.5 * Math.sin(pulse * 0.9 + i));
            g2.fill(new Ellipse2D.Double(p[0] - rr, p[1] - rr, rr * 2, rr * 2));
        }

        // Path lines
        int pathEdges = Math.max(0, path.size() - 1);
        int drawEdges = (int) Math.floor(pathEdges * progress);
        g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < drawEdges; i++) {
            double[] a = pos.get(path.get(i));
            double[] b = pos.get(path.get(i + 1));
            if (a == null || b == null) continue;
            g2.setColor(Theme.ACCENT);
            g2.draw(new Line2D.Double(a[0], a[1], b[0], b[1]));
        }

        // nodes
        Font f = getFont().deriveFont(Font.BOLD, 12f);
        g2.setFont(f);
        for (String n : nodes) {
            double[] p = pos.get(n);
            boolean inPath = path.contains(n);
            boolean isVisited = visited.contains(n);

            Color ring = inPath ? Theme.ACCENT : (isVisited ? Theme.ACCENT_2 : new Color(90, 105, 140));
            Color fill = new Color(12, 14, 24);

            g2.setColor(fill);
            g2.fill(new Ellipse2D.Double(p[0] - 12, p[1] - 12, 24, 24));

            g2.setStroke(new BasicStroke(2.2f));
            g2.setColor(ring);
            g2.draw(new Ellipse2D.Double(p[0] - 12, p[1] - 12, 24, 24));

            g2.setColor(Theme.TEXT);
            g2.drawString(n, (int) p[0] - 14, (int) p[1] - 16);
        }

        g2.dispose();
    }

    private static Map<String, double[]> layout(String[] nodes, int w, int h) {
        Map<String, double[]> pos = new HashMap<>();
        double cx = w / 2.0;
        double cy = h / 2.0;
        double r = Math.min(w, h) * 0.36;
        for (int i = 0; i < nodes.length; i++) {
            double ang = (Math.PI * 2.0) * ((double) i / (double) nodes.length) - Math.PI / 2.0;
            double x = cx + Math.cos(ang) * r;
            double y = cy + Math.sin(ang) * r;
            pos.put(nodes[i], new double[] { x, y });
        }
        return pos;
    }
}
