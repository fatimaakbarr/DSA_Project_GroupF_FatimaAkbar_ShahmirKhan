import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class AttendanceUI extends JPanel {
    private final NativeBridge nb;
    private final JLayeredPane layers;

    private final JTextField roll = field("Roll");
    private final JTextField threshold = field("Defaulter threshold % (e.g. 75)");

    private final ProgressRing ring = new ProgressRing();

    private final DefaultTableModel model = new DefaultTableModel(new Object[] { "Roll", "Name", "Present", "Total", "%" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    public AttendanceUI(NativeBridge nb, JLayeredPane layers) {
        this.nb = nb;
        this.layers = layers;

        setOpaque(false);
        setLayout(null);

        JComponent header = header();
        JComponent body = body();
        add(header);
        add(body);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int pad = 24;
                header.setBounds(pad, pad, getWidth() - pad * 2, 72);
                body.setBounds(pad, pad + 84, getWidth() - pad * 2, getHeight() - (pad + 84) - pad);
            }
        });

        ring.setPercent(0);
        threshold.setText("75");
    }

    private JComponent header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel title = new JLabel("Attendance Management");
        title.setForeground(Theme.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel sub = new JLabel("Queue/Array (events + sessions) • Min-Heap (defaulters priority)");
        sub.setForeground(Theme.MUTED);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new javax.swing.BoxLayout(left, javax.swing.BoxLayout.Y_AXIS));
        left.add(title);
        left.add(sub);

        p.add(left, BorderLayout.WEST);
        return p;
    }

    private JComponent body() {
        JPanel p = new JPanel(null);
        p.setOpaque(false);

        JPanel controls = new CardPanel();
        controls.setLayout(null);

        ModernButton newDay = new ModernButton("New Day", Theme.CARD, Theme.CARD_2);
        ModernButton present = new ModernButton("Mark Present", Theme.ACCENT, Theme.ACCENT_2);
        ModernButton summary = new ModernButton("Get Summary", Theme.CARD, Theme.CARD_2);
        ModernButton defaulters = new ModernButton("Show Defaulters", Theme.DANGER, Theme.ACCENT);

        newDay.addActionListener(e -> {
            Map<String, String> o = JsonMini.obj(nb.attNewSessionDay());
            Toast.show(layers, JsonMini.asString(o.getOrDefault("message", "OK")), JsonMini.asBool(o.get("ok")) ? Theme.OK : Theme.DANGER);
        });
        present.addActionListener(e -> markPresent());
        summary.addActionListener(e -> showSummary());
        defaulters.addActionListener(e -> showDefaulters());

        controls.add(roll);
        controls.add(newDay);
        controls.add(present);
        controls.add(summary);
        controls.add(threshold);
        controls.add(defaulters);

        JPanel ringCard = new CardPanel();
        ringCard.setLayout(new BorderLayout());
        JLabel rl = new JLabel("Live Attendance Ring");
        rl.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        rl.setForeground(Theme.TEXT);
        rl.setFont(rl.getFont().deriveFont(Font.BOLD, 13f));
        ringCard.add(rl, BorderLayout.NORTH);
        ringCard.add(ring, BorderLayout.CENTER);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.CARD);

        JPanel listCard = new CardPanel();
        listCard.setLayout(new BorderLayout());
        JLabel tl = new JLabel("Defaulters (min-heap priority)");
        tl.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        tl.setForeground(Theme.TEXT);
        tl.setFont(tl.getFont().deriveFont(Font.BOLD, 13f));
        listCard.add(tl, BorderLayout.NORTH);
        listCard.add(sp, BorderLayout.CENTER);

        p.add(controls);
        p.add(ringCard);
        p.add(listCard);

        p.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = p.getWidth();
                int h = p.getHeight();

                int leftW = Math.max(360, (int) (w * 0.40));
                controls.setBounds(0, 0, leftW, 182);
                ringCard.setBounds(0, 196, leftW, h - 196);

                listCard.setBounds(leftW + 16, 0, w - leftW - 16, h);

                int x = 14;
                roll.setBounds(x, 14, leftW - 28, 34);
                newDay.setBounds(x, 60, 100, 34);
                present.setBounds(x + 108, 60, 128, 34);
                summary.setBounds(x + 244, 60, 112, 34);

                threshold.setBounds(x, 106, leftW - 28, 34);
                defaulters.setBounds(x, 146, leftW - 28, 34);
            }
        });

        return p;
    }

    private void markPresent() {
        int r = parseInt(roll.getText(), -1);
        if (r <= 0) { Toast.show(layers, "Enter a valid roll.", Theme.DANGER); return; }
        Map<String, String> o = JsonMini.obj(nb.attMarkPresent(r));
        Toast.show(layers, JsonMini.asString(o.getOrDefault("message", "OK")), JsonMini.asBool(o.get("ok")) ? Theme.OK : Theme.DANGER);
        showSummary();
    }

    private void showSummary() {
        int r = parseInt(roll.getText(), -1);
        if (r <= 0) { Toast.show(layers, "Enter a roll to view summary.", Theme.DANGER); return; }
        String json = nb.attGetSummary(r);
        if (json == null || json.trim().isEmpty()) {
            Toast.show(layers, "Roll not found.", Theme.DANGER);
            ring.setPercent(0);
            return;
        }
        Map<String, String> o = JsonMini.obj(json);
        int pct = JsonMini.asInt(o.get("percent"), 0);
        ring.setPercent(pct);
        Toast.show(layers, JsonMini.asString(o.get("name")) + " • " + pct + "%", Theme.OK);
    }

    private void showDefaulters() {
        int min = parseInt(threshold.getText(), 75);
        min = Math.max(0, Math.min(100, min));

        model.setRowCount(0);
        List<Map<String, String>> arr = JsonMini.arrObjects(nb.attGetDefaulters(min));
        for (Map<String, String> o : arr) {
            model.addRow(new Object[] {
                    JsonMini.asInt(o.get("roll"), 0),
                    JsonMini.asString(o.get("name")),
                    JsonMini.asInt(o.get("present"), 0),
                    JsonMini.asInt(o.get("total"), 0),
                    JsonMini.asInt(o.get("percent"), 0)
            });
        }
        Toast.show(layers, "Loaded " + arr.size() + " defaulters below " + min + "%.", Theme.OK);
    }

    private static JTextField field(String placeholder) {
        JTextField f = new JTextField();
        f.setOpaque(true);
        f.setForeground(Theme.TEXT);
        f.setCaretColor(Theme.TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255, 30), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.setToolTipText(placeholder);
        return f;
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static final class CardPanel extends JPanel {
        CardPanel() { setOpaque(false); }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new java.awt.Color(0, 0, 0, 80));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(420, 220);
        }
    }
}
