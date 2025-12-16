import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.GridLayout;
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

        UIStyle.table(table);

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
        form.setLayout(new GridBagLayout());

        JLabel hint = new JLabel("Roll = unique key (AVL). Save inserts/updates. Search loads by roll. Delete removes by roll.");
        hint.setForeground(Theme.MUTED);
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 12f));

        ModernButton save = new ModernButton("Save", Theme.ACCENT, Theme.ACCENT_2);
        ModernButton search = new ModernButton("Search", Theme.CARD, Theme.CARD_2);
        ModernButton del = new ModernButton("Delete", Theme.DANGER, Theme.ACCENT);
        ModernButton refresh = new ModernButton("Refresh", Theme.CARD, Theme.CARD_2);

        save.addActionListener(e -> save());
        search.addActionListener(e -> search());
        del.addActionListener(e -> delete());
        refresh.addActionListener(e -> refresh());

        // Field labels (so students understand what each field is for)
        JLabel lRoll = label("Roll Number");
        JLabel lName = label("Student Name");
        JLabel lProg = label("Program");
        JLabel lYear = label("Year/Semester");

        roll.setToolTipText("Unique student roll number (AVL key). Example: 101");
        name.setToolTipText("Student full name (stored in AVL record).");
        program.setToolTipText("Degree/program name. Example: BSCS");
        year.setToolTipText("Year/Semester number (1-8).");

        // Button grid (always aligned)
        JPanel buttons = new JPanel(new GridLayout(2, 2, 10, 10));
        buttons.setOpaque(false);
        buttons.add(save);
        buttons.add(search);
        buttons.add(del);
        buttons.add(refresh);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(10, 12, 4, 12);
        form.add(lRoll, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 12, 10, 12);
        form.add(roll, gc);

        gc.gridy++;
        gc.insets = new Insets(2, 12, 4, 12);
        form.add(lName, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 12, 10, 12);
        form.add(name, gc);

        gc.gridy++;
        gc.insets = new Insets(2, 12, 4, 12);
        form.add(lProg, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 12, 10, 12);
        form.add(program, gc);

        gc.gridy++;
        gc.insets = new Insets(2, 12, 4, 12);
        form.add(lYear, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 12, 12, 12);
        form.add(year, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 12, 10, 12);
        form.add(buttons, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 12, 12, 12);
        form.add(hint, gc);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.CARD);
        table.setBackground(Theme.CARD);
        table.setForeground(Theme.TEXT);

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
                int formH = 360;
                int gap = 14;
                form.setBounds(0, 0, leftW, formH);
                listCard.setBounds(0, formH + gap, leftW, h - (formH + gap));

                treeCard.setBounds(leftW + 16, 0, w - leftW - 16, h);
            }
        });

        return p;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.MUTED);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        return l;
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
        List<int[]> edges = JsonMini.arrIntTriples(snap);
        tree.setSnapshot(edges, rollToHighlight);
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
            g2.setColor(Theme.GLASS);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(420, 220);
        }
    }
}
