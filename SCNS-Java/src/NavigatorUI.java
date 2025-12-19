import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

public class NavigatorUI extends JPanel {
    private final NativeBridge nb;
    private final JLayeredPane layers;

    private final JComboBox<String> src;
    private final JComboBox<String> dst;
    private String algorithm = "Dijkstra";

    private final JLabel out = new JLabel("Pick two locations to compute the shortest route.");
    private final JLabel compare = new JLabel(" ");
    private final JLabel explain = new JLabel(" ");
    private final ChipRow chips = new ChipRow();
    private final GraphView graph = new GraphView();

    // replay cache
    private java.util.List<String> lastBfsPath, lastDijPath, lastBfsVisited, lastDijVisited;
    private java.util.List<Integer> lastBfsEdgeW, lastDijEdgeW;
    private String lastWinner = "Dijkstra";

    public NavigatorUI(NativeBridge nb, JLayeredPane layers) {
        this.nb = nb;
        this.layers = layers;

        setOpaque(false);
        setLayout(null);

        String[] nodes = safe(nb.navLocations());
        src = new JComboBox<>(nodes);
        dst = new JComboBox<>(nodes);
        UIStyle.comboBox(src);
        UIStyle.comboBox(dst);

        graph.setNodes(nodes);

        JComponent header = header();
        JComponent card = card(nodes);

        add(header);
        add(card);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int pad = 24;
                header.setBounds(pad, pad, getWidth() - pad * 2, 72);
                card.setBounds(pad, pad + 84, getWidth() - pad * 2, getHeight() - (pad + 84) - pad);
            }
        });

    }

    private JComponent header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel title = new JLabel("Campus Navigator");
        title.setForeground(Theme.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel sub = new JLabel("Weighted campus graph • BFS = Fewest Stops • Dijkstra = Shortest Distance");
        sub.setForeground(Theme.MUTED);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new javax.swing.BoxLayout(left, javax.swing.BoxLayout.Y_AXIS));
        left.add(title);
        left.add(sub);

        p.add(left, BorderLayout.WEST);
        return p;
    }

    private JComponent card(String[] nodes) {
        JPanel p = new JPanel(null);
        p.setOpaque(false);

        JPanel controls = new RoundedCard();
        controls.setLayout(null);

        JLabel l1 = new JLabel("From");
        l1.setForeground(Theme.MUTED);
        JLabel l2 = new JLabel("To");
        l2.setForeground(Theme.MUTED);

        PillToggle bBfs = new PillToggle("Fewest Stops");
        PillToggle bDij = new PillToggle("Shortest Distance");
        setAlgoButtons(bBfs, bDij);

        bBfs.addActionListener(e -> {
            algorithm = "BFS";
            setAlgoButtons(bBfs, bDij);
            out.setText("Fewest Stops (BFS)   •   Pick two locations and click Compute Route.");
            explain.setText("<html><b>BFS</b> minimizes number of stops (edges). Ignores weights.</html>");
        });
        bDij.addActionListener(e -> {
            algorithm = "Dijkstra";
            setAlgoButtons(bBfs, bDij);
            out.setText("Shortest Distance (Dijkstra)   •   Pick two locations and click Compute Route.");
            explain.setText("<html><b>Dijkstra</b> minimizes total walking cost (sum of edge weights).</html>");
        });

        ModernButton run = new ModernButton("Compute Route", Theme.ACCENT, Theme.ACCENT_2);
        run.addActionListener(e -> compute());

        ModernButton cmp = new ModernButton("Compare BFS vs Dijkstra", Theme.CARD, Theme.CARD_2);
        cmp.addActionListener(e -> compare());

        ModernButton replay = new ModernButton("Replay Race", Theme.CARD, Theme.CARD_2);
        replay.addActionListener(e -> {
            if (lastBfsPath == null || lastDijPath == null) {
                Toast.show(layers, "Run Compare first to record a race.", Theme.MUTED);
                return;
            }
            graph.animateRace(lastBfsPath, lastBfsVisited, lastBfsEdgeW, lastDijPath, lastDijVisited, lastDijEdgeW, lastWinner);
        });

        out.setForeground(Theme.TEXT);
        out.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        compare.setForeground(Theme.MUTED);
        compare.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        explain.setForeground(Theme.MUTED);
        explain.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        explain.setText("<html><b>Dijkstra</b> minimizes total walking cost (sum of edge weights).</html>");

        controls.add(l1);
        controls.add(l2);
        controls.add(src);
        controls.add(dst);
        controls.add(bBfs);
        controls.add(bDij);
        controls.add(run);
        controls.add(cmp);
        controls.add(replay);
        controls.add(out);
        controls.add(compare);
        controls.add(explain);
        controls.add(chips);

        graph.setPreferredSize(new Dimension(10, 10));
        JPanel graphWrap = new JPanel(new BorderLayout());
        graphWrap.setOpaque(false);
        graphWrap.add(graph, BorderLayout.CENTER);

        p.add(controls);
        p.add(graphWrap);

        p.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = p.getWidth();
                int h = p.getHeight();
                int leftW = Math.max(420, (int) (w * 0.40));
                controls.setBounds(0, 0, leftW, h);
                graphWrap.setBounds(leftW + 16, 0, w - leftW - 16, h);

                int cx = 18;
                chips.setBounds(cx, 10, leftW - 36, 24);

                l1.setBounds(cx, 46, 120, 18);
                src.setBounds(cx, 68, leftW - 36, 36);
                l2.setBounds(cx, 112, 120, 18);
                dst.setBounds(cx, 134, leftW - 36, 36);

                int toggleY = 184;
                int toggleH = 38;
                int gap = 10;
                int bw = (leftW - 36 - gap) / 2;
                bBfs.setBounds(cx, toggleY, bw, toggleH);
                bDij.setBounds(cx + bw + gap, toggleY, bw, toggleH);

                run.setBounds(cx, toggleY + 52, leftW - 36, 40);
                cmp.setBounds(cx, toggleY + 98, leftW - 36, 36);

                replay.setBounds(cx, toggleY + 140, leftW - 36, 34);
                out.setBounds(cx, toggleY + 180, leftW - 36, 22);
                compare.setBounds(cx, toggleY + 202, leftW - 36, 18);
                explain.setBounds(cx, toggleY + 226, leftW - 36, 46);
            }
        });

        return p;
    }

    private void setAlgoButtons(PillToggle bfs, PillToggle dij) {
        bfs.setActive("BFS".equals(algorithm));
        dij.setActive(!"BFS".equals(algorithm));
    }

    private void compute() {
        String a = (String) src.getSelectedItem();
        String b = (String) dst.getSelectedItem();
        if (a == null || b == null) {
            Toast.show(layers, "Pick both source and destination.", Theme.DANGER);
            return;
        }
        if (a.equals(b)) {
            Toast.show(layers, "Source and destination must be different.", Theme.DANGER);
            return;
        }

        String json = nb.navShortestPath(a, b, algorithm);
        java.util.Map<String, String> o = JsonMini.obj(json);
        if (!JsonMini.asBool(o.get("ok"))) {
            Toast.show(layers, JsonMini.asString(o.getOrDefault("error", "Route not found.")), Theme.DANGER);
            out.setText("No route found.");
            graph.animateTraversal(java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyList(), algorithm);
            return;
        }

        int hops = JsonMini.asInt(o.get("hops"), -1);
        int cost = JsonMini.asInt(o.get("cost"), -1);
        java.util.List<String> path = JsonMini.arrStrings(o.get("path"));
        java.util.List<String> visited = JsonMini.arrStrings(o.get("visited"));
        java.util.List<Integer> edgeW = JsonMini.arrInts(o.get("edgeWeights"));

        if ("BFS".equals(JsonMini.asString(o.get("algorithm")))) {
            out.setText("BFS: hops " + hops + " • cost " + cost + "   •   Path: " + String.join(" → ", path));
        } else {
            out.setText("Dijkstra: cost " + cost + " • hops " + hops + "   •   Path: " + String.join(" → ", path));
        }
        compare.setText("Tip: click Compare to see both algorithms at once.");
        chips.setChips(new String[] { "Visited: " + visited.size(), "Hops: " + hops, "Cost: " + cost },
                new java.awt.Color[] { Theme.ACCENT_2, Theme.CARD_2, Theme.CARD_2 });
        graph.setMode(algorithm);
        graph.animateTraversal(path, visited, edgeW, algorithm);

        if ("BFS".equals(algorithm)) {
            explain.setText("<html><b>Why this route?</b><br/>BFS minimizes number of locations passed (hops).<br/>It may ignore a shorter walking-time route.</html>");
        } else {
            explain.setText("<html><b>Why this route?</b><br/>Dijkstra minimizes total walking cost (sum of edge weights).<br/>It may take more stops if the total is smaller.</html>");
        }
    }

    private void compare() {
        String a = (String) src.getSelectedItem();
        String b = (String) dst.getSelectedItem();
        if (a == null || b == null) {
            Toast.show(layers, "Pick both source and destination.", Theme.DANGER);
            return;
        }
        if (a.equals(b)) {
            Toast.show(layers, "Source and destination must be different.", Theme.DANGER);
            return;
        }

        java.util.Map<String, String> bfs = JsonMini.obj(nb.navShortestPath(a, b, "BFS"));
        java.util.Map<String, String> dij = JsonMini.obj(nb.navShortestPath(a, b, "Dijkstra"));
        if (!JsonMini.asBool(bfs.get("ok")) || !JsonMini.asBool(dij.get("ok"))) {
            Toast.show(layers, "Comparison failed (route not found).", Theme.DANGER);
            return;
        }

        int bfsHops = JsonMini.asInt(bfs.get("hops"), -1);
        int bfsCost = JsonMini.asInt(bfs.get("cost"), -1);
        int dijHops = JsonMini.asInt(dij.get("hops"), -1);
        int dijCost = JsonMini.asInt(dij.get("cost"), -1);

        java.util.List<String> bfsPath = JsonMini.arrStrings(bfs.get("path"));
        java.util.List<String> dijPath = JsonMini.arrStrings(dij.get("path"));
        java.util.List<String> bfsVisited = JsonMini.arrStrings(bfs.get("visited"));
        java.util.List<String> dijVisited = JsonMini.arrStrings(dij.get("visited"));
        java.util.List<Integer> bfsEdgeW = JsonMini.arrInts(bfs.get("edgeWeights"));
        java.util.List<Integer> dijEdgeW = JsonMini.arrInts(dij.get("edgeWeights"));

        out.setText("Algorithm Race: BFS vs Dijkstra");
        boolean costWinnerDij = (dijCost >= 0 && bfsCost >= 0 && dijCost < bfsCost);
        boolean hopWinnerBfs = (bfsHops >= 0 && dijHops >= 0 && bfsHops < dijHops);
        String winner = costWinnerDij ? "Dijkstra" : "BFS";
        compare.setText("Winner: " + winner + "  •  (Cost decides the winner; BFS can still win on hops)");

        String chip1 = costWinnerDij ? "Best cost: Dijkstra" : "Best cost: BFS path";
        String chip2 = hopWinnerBfs ? "Fewest hops: BFS" : "Fewest hops: Dijkstra";
        String chip3 = "Visited: BFS " + bfsVisited.size() + " • Dij " + dijVisited.size();
        chips.setChips(new String[] { chip1, chip2, chip3 }, new java.awt.Color[] { Theme.ACCENT, Theme.ACCENT_2, Theme.CARD_2 });

        explain.setText("<html><b>Why do they differ?</b><br/>"
                + "BFS chooses the route with <b>fewest stops</b> (hops = " + bfsHops + ").<br/>"
                + "Dijkstra chooses the route with <b>lowest total cost</b> (cost = " + dijCost + ").</html>");

        // cache for replay
        lastBfsPath = bfsPath;
        lastDijPath = dijPath;
        lastBfsVisited = bfsVisited;
        lastDijVisited = dijVisited;
        lastBfsEdgeW = bfsEdgeW;
        lastDijEdgeW = dijEdgeW;
        lastWinner = winner;

        graph.animateRace(bfsPath, bfsVisited, bfsEdgeW, dijPath, dijVisited, dijEdgeW, winner);
    }

    // Premium chips row
    private static final class ChipRow extends JComponent {
        private String[] labels = new String[0];
        private java.awt.Color[] colors = new java.awt.Color[0];

        void setChips(String[] labels, java.awt.Color[] colors) {
            this.labels = labels == null ? new String[0] : labels;
            this.colors = colors == null ? new java.awt.Color[0] : colors;
            repaint();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(getFont().deriveFont(java.awt.Font.BOLD, 11.5f));

            int x = 0;
            int y = 0;
            int h = getHeight();
            for (int i = 0; i < labels.length; i++) {
                String s = labels[i] == null ? "" : labels[i];
                int w = g2.getFontMetrics().stringWidth(s) + 18;
                java.awt.Color c = (i < colors.length && colors[i] != null) ? colors[i] : Theme.CARD_2;

                g2.setColor(new java.awt.Color(0, 0, 0, 85));
                g2.fillRoundRect(x, y, w, h, 999, 999);
                g2.setColor(new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), 140));
                g2.fillRoundRect(x, y, 6, h, 999, 999);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(x, y, w - 1, h - 1, 999, 999);

                g2.setColor(Theme.TEXT);
                g2.drawString(s, x + 12, y + h - 7);

                x += w + 10;
                if (x > getWidth() - 40) break;
            }
            g2.dispose();
        }
    }

    private static String[] safe(String[] a) {
        return a == null ? new String[0] : a;
    }

    private static final class RoundedCard extends JPanel {
        RoundedCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.GLASS);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
