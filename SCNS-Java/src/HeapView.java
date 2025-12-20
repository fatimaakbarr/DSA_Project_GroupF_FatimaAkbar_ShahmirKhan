import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

// Mini heap visualization: shows top defaulters as flagged priority files.
public class HeapView extends JComponent {
    public static final class Item {
        public int roll;
        public String name;
        public int percent;
    }

    private final List<Item> items = new ArrayList<>();
    private float pop = 0f;

    public HeapView() {
        setOpaque(false);
    }

    public void setItems(List<Item> list, boolean animatePop) {
        items.clear();
        if (list != null) items.addAll(list);
        if (animatePop) {
            pop = 1f;
            Anim.run(340, 60, t -> { pop = 1f - (float) Anim.easeOutCubic(t); repaint(); }, () -> { pop = 0f; repaint(); });
        } else {
            pop = 0f;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Paper background
        g2.setColor(Theme.CARD);
        g2.fillRoundRect(0, 0, w, h, 0, 0);
        g2.setColor(new Color(200, 190, 180, 60));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 0, 0);

        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        g2.setColor(Theme.TEXT);
        g2.drawString("Min-Heap Defaulters", 12, 18);

        int top = 30;
        int pageH = 32;
        int show = Math.min(5, items.size());
        
        // Draw stacked pages (lowest attendance rises to top)
        for (int i = show - 1; i >= 0; i--) {
            Item it = items.get(i);
            int y = top + i * pageH;
            float a = (i == 0) ? (1f - pop) : 1f;
            
            // Stacked page effect (offset for depth)
            int offset = (show - 1 - i) * 2;
            int pageY = y + offset;
            
            // Page (paper color)
            g2.setColor(new Color(255, 253, 248, (int) (240 * a)));
            g2.fillRoundRect(8 + offset, pageY - 14, w - 16 - offset * 2, pageH, 2, 2);
            
            // Page border (ink)
            g2.setColor(new Color(100, 90, 80, (int) (120 * a)));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(8 + offset, pageY - 14, w - 16 - offset * 2, pageH, 2, 2);
            
            // Red flag on top element
            if (i == 0) {
                // Red bookmark slides in
                int bookmarkX = w - 24;
                int bookmarkY = pageY - 14;
                g2.setColor(new Color(Theme.DANGER.getRed(), Theme.DANGER.getGreen(), Theme.DANGER.getBlue(), (int) (200 * a)));
                g2.fillRect(bookmarkX, bookmarkY, 8, pageH);
                
                // Margin note "Below Threshold"
                g2.setColor(new Color(Theme.DANGER.getRed(), Theme.DANGER.getGreen(), Theme.DANGER.getBlue(), (int) (180 * a)));
                g2.setFont(getFont().deriveFont(Font.PLAIN, 9f));
                g2.drawString("Below", bookmarkX - 38, pageY + 2);
                g2.drawString("Threshold", bookmarkX - 50, pageY + 12);
            }

            // Page content (handwritten style)
            g2.setColor(new Color(Theme.TEXT.getRed(), Theme.TEXT.getGreen(), Theme.TEXT.getBlue(), (int) (220 * a)));
            g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
            String label = "#" + it.roll + "  " + (it.name == null ? "" : it.name);
            if (label.length() > 24) label = label.substring(0, 24) + "…";
            g2.drawString(label, 16 + offset, pageY + 2);
            
            // Attendance percentage (red ink for low)
            g2.setColor(new Color(Theme.DANGER.getRed(), Theme.DANGER.getGreen(), Theme.DANGER.getBlue(), (int) (200 * a)));
            g2.setFont(getFont().deriveFont(Font.BOLD, 10f));
            String pct = it.percent + "%";
            int pctW = g2.getFontMetrics().stringWidth(pct);
            g2.drawString(pct, w - 40 - offset - pctW, pageY + 2);
        }

        g2.dispose();
    }
}
