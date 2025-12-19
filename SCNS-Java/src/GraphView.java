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

    // Race mode (BFS vs Dijkstra simultaneously)
    private boolean raceMode = false;
    private final List<String> bfsPath = new ArrayList<>();
    private final List<String> dijPath = new ArrayList<>();
    private final List<String> bfsVisited = new ArrayList<>();
    private final List<String> dijVisited = new ArrayList<>();
    private final List<Integer> bfsEdgeW = new ArrayList<>();
    private final List<Integer> dijEdgeW = new ArrayList<>();
    private int bfsVisIndex = 0, dijVisIndex = 0;
    private int bfsEdgeIndex = 0, dijEdgeIndex = 0;
    private float bfsEdgeT = 0f, dijEdgeT = 0f;
    private float loserAlpha = 1f; // fades losing trail
    private String winner = "Dijkstra";

    // Parkour-ish impact effects on nodes
    private final Map<String, Float> impact = new HashMap<>();

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
        raceMode = false;
        setMode(mode);
        impact.clear();
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
        raceMode = false;
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

    public void animateRace(
            List<String> bfsPath,
            List<String> bfsVisited,
            List<Integer> bfsEdgeWeights,
            List<String> dijPath,
            List<String> dijVisited,
            List<Integer> dijEdgeWeights,
            String winner) {
        raceMode = true;
        this.winner = (winner == null || winner.isEmpty()) ? "Dijkstra" : winner;
        this.loserAlpha = 1f;
        this.impact.clear();

        this.path.clear();
        this.path2.clear();
        this.visited.clear();
        this.edgeWeights.clear();

        this.bfsPath.clear();
        if (bfsPath != null) this.bfsPath.addAll(bfsPath);
        this.dijPath.clear();
        if (dijPath != null) this.dijPath.addAll(dijPath);

        this.bfsVisited.clear();
        if (bfsVisited != null) this.bfsVisited.addAll(bfsVisited);
        this.dijVisited.clear();
        if (dijVisited != null) this.dijVisited.addAll(dijVisited);

        this.bfsEdgeW.clear();
        if (bfsEdgeWeights != null) this.bfsEdgeW.addAll(bfsEdgeWeights);
        this.dijEdgeW.clear();
        if (dijEdgeWeights != null) this.dijEdgeW.addAll(dijEdgeWeights);

        if (routeTimer != null) routeTimer.stop();
        bfsVisIndex = 0;
        dijVisIndex = 0;
        bfsEdgeIndex = 0;
        dijEdgeIndex = 0;
        bfsEdgeT = 0f;
        dijEdgeT = 0f;
        lastTick = System.currentTimeMillis();
        routeTimer = new javax.swing.Timer(16, e -> tick());
        routeTimer.start();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long dt = Math.max(1, now - lastTick);
        lastTick = now;

        if (raceMode) {
            // visited timing
            int bfsVisStep = Math.max(1, (int) (dt / 85));
            int dijVisStep = Math.max(1, (int) (dt / 105));
            bfsVisIndex = Math.min(bfsVisited.size(), bfsVisIndex + bfsVisStep);
            dijVisIndex = Math.min(dijVisited.size(), dijVisIndex + dijVisStep);

            // edge timing (BFS uniform; Dijkstra proportional to edge weights)
            int bfsEdges = Math.max(0, bfsPath.size() - 1);
            int dijEdges = Math.max(0, dijPath.size() - 1);

            if (bfsEdges > 0 && bfsEdgeIndex < bfsEdges) {
                int segMs = 220; // uniform
                bfsEdgeT += (float) dt / (float) segMs;
                if (bfsEdgeT >= 1f) { bfsEdgeIndex++; bfsEdgeT = 0f; }
                // impact when landing on a node
                if (bfsEdgeT == 0f && bfsEdgeIndex < bfsPath.size()) triggerImpact(bfsPath.get(bfsEdgeIndex));
            }

            if (dijEdges > 0 && dijEdgeIndex < dijEdges) {
                int w = 1;
                if (dijEdgeIndex < dijEdgeW.size()) w = Math.max(1, dijEdgeW.get(dijEdgeIndex));
                int segMs = 120 + w * 45;
                dijEdgeT += (float) dt / (float) segMs;
                if (dijEdgeT >= 1f) { dijEdgeIndex++; dijEdgeT = 0f; }
                if (dijEdgeT == 0f && dijEdgeIndex < dijPath.size()) triggerImpact(dijPath.get(dijEdgeIndex));
            }

            boolean bfsDone = (bfsEdges <= 0) || (bfsEdgeIndex >= bfsEdges);
            boolean dijDone = (dijEdges <= 0) || (dijEdgeIndex >= dijEdges);
            if (bfsDone && dijDone) {
                loserAlpha -= (float) dt / 900f;
                if (loserAlpha <= 0.18f) {
                    loserAlpha = 0.18f;
                    routeTimer.stop();
                }
            }
            repaint();
            return;
        }

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
            if (edgeIndex < path.size()) triggerImpact(path.get(edgeIndex));
        }

        // decay impacts
        decayImpacts((float) dt / 1000f);
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

        if (raceMode) {
            paintRace(g2, pos);
            g2.dispose();
            return;
        }

        Color bfsC = new Color(60, 220, 255);
        Color dijC = new Color(180, 110, 255);
        Color visitC = "Dijkstra".equalsIgnoreCase(mode) ? dijC : bfsC;

        // Base map edges (main roads + key shortcuts). We don't draw every \"shortcut\" edge to avoid clutter.
        drawCampusEdge(g2, pos, "Gate", "Ground", 3);
        drawCampusEdge(g2, pos, "Ground", "Cafeteria", 3);
        drawCampusEdge(g2, pos, "Cafeteria", "Library", 3);
        drawCampusEdge(g2, pos, "Gate", "Admin", 12);
        drawCampusEdge(g2, pos, "Admin", "Library", 12);
        drawCampusEdge(g2, pos, "Ground", "Admin", 2);
        drawCampusEdge(g2, pos, "Admin", "Block-A", 2);
        drawCampusEdge(g2, pos, "Admin", "Block-B", 2);
        drawCampusEdge(g2, pos, "Block-A", "Lab", 2);
        drawCampusEdge(g2, pos, "Block-B", "Lab", 2);
        drawCampusEdge(g2, pos, "Lab", "Gym", 2);
        drawCampusEdge(g2, pos, "Gym", "Dorms", 2);
        drawCampusEdge(g2, pos, "Dorms", "Hostel", 2);
        drawCampusEdge(g2, pos, "Ground", "Hostel", 3);

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

        // nodes (with impact bounce + dust puff)
        Font f = getFont().deriveFont(Font.BOLD, 12f);
        g2.setFont(f);
        for (String n : nodes) {
            double[] p = pos.get(n);
            if (p == null) continue;
            boolean inPath = path.contains(n);
            boolean inPath2 = path2.contains(n);
            boolean isVisited = visited.contains(n);

            Color ring = inPath ? Theme.ACCENT : (inPath2 ? visitC : (isVisited ? visitC : new Color(90, 105, 140)));
            Color fill = new Color(12, 14, 24);

            float it = impact.getOrDefault(n, 0f);
            double bx = p[0];
            double by = p[1] - 5 * Math.sin(Math.min(1, it) * Math.PI); // bounce

            // dust puff
            if (it > 0f) {
                int da = (int) (70 * it);
                g2.setColor(new Color(255, 255, 255, da));
                g2.fill(new Ellipse2D.Double(bx - 18, by + 10, 10, 6));
                g2.fill(new Ellipse2D.Double(bx + 6, by + 10, 12, 7));
            }

            g2.setColor(fill);
            g2.fill(new Ellipse2D.Double(bx - 12, by - 12, 24, 24));

            g2.setStroke(new BasicStroke(2.2f));
            g2.setColor(ring);
            g2.draw(new Ellipse2D.Double(bx - 12, by - 12, 24, 24));

            g2.setColor(Theme.TEXT);
            g2.drawString(n, (int) bx - 14, (int) by - 16);
        }

        g2.dispose();
    }

    private void paintRace(Graphics2D g2, Map<String, double[]> pos) {
        Color bfsC = new Color(60, 220, 255);
        Color dijC = new Color(180, 110, 255);

        // Base map edges (weight-based styling)
        drawCampusEdge(g2, pos, "Gate", "Admin", 12);
        drawCampusEdge(g2, pos, "Admin", "Library", 12);
        drawCampusEdge(g2, pos, "Gate", "Ground", 3);
        drawCampusEdge(g2, pos, "Ground", "Cafeteria", 3);
        drawCampusEdge(g2, pos, "Cafeteria", "Library", 3);
        drawCampusEdge(g2, pos, "Ground", "Admin", 2);
        drawCampusEdge(g2, pos, "Admin", "Block-A", 2);
        drawCampusEdge(g2, pos, "Admin", "Block-B", 2);
        drawCampusEdge(g2, pos, "Block-A", "Lab", 2);
        drawCampusEdge(g2, pos, "Block-B", "Lab", 2);
        drawCampusEdge(g2, pos, "Lab", "Gym", 2);
        drawCampusEdge(g2, pos, "Gym", "Dorms", 2);
        drawCampusEdge(g2, pos, "Dorms", "Hostel", 2);
        drawCampusEdge(g2, pos, "Ground", "Hostel", 3);

        boolean bfsWins = "BFS".equalsIgnoreCase(winner);
        float bfsA = bfsWins ? 1f : loserAlpha;
        float dijA = bfsWins ? loserAlpha : 1f;

        // visited trails
        drawVisitedTrail(g2, pos, bfsVisited, bfsVisIndex, bfsC, bfsA);
        drawVisitedTrail(g2, pos, dijVisited, dijVisIndex, dijC, dijA);

        // path paint
        drawProgressPath(g2, pos, bfsPath, bfsEdgeIndex, bfsEdgeT, bfsC, 4.6f, bfsA, false);
        drawProgressPath(g2, pos, dijPath, dijEdgeIndex, dijEdgeT, dijC, 2.8f, dijA, true);

        // runner dots
        double[] bp = raceAvatar(pos, bfsPath, bfsEdgeIndex, bfsEdgeT);
        if (bp != null) drawRunner(g2, bp[0], bp[1], bfsC, bfsA);
        double[] dp = raceAvatar(pos, dijPath, dijEdgeIndex, dijEdgeT);
        if (dp != null) drawRunner(g2, dp[0], dp[1], dijC, dijA);

        // Nodes on top
        Font f = getFont().deriveFont(Font.BOLD, 12f);
        g2.setFont(f);
        for (String n : nodes) {
            double[] p = pos.get(n);
            if (p == null) continue;
            boolean inB = bfsPath.contains(n);
            boolean inD = dijPath.contains(n);
            Color ring = inD ? new Color(dijC.getRed(), dijC.getGreen(), dijC.getBlue(), 220) : (inB ? new Color(bfsC.getRed(), bfsC.getGreen(), bfsC.getBlue(), 220) : new Color(90, 105, 140));
            g2.setColor(new Color(12, 14, 24));
            g2.fill(new Ellipse2D.Double(p[0] - 12, p[1] - 12, 24, 24));
            g2.setStroke(new BasicStroke(2.2f));
            g2.setColor(ring);
            g2.draw(new Ellipse2D.Double(p[0] - 12, p[1] - 12, 24, 24));
            g2.setColor(Theme.TEXT);
            g2.drawString(n, (int) p[0] - 14, (int) p[1] - 16);
        }
    }

    private static void drawVisitedTrail(Graphics2D g2, Map<String, double[]> pos, List<String> v, int count, Color c, float alphaMul) {
        int visCount = Math.min(v.size(), count);
        for (int i = 0; i < visCount; i++) {
            String name = v.get(i);
            double[] p = pos.get(name);
            if (p == null) continue;
            int age = (visCount - 1) - i;
            int a = Math.max(10, 70 - age * 12);
            a = (int) (a * alphaMul);
            double rr = 15;
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(120, a)));
            g2.fill(new Ellipse2D.Double(p[0] - rr, p[1] - rr, rr * 2, rr * 2));
        }
    }

    private static void drawProgressPath(Graphics2D g2, Map<String, double[]> pos, List<String> path, int edgeIndex, float edgeT, Color c, float stroke, float alphaMul, boolean dashed) {
        int edges = Math.max(0, path.size() - 1);
        if (edges <= 0) return;
        if (dashed) g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[] { 6f, 8f }, 0f));
        else g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (220 * alphaMul)));

        for (int i = 0; i < Math.min(edgeIndex, edges); i++) {
            double[] a = pos.get(path.get(i));
            double[] b = pos.get(path.get(i + 1));
            if (a == null || b == null) continue;
            g2.draw(new Line2D.Double(a[0], a[1], b[0], b[1]));
        }
        if (edgeIndex < edges) {
            double[] a = pos.get(path.get(edgeIndex));
            double[] b = pos.get(path.get(edgeIndex + 1));
            if (a != null && b != null) {
                double x = a[0] + (b[0] - a[0]) * Math.max(0, Math.min(1, edgeT));
                double y = a[1] + (b[1] - a[1]) * Math.max(0, Math.min(1, edgeT));
                g2.draw(new Line2D.Double(a[0], a[1], x, y));
            }
        }
    }

    private static void drawRunner(Graphics2D g2, double x, double y, Color c, float aMul) {
        double glow = 9;
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (120 * aMul)));
        g2.fill(new Ellipse2D.Double(x - glow, y - glow, glow * 2, glow * 2));
        g2.setColor(new Color(255, 255, 255, (int) (230 * aMul)));
        g2.fill(new Ellipse2D.Double(x - 3.5, y - 3.5, 7, 7));
    }

    private static double[] raceAvatar(Map<String, double[]> pos, List<String> path, int edgeIndex, float edgeT) {
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
            if ("Gym".equals(n))       { x = 0.52; y = 0.72; }
            if ("Dorms".equals(n))     { x = 0.66; y = 0.80; }
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

    private void triggerImpact(String node) {
        if (node == null) return;
        impact.put(node, 1f);
    }

    private void decayImpacts(float dtSec) {
        if (impact.isEmpty()) return;
        for (String k : new ArrayList<>(impact.keySet())) {
            float v = impact.getOrDefault(k, 0f);
            v -= dtSec * 2.4f;
            if (v <= 0f) impact.remove(k);
            else impact.put(k, v);
        }
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
