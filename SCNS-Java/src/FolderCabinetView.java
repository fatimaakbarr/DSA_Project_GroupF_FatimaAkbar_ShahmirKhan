import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

// File-cabinet style record list with animated folders.
public class FolderCabinetView extends JComponent {
    public static final class Record {
        public int roll;
        public String name;
        public String program;
        public int semester;
        public int present;
        public int total;
    }

    private final List<Record> records = new ArrayList<>();

    private final List<Float> y = new ArrayList<>();
    private final List<Float> yTarget = new ArrayList<>();
    private final List<Float> alpha = new ArrayList<>();

    private int highlightRoll = -1;
    private float zoom = 1f;
    private float exportPulse = 0f;

    public FolderCabinetView() {
        setOpaque(false);
    }

    public void setRecords(List<Record> recs, boolean animate) {
        records.clear();
        y.clear();
        yTarget.clear();
        alpha.clear();
        if (recs != null) {
            for (Record r : recs) {
                records.add(r);
                y.add(0f);
                yTarget.add(0f);
                alpha.add(1f);
            }
        }

        layoutTargets();
        if (!animate) {
            for (int i = 0; i < y.size(); i++) y.set(i, yTarget.get(i));
            repaint();
            return;
        }

        // reflow animation
        Anim.run(520, 60, t -> {
            float k = (float) Anim.easeInOutCubic(t);
            for (int i = 0; i < y.size(); i++) {
                float a = y.get(i);
                float b = yTarget.get(i);
                y.set(i, a + (b - a) * k);
            }
            repaint();
        }, null);
    }

    public void animateInsert(Record r, int targetIndex) {
        if (r == null) return;
        // insert at beginning visually (desk), then reflow to target
        records.add(0, r);
        y.add(0, -90f);
        yTarget.add(0, 0f);
        alpha.add(0, 1f);

        layoutTargets();

        Anim.run(720, 60, t -> {
            float k = (float) Anim.easeOutCubic(t);
            for (int i = 0; i < y.size(); i++) {
                float a = y.get(i);
                float b = yTarget.get(i);
                y.set(i, a + (b - a) * k);
            }
            repaint();
        }, null);

        // Stamp effect (highlight)
        highlightRoll = r.roll;
        Anim.run(420, 60, tt -> repaint(), () -> {
            highlightRoll = -1;
            repaint();
        });
    }

    public void animateSearch(int roll) {
        highlightRoll = roll;
        zoom = 1f;
        Anim.run(260, 60, t -> {
            zoom = 1f + 0.03f * (float) Anim.easeOutCubic(t);
            repaint();
        }, null);

        // Dim non-matching
        for (int i = 0; i < records.size(); i++) {
            Record r = records.get(i);
            alpha.set(i, (r.roll == roll) ? 1f : 0.25f);
        }

        // Settle back
        Anim.run(340, 60, t -> {
            zoom = 1.03f - 0.03f * (float) Anim.easeInOutCubic(t);
            repaint();
        }, null);
    }

    public void animateSearchTrace(List<Integer> visitedRolls, int targetRoll) {
        if (visitedRolls == null || visitedRolls.isEmpty()) {
            animateSearch(targetRoll);
            return;
        }
        clearSearch();
        zoom = 1f;
        Anim.run(200, 60, t -> { zoom = 1f + 0.025f * (float) Anim.easeOutCubic(t); repaint(); }, null);

        final int[] i = {0};
        javax.swing.Timer step = new javax.swing.Timer(120, null);
        step.addActionListener(e -> {
            int idx = i[0]++;
            if (idx >= visitedRolls.size()) {
                ((javax.swing.Timer) e.getSource()).stop();
                highlightRoll = targetRoll;
                // dim non-matching
                for (int k = 0; k < records.size(); k++) alpha.set(k, records.get(k).roll == targetRoll ? 1f : 0.22f);
                Anim.run(260, 60, tt -> { zoom = 1.025f - 0.025f * (float) Anim.easeInOutCubic(tt); repaint(); }, null);
                return;
            }
            highlightRoll = visitedRolls.get(idx);
            // subtle riffle: briefly dim all, then restore
            for (int k = 0; k < alpha.size(); k++) alpha.set(k, 0.55f);
            repaint();
            new javax.swing.Timer(70, ev -> {
                ((javax.swing.Timer) ev.getSource()).stop();
                for (int k = 0; k < alpha.size(); k++) alpha.set(k, 1f);
                repaint();
            }).start();
        });
        step.start();
    }

    public void clearSearch() {
        highlightRoll = -1;
        for (int i = 0; i < alpha.size(); i++) alpha.set(i, 1f);
        zoom = 1f;
        repaint();
    }

    public void animateExportPulse() {
        exportPulse = 1f;
        Anim.run(420, 60, t -> {
            exportPulse = 1f - (float) Anim.easeOutCubic(t);
            repaint();
        }, () -> { exportPulse = 0f; repaint(); });
    }

    public void animateDelete(int roll) {
        int idx = -1;
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).roll == roll) { idx = i; break; }
        }
        if (idx < 0) return;

        int rem = idx;
        // Card pulled out with "ARCHIVED" stamp
        highlightRoll = roll;
        Anim.run(300, 60, t -> {
            float k = (float) Anim.easeInOutCubic(t);
            alpha.set(rem, 1f - k);
            // Pull card out
            y.set(rem, yTarget.get(rem) - 25f * k);
            repaint();
        }, () -> {
            records.remove(rem);
            y.remove(rem);
            yTarget.remove(rem);
            alpha.remove(rem);
            highlightRoll = -1;
            layoutTargets();
            // Drawer reflows
            Anim.run(420, 60, t2 -> {
                float k2 = (float) Anim.easeOutCubic(t2);
                for (int i = 0; i < y.size(); i++) {
                    float a = y.get(i);
                    float b = yTarget.get(i);
                    y.set(i, a + (b - a) * k2);
                }
                repaint();
            }, null);
        });
    }

    private void layoutTargets() {
        int cardH = 74;
        int gap = 12;
        int top = 16;
        for (int i = 0; i < records.size(); i++) {
            yTarget.set(i, (float) (top + i * (cardH + gap)));
            if (y.get(i) == 0f) y.set(i, yTarget.get(i));
        }
        int h = top + Math.max(0, records.size()) * (cardH + gap) + 16;
        setPreferredSize(new java.awt.Dimension(10, h));
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();

        // export folder icon (top-right)
        if (exportPulse > 0f) {
            int fx = w - 70;
            int fy = 10;
            int fa = (int) (160 * exportPulse);
            g2.setColor(new Color(Theme.ACCENT_2.getRed(), Theme.ACCENT_2.getGreen(), Theme.ACCENT_2.getBlue(), fa));
            g2.fillRoundRect(fx, fy + 8, 52, 34, 10, 10);
            g2.fillRoundRect(fx + 10, fy, 22, 16, 10, 10);
            g2.setColor(new Color(255, 255, 255, (int) (70 * exportPulse)));
            g2.drawRoundRect(fx, fy + 8, 52, 34, 10, 10);
        }

        // Card catalog drawer rail (wooden style)
        g2.setColor(new Color(180, 170, 160, 100));
        g2.fillRoundRect(8, 8, w - 16, 12, 2, 2);
        g2.setColor(new Color(140, 130, 120, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(8, 8, w - 16, 12, 2, 2);

        g2.translate(w * 0.0, 0);
        g2.scale(zoom, zoom);

        Font tabF = getFont().deriveFont(Font.BOLD, 12f);
        Font nameF = getFont().deriveFont(Font.BOLD, 15f);
        Font metaF = getFont().deriveFont(Font.PLAIN, 12f);

        int cardH = 74;
        for (int i = 0; i < records.size(); i++) {
            Record r = records.get(i);
            float yy = y.get(i);
            float a = alpha.get(i);
            paintFolder(g2, 16, (int) yy, w - 32, cardH, r, a, tabF, nameF, metaF);
        }

        g2.dispose();
    }

    private void paintFolder(Graphics2D g2, int x, int y, int w, int h, Record r, float a, Font tabF, Font nameF, Font metaF) {
        // Index card style (card catalog drawer)
        int arc = 4;
        
        // Card paper color
        Color cardColor = new Color(255, 253, 248, (int) (250 * a));
        Color inkColor = new Color(Theme.TEXT.getRed(), Theme.TEXT.getGreen(), Theme.TEXT.getBlue(), (int) (220 * a));
        Color mutedInk = new Color(Theme.MUTED.getRed(), Theme.MUTED.getGreen(), Theme.MUTED.getBlue(), (int) (180 * a));

        boolean hi = (r.roll == highlightRoll);
        
        // Highlight glow (subtle)
        if (hi) {
            g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), (int) (40 * a)));
            g2.fillRoundRect(x - 3, y - 3, w + 6, h + 6, arc + 2, arc + 2);
        }

        // Index card body
        g2.setColor(cardColor);
        g2.fillRoundRect(x, y, w, h, arc, arc);

        // Card border (ink line)
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(100, 90, 80, (int) (150 * a)));
        g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);

        // Top tab (like card catalog drawer tab)
        int tabW = 60;
        int tabH = 18;
        g2.setColor(new Color(248, 246, 240, (int) (255 * a)));
        g2.fillRoundRect(x, y, tabW, tabH, arc, arc);
        g2.setColor(new Color(100, 90, 80, (int) (150 * a)));
        g2.drawRoundRect(x, y, tabW - 1, tabH - 1, arc, arc);

        // Roll number on tab (stamped style)
        g2.setFont(tabF);
        g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), (int) (200 * a)));
        g2.drawString("#" + r.roll, x + 8, y + 14);

        // Student name (handwritten style)
        g2.setFont(nameF);
        g2.setColor(inkColor);
        String name = r.name == null ? "" : r.name;
        if (name.length() > 30) name = name.substring(0, 30) + "...";
        g2.drawString(name, x + 12, y + 38);

        // Metadata (pencil gray)
        g2.setFont(metaF);
        g2.setColor(mutedInk);
        String meta = (r.program == null ? "" : r.program) + "  •  Sem " + r.semester;
        g2.drawString(meta, x + 12, y + 56);
        
        // Attendance (small, bottom right)
        String att = r.present + "/" + r.total;
        int attW = g2.getFontMetrics().stringWidth(att);
        g2.drawString(att, x + w - attW - 12, y + h - 8);
        
        // "FOUND" stamp if highlighted
        if (hi) {
            int stampW = 70;
            int stampH = 24;
            int stampX = x + w - stampW - 8;
            int stampY = y + h - stampH - 8;
            
            g2.setColor(new Color(Theme.OK.getRed(), Theme.OK.getGreen(), Theme.OK.getBlue(), (int) (180 * a)));
            g2.fillRoundRect(stampX, stampY, stampW, stampH, 3, 3);
            g2.setColor(new Color(Theme.OK.getRed(), Theme.OK.getGreen(), Theme.OK.getBlue(), (int) (255 * a)));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(stampX, stampY, stampW, stampH, 3, 3);
            
            g2.setColor(Theme.CARD);
            g2.setFont(getFont().deriveFont(Font.BOLD, 9f));
            g2.drawString("FOUND", stampX + 12, stampY + 16);
        }
    }
}
