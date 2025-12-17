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
    private final GraphView graph = new GraphView();

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

        JLabel sub = new JLabel("BFS = fewest hops • Dijkstra = minimum distance (weighted) • Visual route + visited order");
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

        JPanel top = new RoundedCard();
        top.setLayout(null);

        JLabel l1 = new JLabel("From");
        l1.setForeground(Theme.MUTED);
        JLabel l2 = new JLabel("To");
        l2.setForeground(Theme.MUTED);

        PillToggle bBfs = new PillToggle("BFS");
        PillToggle bDij = new PillToggle("Dijkstra");
        setAlgoButtons(bBfs, bDij);

        bBfs.addActionListener(e -> {
            algorithm = "BFS";
            setAlgoButtons(bBfs, bDij);
            out.setText("Algorithm: BFS (hops)   •   Pick two locations and click Compute Route.");
        });
        bDij.addActionListener(e -> {
            algorithm = "Dijkstra";
            setAlgoButtons(bBfs, bDij);
            out.setText("Algorithm: Dijkstra (weighted)   •   Pick two locations and click Compute Route.");
        });

        ModernButton run = new ModernButton("Compute Route", Theme.ACCENT, Theme.ACCENT_2);
        run.addActionListener(e -> compute());

        ModernButton cmp = new ModernButton("Compare", Theme.CARD, Theme.CARD_2);
        cmp.addActionListener(e -> compare());

        out.setForeground(Theme.TEXT);
        out.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        compare.setForeground(Theme.MUTED);
        compare.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        top.add(l1);
        top.add(l2);
        top.add(src);
        top.add(dst);
        top.add(bBfs);
        top.add(bDij);
        top.add(run);
        top.add(cmp);
        top.add(out);
        top.add(compare);

        graph.setPreferredSize(new Dimension(10, 10));
        JPanel graphWrap = new JPanel(new BorderLayout());
        graphWrap.setOpaque(false);
        graphWrap.add(graph, BorderLayout.CENTER);

        p.add(top);
        p.add(graphWrap);

        p.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = p.getWidth();
                int h = p.getHeight();
                top.setBounds(0, 0, w, 132);
                graphWrap.setBounds(0, 146, w, h - 146);

                l1.setBounds(18, 18, 80, 18);
                src.setBounds(18, 40, 220, 34);
                l2.setBounds(252, 18, 80, 18);
                dst.setBounds(252, 40, 220, 34);

                bBfs.setBounds(w - 360, 38, 76, 36);
                bDij.setBounds(w - 276, 38, 110, 36);

                cmp.setBounds(w - 260, 84, 96, 34);
                run.setBounds(w - 152, 38, 140, 36);
                out.setBounds(18, 88, w - 290, 22);
                compare.setBounds(18, 108, w - 36, 18);
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
            graph.animateResult(java.util.Collections.emptyList(), java.util.Collections.emptyList());
            return;
        }

        int hops = JsonMini.asInt(o.get("hops"), -1);
        int cost = JsonMini.asInt(o.get("cost"), -1);
        java.util.List<String> path = JsonMini.arrStrings(o.get("path"));
        java.util.List<String> visited = JsonMini.arrStrings(o.get("visited"));

        if ("BFS".equals(JsonMini.asString(o.get("algorithm")))) {
            out.setText("BFS: hops " + hops + " • cost " + cost + "   •   Path: " + String.join(" → ", path));
        } else {
            out.setText("Dijkstra: cost " + cost + " • hops " + hops + "   •   Path: " + String.join(" → ", path));
        }
        compare.setText("Tip: click Compare to see both algorithms at once.");
        graph.animateResult(path, visited);
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
        java.util.List<String> visited = JsonMini.arrStrings(dij.get("visited")); // dijkstra visited looks nicer for demo

        out.setText("Compare: BFS(hops " + bfsHops + ", cost " + bfsCost + ") vs Dijkstra(cost " + dijCost + ", hops " + dijHops + ")");
        String winner = (dijCost >= 0 && bfsCost >= 0 && dijCost <= bfsCost) ? "Dijkstra wins for cost" : "BFS may be shorter in hops";
        compare.setText(winner + "  •  Purple=primary (current toggle), Cyan=dashed=other");

        // Primary = currently selected algorithm, secondary = other
        boolean primaryIsBfs = "BFS".equals(algorithm);
        java.util.List<String> primary = primaryIsBfs ? bfsPath : dijPath;
        java.util.List<String> secondary = primaryIsBfs ? dijPath : bfsPath;
        graph.animateCompare(primary, secondary, visited);
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
