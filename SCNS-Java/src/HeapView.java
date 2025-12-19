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

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRoundRect(0, 0, w, h, 18, 18);
        g2.setColor(new Color(255, 255, 255, 18));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);

        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        g2.setColor(Theme.TEXT);
        g2.drawString("Defaulters (heap)", 12, 18);

        int top = 30;
        int rowH = 28;
        int show = Math.min(5, items.size());
        for (int i = 0; i < show; i++) {
            Item it = items.get(i);
            int y = top + i * rowH;
            float a = (i == 0) ? (1f - pop) : 1f;

            // flagged top element
            Color c = (i == 0) ? Theme.DANGER : Theme.ACCENT_2;
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (120 * a)));
            g2.fillRoundRect(10, y - 12, w - 20, 24, 999, 999);

            g2.setColor(new Color(255, 255, 255, (int) (40 * a)));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(10, y - 12, w - 21, 23, 999, 999);

            // icon
            g2.setColor(new Color(255, 255, 255, (int) (200 * a)));
            g2.fill(new Ellipse2D.Double(16, y - 6, 12, 12));
            g2.setColor(new Color(0, 0, 0, (int) (180 * a)));
            g2.drawString(String.valueOf(Math.max(0, it.percent)), 34, y + 4);

            g2.setColor(new Color(255, 255, 255, (int) (220 * a)));
            String label = "#" + it.roll + "  " + (it.name == null ? "" : it.name);
            if (label.length() > 26) label = label.substring(0, 26) + "…";
            g2.drawString(label, 64, y + 4);
        }

        g2.dispose();
    }
}
