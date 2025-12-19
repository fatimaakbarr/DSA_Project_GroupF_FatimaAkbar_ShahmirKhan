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
    private final List<String> path = new ArrayList<>(); // primary
    private final List<String> path2 = new ArrayList<>(); // secondary (compare mode)
    private final List<String> visited = new ArrayList<>();
    private final List<Integer> edgeWeights = new ArrayList<>();

    private String mode = "BFS"; // BFS or Dijkstra (for coloring)

    private float pulse = 0f;
    private final javax.swing.Timer idle;

    private javax.swing.Timer routeTimer;
    private int visIndex = 0;
    private int edgeIndex = 0;
    private float edgeT = 0f;
    private long lastTick = 0;

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

    public void setMode(String mode) {
        this.mode = mode == null ? "BFS" : mode;
        repaint();
    }

    public void animateTraversal(List<String> path, List<String> visited, List<Integer> edgeWeights, String mode) {
        setMode(mode);
        this.path.clear();
        if (path != null) this.path.addAll(path);
        this.path2.clear();
        this.visited.clear();
        if (visited != null) this.visited.addAll(visited);
        this.edgeWeights.clear();
        if (edgeWeights != null) this.edgeWeights.addAll(edgeWeights);

        if (routeTimer != null) routeTimer.stop();
        visIndex = 0;
        edgeIndex = 0;
        edgeT = 0f;
        lastTick = System.currentTimeMillis();

        routeTimer = new javax.swing.Timer(16, e -> tick());
        routeTimer.start();
    }

    public void animateCompare(List<String> primaryPath, List<String> secondaryPath, List<String> visited) {
        this.path.clear();
        if (primaryPath != null) this.path.addAll(primaryPath);
        this.path2.clear();
        if (secondaryPath != null) this.path2.addAll(secondaryPath);
        this.visited.clear();
        if (visited != null) this.visited.addAll(visited);
        this.edgeWeights.clear();
        if (routeTimer != null) routeTimer.stop();
        visIndex = this.visited.size();
        edgeIndex = Math.max(0, this.path.size() - 1);
        edgeT = 1f;
        repaint();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long dt = Math.max(1, now - lastTick);
        lastTick = now;

        int visStepMs = "Dijkstra".equalsIgnoreCase(mode) ? 105 : 85;
        int visStep = (int) (dt / visStepMs);
        if (visStep < 1) visStep = 1;
        visIndex = Math.min(visited.size(), visIndex + visStep);

        int edges = Math.max(0, path.size() - 1);
        if (edges <= 0) { routeTimer.stop(); repaint(); return; }
        if (edgeIndex >= edges) { routeTimer.stop(); repaint(); return; }

        int w = 1;
        if (edgeIndex < edgeWeights.size()) w = Math.max(1, edgeWeights.get(edgeIndex));
        int segMs = "Dijkstra".equalsIgnoreCase(mode) ? (120 + w * 45) : 220;
        edgeT += (float) dt / (float) segMs;
        if (edgeT >= 1f) {
            edgeIndex++;
            edgeT = 0f;
        }
        repaint();
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

        Color bfsC = new Color(60, 220, 255);
        Color dijC = new Color(180, 110, 255);
        Color visitC = "Dijkstra".equalsIgnoreCase(mode) ? dijC : bfsC;

        // Weighted campus edges: shorter = brighter/thicker
        drawCampusEdge(g2, pos, "Gate", "Admin", 3);
        drawCampusEdge(g2, pos, "Gate", "Library", 15);
        drawCampusEdge(g2, pos, "Gate", "Ground", 2);
        drawCampusEdge(g2, pos, "Ground", "Cafeteria", 2);
        drawCampusEdge(g2, pos, "Cafeteria", "Library", 2);
        drawCampusEdge(g2, pos, "Admin", "Block-A", 4);
        drawCampusEdge(g2, pos, "Admin", "Block-B", 6);
        drawCampusEdge(g2, pos, "Block-A", "Lab", 3);
        drawCampusEdge(g2, pos, "Block-B", "Lab", 2);
        drawCampusEdge(g2, pos, "Lab", "Hostel", 5);
        drawCampusEdge(g2, pos, "Ground", "Hostel", 4);

        // Visited glow
        int visCount = Math.min(visited.size(), visIndex);
        for (int i = 0; i < visCount; i++) {
            String v = visited.get(i);
            double[] p = pos.get(v);
            if (p == null) continue;
            int age = (visCount - 1) - i;
            int a = Math.max(10, 70 - age * 12);
            a += (int) (18 * (0.5 + 0.5 * Math.sin(pulse + i)));
            g2.setColor(new Color(visitC.getRed(), visitC.getGreen(), visitC.getBlue(), Math.min(120, a)));
            double rr = 16 + 4 * (0.5 + 0.5 * Math.sin(pulse * 0.9 + i));
            g2.fill(new Ellipse2D.Double(p[0] - rr, p[1] - rr, rr * 2, rr * 2));
        }

        // Path lines
        int pathEdges = Math.max(0, path.size() - 1);
        g2.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < Math.min(edgeIndex, pathEdges); i++) {
            double[] a = pos.get(path.get(i));
            double[] b = pos.get(path.get(i + 1));
            if (a == null || b == null) continue;
            g2.setColor(Theme.ACCENT);
            g2.draw(new Line2D.Double(a[0], a[1], b[0], b[1]));
        }
        if (edgeIndex < pathEdges) {
            double[] a = pos.get(path.get(edgeIndex));
            double[] b = pos.get(path.get(edgeIndex + 1));
            if (a != null && b != null) {
                double x = a[0] + (b[0] - a[0]) * Math.max(0, Math.min(1, edgeT));
                double y = a[1] + (b[1] - a[1]) * Math.max(0, Math.min(1, edgeT));
                g2.setColor(Theme.ACCENT);
                g2.draw(new Line2D.Double(a[0], a[1], x, y));
            }
        }

        // Avatar dot walking along the primary route
        double[] avatar = avatarPos(pos);
        if (avatar != null) {
            double x = avatar[0], y = avatar[1];
            double glow = 7 + 2 * (0.5 + 0.5 * Math.sin(pulse * 1.2));
            g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 120));
            g2.fill(new Ellipse2D.Double(x - glow, y - glow, glow * 2, glow * 2));
            g2.setColor(Theme.TEXT);
            g2.fill(new Ellipse2D.Double(x - 3.5, y - 3.5, 7, 7));
        }

        // Secondary path (compare mode) - dashed cyan
        int path2Edges = Math.max(0, path2.size() - 1);
        if (path2Edges > 0) {
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[] { 7f, 7f }, 0f));
            for (int i = 0; i < path2Edges; i++) {
                double[] a = pos.get(path2.get(i));
                double[] b = pos.get(path2.get(i + 1));
                if (a == null || b == null) continue;
                g2.setColor(new Color(visitC.getRed(), visitC.getGreen(), visitC.getBlue(), 200));
                g2.draw(new Line2D.Double(a[0], a[1], b[0], b[1]));
            }
        }

        // nodes
        Font f = getFont().deriveFont(Font.BOLD, 12f);
        g2.setFont(f);
        for (String n : nodes) {
            double[] p = pos.get(n);
            boolean inPath = path.contains(n);
            boolean inPath2 = path2.contains(n);
            boolean isVisited = visited.contains(n);

            Color ring = inPath ? Theme.ACCENT : (inPath2 ? visitC : (isVisited ? visitC : new Color(90, 105, 140)));
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
        // Map-like layout:
        // Gate at top edge, academics centered, hostels bottom.
        for (String n : nodes) {
            double x = 0.5, y = 0.5;
            if ("Gate".equals(n))      { x = 0.14; y = 0.10; }
            if ("Admin".equals(n))     { x = 0.34; y = 0.22; }
            if ("Library".equals(n))   { x = 0.84; y = 0.16; }
            if ("Block-A".equals(n))   { x = 0.56; y = 0.30; }
            if ("Block-B".equals(n))   { x = 0.50; y = 0.44; }
            if ("Cafeteria".equals(n)) { x = 0.70; y = 0.46; }
            if ("Ground".equals(n))    { x = 0.30; y = 0.56; }
            if ("Lab".equals(n))       { x = 0.62; y = 0.62; }
            if ("Hostel".equals(n))    { x = 0.78; y = 0.86; }
            double px = 24 + x * (w - 48);
            double py = 22 + y * (h - 44);
            pos.put(n, new double[] { px, py });
        }
        return pos;
    }

    private static void drawCampusEdge(Graphics2D g2, Map<String, double[]> pos, String a, String b, int w) {
        double[] pa = pos.get(a);
        double[] pb = pos.get(b);
        if (pa == null || pb == null) return;
        int clamped = Math.max(1, Math.min(15, w));
        float t = (15f - clamped) / 14f; // shorter -> 1
        int alpha = 18 + (int) (44 * t);
        float stroke = 1.1f + 0.9f * t;
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 255, 255, alpha));
        g2.draw(new Line2D.Double(pa[0], pa[1], pb[0], pb[1]));
    }

    private double[] avatarPos(Map<String, double[]> pos) {
        if (path.size() < 2) return null;
        int edges = path.size() - 1;
        if (edges <= 0) return null;
        int seg = Math.max(0, Math.min(edges - 1, edgeIndex));
        double local = edgeIndex >= edges ? 1.0 : Math.max(0.0, Math.min(1.0, edgeT));
        double[] a = pos.get(path.get(seg));
        double[] b = pos.get(path.get(seg + 1));
        if (a == null || b == null) return null;
        return new double[] { a[0] + (b[0] - a[0]) * local, a[1] + (b[1] - a[1]) * local };
    }
}
