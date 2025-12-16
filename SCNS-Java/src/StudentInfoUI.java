import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
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

public class StudentInfoUI extends JPanel {
    private final NativeBridge nb;
    private final JLayeredPane layers;

    private final JTextField roll = field("Roll (e.g. 101)");
    private final JTextField name = field("Name");
    private final JTextField program = field("Program (e.g. BSCS)");
    private final JTextField year = field("Year (1-8)");

    private final DefaultTableModel model = new DefaultTableModel(new Object[] { "Roll", "Name", "Program", "Year" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    private final TreeView tree = new TreeView();

    public StudentInfoUI(NativeBridge nb, JLayeredPane layers) {
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

        refresh();
    }

    private JComponent header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel title = new JLabel("Student Information System");
        title.setForeground(Theme.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JLabel sub = new JLabel("AVL Tree DSAs • insert/search/update/delete • sorted listing");
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

        JPanel form = new CardPanel();
        form.setLayout(null);

        JLabel hint = new JLabel("Tip: ‘Save’ inserts or updates the AVL record.");
        hint.setForeground(Theme.MUTED);

        ModernButton save = new ModernButton("Save", Theme.ACCENT, Theme.ACCENT_2);
        ModernButton search = new ModernButton("Search", Theme.CARD, Theme.CARD_2);
        ModernButton del = new ModernButton("Delete", Theme.DANGER, Theme.ACCENT);
        ModernButton refresh = new ModernButton("Refresh", Theme.CARD, Theme.CARD_2);

        save.addActionListener(e -> save());
        search.addActionListener(e -> search());
        del.addActionListener(e -> delete());
        refresh.addActionListener(e -> refresh());

        form.add(roll);
        form.add(name);
        form.add(program);
        form.add(year);
        form.add(save);
        form.add(search);
        form.add(del);
        form.add(refresh);
        form.add(hint);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.CARD);

        JPanel listCard = new CardPanel();
        listCard.setLayout(new BorderLayout());
        JLabel tl = new JLabel("Records (sorted by roll)");
        tl.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        tl.setForeground(Theme.TEXT);
        tl.setFont(tl.getFont().deriveFont(Font.BOLD, 13f));
        listCard.add(tl, BorderLayout.NORTH);
        listCard.add(sp, BorderLayout.CENTER);

        JPanel treeCard = new CardPanel();
        treeCard.setLayout(new BorderLayout());
        JLabel tv = new JLabel("AVL Visualization");
        tv.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        tv.setForeground(Theme.TEXT);
        tv.setFont(tv.getFont().deriveFont(Font.BOLD, 13f));
        treeCard.add(tv, BorderLayout.NORTH);
        treeCard.add(tree, BorderLayout.CENTER);

        p.add(form);
        p.add(listCard);
        p.add(treeCard);

        p.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = p.getWidth();
                int h = p.getHeight();

                int leftW = Math.max(340, (int) (w * 0.40));
                form.setBounds(0, 0, leftW, 210);
                listCard.setBounds(0, 224, leftW, h - 224);

                treeCard.setBounds(leftW + 16, 0, w - leftW - 16, h);

                int x = 14;
                int y = 14;
                int fw = leftW - 28;
                roll.setBounds(x, y, fw, 34);
                y += 42;
                name.setBounds(x, y, fw, 34);
                y += 42;
                program.setBounds(x, y, fw, 34);
                y += 42;
                year.setBounds(x, y, fw, 34);

                int by = 14;
                save.setBounds(x, by + 170, 80, 34);
                search.setBounds(x + 88, by + 170, 88, 34);
                del.setBounds(x + 184, by + 170, 84, 34);
                refresh.setBounds(x + 276, by + 170, 90, 34);

                hint.setBounds(x, by + 208, fw, 18);
            }
        });

        return p;
    }

    private void save() {
        int r = parseInt(roll.getText(), -1);
        int y = parseInt(year.getText(), 1);
        String n = name.getText().trim();
        String p = program.getText().trim();

        if (r <= 0) { Toast.show(layers, "Invalid roll number.", Theme.DANGER); return; }
        if (n.isEmpty()) { Toast.show(layers, "Name is required.", Theme.DANGER); return; }
        if (p.isEmpty()) { Toast.show(layers, "Program is required.", Theme.DANGER); return; }
        if (y < 1 || y > 8) { Toast.show(layers, "Year must be between 1 and 8.", Theme.DANGER); return; }

        String res = nb.sisUpsertStudent(r, n, p, y);
        Map<String, String> o = JsonMini.obj(res);
        Toast.show(layers, "Student " + JsonMini.asString(o.getOrDefault("action", "saved")) + ".", Theme.OK);
        refresh();
        highlightTree(r);
    }

    private void search() {
        int r = parseInt(roll.getText(), -1);
        if (r <= 0) { Toast.show(layers, "Enter a roll to search.", Theme.DANGER); return; }

        String json = nb.sisGetStudent(r);
        if (json == null || json.trim().isEmpty()) {
            Toast.show(layers, "Student not found.", Theme.DANGER);
            highlightTree(-1);
            return;
        }
        Map<String, String> o = JsonMini.obj(json);
        name.setText(JsonMini.asString(o.get("name")));
        program.setText(JsonMini.asString(o.get("program")));
        year.setText(String.valueOf(JsonMini.asInt(o.get("year"), 1)));
        Toast.show(layers, "Record loaded.", Theme.OK);
        highlightTree(r);
    }

    private void delete() {
        int r = parseInt(roll.getText(), -1);
        if (r <= 0) { Toast.show(layers, "Enter a roll to delete.", Theme.DANGER); return; }
        String json = nb.sisDeleteStudent(r);
        Map<String, String> o = JsonMini.obj(json);
        if (JsonMini.asBool(o.get("ok"))) Toast.show(layers, JsonMini.asString(o.get("message")), Theme.OK);
        else Toast.show(layers, JsonMini.asString(o.get("message")), Theme.DANGER);
        refresh();
        highlightTree(-1);
    }

    private void refresh() {
        model.setRowCount(0);
        List<Map<String, String>> arr = JsonMini.arrObjects(nb.sisListStudents());
        for (Map<String, String> o : arr) {
            model.addRow(new Object[] {
                    JsonMini.asInt(o.get("roll"), 0),
                    JsonMini.asString(o.get("name")),
                    JsonMini.asString(o.get("program")),
                    JsonMini.asInt(o.get("year"), 1)
            });
        }
        highlightTree(-1);
    }

    private void highlightTree(int rollToHighlight) {
        String snap = nb.sisTreeSnapshot();
        List<int[]> edges = parseTriples(snap);
        tree.setSnapshot(edges, rollToHighlight);
    }

    private static List<int[]> parseTriples(String json) {
        List<int[]> out = new ArrayList<>();
        if (json == null) return out;
        String s = json.trim();
        if (!s.startsWith("[") || !s.endsWith("]")) return out;
        // expected: [[n,l,r],[n,l,r],...]
        int i = 0;
        while (i < s.length()) {
            int a = s.indexOf('[', i);
            if (a < 0) break;
            int b = s.indexOf(']', a + 1);
            if (b < 0) break;
            String inner = s.substring(a + 1, b).trim();
            String[] parts = inner.split(",");
            if (parts.length >= 3) {
                int n = safeInt(parts[0]);
                int l = safeInt(parts[1]);
                int r = safeInt(parts[2]);
                out.add(new int[] { n, l, r });
            }
            i = b + 1;
        }
        return out;
    }

    private static int safeInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
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
