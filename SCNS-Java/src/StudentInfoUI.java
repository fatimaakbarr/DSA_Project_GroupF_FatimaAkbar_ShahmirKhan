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
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class StudentInfoUI extends JPanel {
    private final NativeBridge nb;
    private final JLayeredPane layers;

    private final JTextField roll = field("Roll (e.g. 101)");
    private final JTextField name = field("Name");
    private final JTextField program = field("Program (e.g. BSCS)");
    private final JTextField year = field("Semester (1-8)");

    private final FolderCabinetView cabinet = new FolderCabinetView();

    public StudentInfoUI(NativeBridge nb, JLayeredPane layers) {
        this.nb = nb;
        this.layers = layers;

        setOpaque(false);
        setLayout(null);

        // Student module now uses file-cabinet metaphor (no table).

        // Input hardening (prevents strings in numeric fields)
        applyNumericFilter(roll, 9);  // roll up to 9 digits
        applyNumericFilter(year, 1);  // semester 1 digit (1-8)
        applyTokenFilter(program, 20); // letters/spaces/dash only
        applyNameFilter(name, 50);     // letters/spaces/dot only

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

        JLabel hint = new JLabel("Roll is unique (AVL key). Add prevents overwrite. Search is O(log n). List is AVL in-order sorted.");
        hint.setForeground(Theme.MUTED);
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 12f));

        ModernButton save = new ModernButton("Add Student", Theme.ACCENT, Theme.ACCENT_2);
        ModernButton search = new ModernButton("Search", Theme.CARD, Theme.CARD_2);
        ModernButton del = new ModernButton("Delete", Theme.DANGER, Theme.ACCENT);
        ModernButton refresh = new ModernButton("Refresh", Theme.CARD, Theme.CARD_2);
        ModernButton imp = new ModernButton("Import CSV", Theme.CARD, Theme.CARD_2);
        ModernButton exp = new ModernButton("Export CSV", Theme.CARD, Theme.CARD_2);

        save.addActionListener(e -> save());
        search.addActionListener(e -> search());
        del.addActionListener(e -> delete());
        refresh.addActionListener(e -> refresh());
        imp.addActionListener(e -> importCsv());
        exp.addActionListener(e -> exportCsv());

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
        JPanel buttons = new JPanel(new GridLayout(3, 2, 10, 10));
        buttons.setOpaque(false);
        buttons.add(save);
        buttons.add(search);
        buttons.add(del);
        buttons.add(refresh);
        buttons.add(imp);
        buttons.add(exp);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
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

        // Push everything up (prevents dead space at top on tall windows)
        gc.gridy++;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        form.add(javax.swing.Box.createVerticalGlue(), gc);

        JScrollPane sp = new JScrollPane(cabinet);
        UIStyle.scrollPane(sp);
        sp.getViewport().setBackground(Theme.CARD);

        JPanel listCard = new CardPanel();
        listCard.setLayout(new BorderLayout());
        JLabel tl = new JLabel("Record Cabinet (sorted by roll)");
        tl.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        tl.setForeground(Theme.TEXT);
        tl.setFont(tl.getFont().deriveFont(Font.BOLD, 13f));
        listCard.add(tl, BorderLayout.NORTH);
        listCard.add(sp, BorderLayout.CENTER);

        p.add(form);
        p.add(listCard);

        p.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = p.getWidth();
                int h = p.getHeight();

                int leftW = Math.max(340, (int) (w * 0.40));
                form.setBounds(0, 0, leftW, h);
                listCard.setBounds(leftW + 16, 0, w - leftW - 16, h);
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
        Integer r = parseIntStrict(roll.getText());
        Integer y = parseIntStrict(year.getText());
        String n = name.getText().trim();
        String p = program.getText().trim().toUpperCase();

        if (r == null || r <= 0) { Toast.show(layers, "Invalid roll number (digits only).", Theme.DANGER); return; }
        if (n.isEmpty()) { Toast.show(layers, "Name is required.", Theme.DANGER); return; }
        if (p.isEmpty()) { Toast.show(layers, "Program is required.", Theme.DANGER); return; }
        if (y == null || y < 1 || y > 8) { Toast.show(layers, "Semester must be a number between 1 and 8.", Theme.DANGER); return; }
        if (n.length() > 50) { Toast.show(layers, "Name is too long (max 50 chars).", Theme.DANGER); return; }
        if (p.length() > 20) { Toast.show(layers, "Program is too long (max 20 chars).", Theme.DANGER); return; }

        String res = nb.sisUpsertStudent(r, n, p, y);
        Map<String, String> o = JsonMini.obj(res);
        if (JsonMini.asBool(o.get("ok"))) {
            Toast.show(layers, JsonMini.asString(o.getOrDefault("message", "Student added.")), Theme.OK);
            // animate insert into cabinet
            String js = nb.sisGetStudent(r);
            Map<String, String> so = JsonMini.obj(js);
            FolderCabinetView.Record rr = new FolderCabinetView.Record();
            rr.roll = JsonMini.asInt(so.get("roll"), r);
            rr.name = JsonMini.asString(so.get("name"));
            rr.program = JsonMini.asString(so.get("program"));
            rr.semester = JsonMini.asInt(so.get("year"), y);
            rr.present = JsonMini.asInt(so.get("present"), 0);
            rr.total = JsonMini.asInt(so.get("total"), 0);
            // refresh list and animate reflow
            refresh(true);
            cabinet.animateInsert(rr, 0);
        } else {
            Toast.show(layers, JsonMini.asString(o.getOrDefault("message", "Insert failed.")), Theme.DANGER);
        }
    }

    private void importCsv() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setDialogTitle("Import students CSV");
        int res = fc.showOpenDialog(this);
        if (res != javax.swing.JFileChooser.APPROVE_OPTION) return;
        java.io.File f = fc.getSelectedFile();
        if (f == null) return;
        Map<String, String> o = JsonMini.obj(nb.sisImportCsv(f.getAbsolutePath()));
        Toast.show(layers, JsonMini.asString(o.getOrDefault("message", "OK")), JsonMini.asBool(o.get("ok")) ? Theme.OK : Theme.DANGER);
        // conveyor belt feel: reflow in
        refresh(true);
    }

    private void exportCsv() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setDialogTitle("Export students CSV");
        int res = fc.showSaveDialog(this);
        if (res != javax.swing.JFileChooser.APPROVE_OPTION) return;
        java.io.File f = fc.getSelectedFile();
        if (f == null) return;
        Map<String, String> o = JsonMini.obj(nb.sisExportCsv(f.getAbsolutePath()));
        Toast.show(layers, JsonMini.asString(o.getOrDefault("message", "OK")), JsonMini.asBool(o.get("ok")) ? Theme.OK : Theme.DANGER);
        // tiny \"fly to folder\" pulse
        cabinet.animateExportPulse();
    }

    private void search() {
        Integer r = parseIntStrict(roll.getText());
        if (r == null || r <= 0) { Toast.show(layers, "Enter a valid numeric roll to search.", Theme.DANGER); return; }

        String json = nb.sisGetStudentTrace(r);
        if (json == null || json.trim().isEmpty()) {
            Toast.show(layers, "Student not found.", Theme.DANGER);
            return;
        }
        Map<String, String> o = JsonMini.obj(json);
        name.setText(JsonMini.asString(o.get("name")));
        program.setText(JsonMini.asString(o.get("program")));
        year.setText(String.valueOf(JsonMini.asInt(o.get("year"), 1)));
        Toast.show(layers, "Record loaded.", Theme.OK);
        java.util.List<Integer> trace = JsonMini.arrInts(o.get("visited"));
        cabinet.animateSearchTrace(trace, r);
    }

    private void delete() {
        Integer r = parseIntStrict(roll.getText());
        if (r == null || r <= 0) { Toast.show(layers, "Enter a valid numeric roll to delete.", Theme.DANGER); return; }
        String json = nb.sisDeleteStudent(r);
        Map<String, String> o = JsonMini.obj(json);
        if (JsonMini.asBool(o.get("ok"))) Toast.show(layers, JsonMini.asString(o.get("message")), Theme.OK);
        else Toast.show(layers, JsonMini.asString(o.get("message")), Theme.DANGER);
        if (JsonMini.asBool(o.get("ok"))) cabinet.animateDelete(r);
        refresh(true);
    }

    private void refresh() { refresh(false); }

    private void refresh(boolean animate) {
        cabinet.clearSearch();
        List<Map<String, String>> arr = JsonMini.arrObjects(nb.sisListStudents());
        List<FolderCabinetView.Record> recs = new ArrayList<>();
        for (Map<String, String> o : arr) {
            FolderCabinetView.Record r = new FolderCabinetView.Record();
            r.roll = JsonMini.asInt(o.get("roll"), 0);
            r.name = JsonMini.asString(o.get("name"));
            r.program = JsonMini.asString(o.get("program"));
            r.semester = JsonMini.asInt(o.get("year"), 1);
            r.present = JsonMini.asInt(o.get("present"), 0);
            r.total = JsonMini.asInt(o.get("total"), 0);
            recs.add(r);
        }
        cabinet.setRecords(recs, animate);
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

    private static Integer parseIntStrict(String s) {
        try {
            String t = s == null ? "" : s.trim();
            if (t.isEmpty()) return null;
            return Integer.parseInt(t);
        } catch (Exception e) {
            return null;
        }
    }

    private static void applyNumericFilter(JTextField f, int maxLen) {
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                replace(fb, offset, 0, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) text = "";
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = cur.substring(0, offset) + text + cur.substring(offset + length);
                if (next.length() > maxLen) return;
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c < '0' || c > '9') return;
                }
                super.replace(fb, offset, length, text, attrs);
            }
        });
    }

    private static void applyTokenFilter(JTextField f, int maxLen) {
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                replace(fb, offset, 0, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) text = "";
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = cur.substring(0, offset) + text + cur.substring(offset + length);
                if (next.length() > maxLen) return;
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == ' ' || c == '-' ;
                    if (!ok) return;
                }
                super.replace(fb, offset, length, text, attrs);
            }
        });
    }

    private static void applyNameFilter(JTextField f, int maxLen) {
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                replace(fb, offset, 0, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text == null) text = "";
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = cur.substring(0, offset) + text + cur.substring(offset + length);
                if (next.length() > maxLen) return;
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == ' ' || c == '.' ;
                    if (!ok) return;
                }
                super.replace(fb, offset, length, text, attrs);
            }
        });
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
