import java.awt.Color;
import java.awt.Font;

import javax.swing.UIManager;

public final class Theme {
    private Theme() {}

    public static final Color BG_0 = new Color(10, 12, 20);
    public static final Color BG_1 = new Color(18, 22, 36);
    public static final Color CARD = new Color(22, 28, 46);
    public static final Color CARD_2 = new Color(26, 34, 56);
    public static final Color TEXT = new Color(235, 242, 255);
    public static final Color MUTED = new Color(160, 174, 200);
    public static final Color ACCENT = new Color(120, 92, 255);
    public static final Color ACCENT_2 = new Color(0, 210, 255);
    public static final Color DANGER = new Color(255, 86, 120);
    public static final Color OK = new Color(72, 255, 190);

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
    }
}
