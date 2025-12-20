import java.awt.Color;
import java.awt.Font;

import javax.swing.UIManager;

public final class Theme {
    private Theme() {}

    // Parchment/Paper colors
    public static final Color BG_0 = new Color(250, 248, 240);  // Parchment white
    public static final Color BG_1 = new Color(245, 242, 235);  // Slightly aged paper
    public static final Color CARD = new Color(255, 253, 248);  // Clean paper
    public static final Color CARD_2 = new Color(248, 246, 240); // Folder paper
    public static final Color GLASS = new Color(240, 238, 230, 200); // Paper overlay
    public static final Color BORDER = new Color(60, 50, 40, 80); // Ink border
    // Ink colors
    public static final Color TEXT = new Color(20, 15, 10);  // Black ink
    public static final Color MUTED = new Color(100, 90, 80); // Pencil gray
    // Stamp colors
    public static final Color ACCENT = new Color(180, 40, 30);  // Faded red stamp
    public static final Color ACCENT_2 = new Color(150, 30, 25); // Darker red stamp
    public static final Color DANGER = new Color(200, 50, 40);   // Red stamp (archived)
    public static final Color OK = new Color(40, 120, 60);        // Green stamp (approved)

    public static void apply() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        Font base = new Font("SansSerif", Font.PLAIN, 13);
        UIManager.put("Label.font", base);
        UIManager.put("Button.font", base);
        UIManager.put("TextField.font", base);
        UIManager.put("ComboBox.font", base);
        UIManager.put("Table.font", base);
        UIManager.put("TableHeader.font", base.deriveFont(Font.BOLD));

        UIManager.put("TextField.background", CARD_2);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextField.inactiveForeground", MUTED);

        UIManager.put("ComboBox.background", CARD_2);
        UIManager.put("ComboBox.foreground", TEXT);

        UIManager.put("Panel.background", BG_0);
        UIManager.put("Label.foreground", TEXT);

        UIManager.put("Table.background", CARD);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.gridColor", new Color(255, 255, 255, 18));
        UIManager.put("Table.selectionBackground", new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 80));
        UIManager.put("TableHeader.background", CARD_2);
        UIManager.put("TableHeader.foreground", TEXT);
    }
}
