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

    private String mode = "BFS"; // BFS or Dijkstra (for coloring)

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

    public void setMode(String mode) {
        this.mode = mode == null ? "BFS" : mode;
        repaint();
    }

    public void animateResult(List<String> path, List<String> visited) {
        this.path.clear();
        if (path != null) this.path.addAll(path);
        this.path2.clear();
        this.visited.clear();
        if (visited != null) this.visited.addAll(visited);
        progress = 0f;
        Anim.run(700, 60, t -> {
            progress = (float) Anim.easeOutCubic(t);
            repaint();
        }, null);
    }

    public void animateCompare(List<String> primaryPath, List<String> secondaryPath, List<String> visited) {
        this.path.clear();
        if (primaryPath != null) this.path.addAll(primaryPath);
        this.path2.clear();
        if (secondaryPath != null) this.path2.addAll(secondaryPath);
        this.visited.clear();
        if (visited != null) this.visited.addAll(visited);
        progress = 0f;
        Anim.run(780, 60, t -> {
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

        Color bfsC = new Color(60, 220, 255);
        Color dijC = new Color(180, 110, 255);
        Color visitC = "Dijkstra".equalsIgnoreCase(mode) ? dijC : bfsC;

        // faint map connections (real campus feel)
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 255, 255, 22));
        drawEdge(g2, pos, "Gate", "Admin");
        drawEdge(g2, pos, "Gate", "Library");
        drawEdge(g2, pos, "Gate", "Ground");
        drawEdge(g2, pos, "Ground", "Cafeteria");
        drawEdge(g2, pos, "Cafeteria", "Library");
        drawEdge(g2, pos, "Admin", "Block-A");
        drawEdge(g2, pos, "Admin", "Block-B");
        drawEdge(g2, pos, "Block-A", "Lab");
        drawEdge(g2, pos, "Block-B", "Lab");
        drawEdge(g2, pos, "Lab", "Hostel");
        drawEdge(g2, pos, "Ground", "Hostel");

        // Visited glow
        int visCount = (int) Math.floor(visited.size() * progress);
        for (int i = 0; i < visCount; i++) {
            String v = visited.get(i);
            double[] p = pos.get(v);
            if (p == null) continue;
            int a = 28 + (int) (18 * (0.5 + 0.5 * Math.sin(pulse + i)));
            g2.setColor(new Color(visitC.getRed(), visitC.getGreen(), visitC.getBlue(), a));
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
        int draw2 = (int) Math.floor(path2Edges * progress);
        if (draw2 > 0) {
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[] { 7f, 7f }, 0f));
            for (int i = 0; i < draw2; i++) {
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
        // Practical \"campus map\" layout (stable positions so routes look meaningful)
        // Coordinates are in percent of view box.
        for (String n : nodes) {
            double x = 0.5, y = 0.5;
            if ("Gate".equals(n))      { x = 0.14; y = 0.52; }
            if ("Admin".equals(n))     { x = 0.34; y = 0.36; }
            if ("Library".equals(n))   { x = 0.82; y = 0.34; }
            if ("Ground".equals(n))    { x = 0.34; y = 0.74; }
            if ("Cafeteria".equals(n)) { x = 0.62; y = 0.62; }
            if ("Block-A".equals(n))   { x = 0.56; y = 0.20; }
            if ("Block-B".equals(n))   { x = 0.50; y = 0.46; }
            if ("Lab".equals(n))       { x = 0.70; y = 0.44; }
            if ("Hostel".equals(n))    { x = 0.84; y = 0.78; }
            double px = 24 + x * (w - 48);
            double py = 22 + y * (h - 44);
            pos.put(n, new double[] { px, py });
        }
        return pos;
    }

    private static void drawEdge(Graphics2D g2, Map<String, double[]> pos, String a, String b) {
        double[] pa = pos.get(a);
        double[] pb = pos.get(b);
        if (pa == null || pb == null) return;
        g2.draw(new Line2D.Double(pa[0], pa[1], pb[0], pb[1]));
    }

    private double[] avatarPos(Map<String, double[]> pos) {
        if (path.size() < 2) return null;
        int edges = path.size() - 1;
        double t = Math.max(0.0, Math.min(1.0, progress));
        double scaled = t * edges;
        int seg = (int) Math.floor(scaled);
        if (seg >= edges) seg = edges - 1;
        double local = scaled - seg;

        double[] a = pos.get(path.get(seg));
        double[] b = pos.get(path.get(seg + 1));
        if (a == null || b == null) return null;
        return new double[] { a[0] + (b[0] - a[0]) * local, a[1] + (b[1] - a[1]) * local };
    }
}
