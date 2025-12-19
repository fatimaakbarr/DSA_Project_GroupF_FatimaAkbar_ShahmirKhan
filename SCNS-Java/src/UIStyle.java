import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public final class UIStyle {
    private UIStyle() {}

    public static void comboBox(JComboBox<String> cb) {
        // Custom glass UI (including the popup) – especially important on Windows.
        try {
            cb.setUI(new GlassComboBoxUI());
        } catch (Throwable ignored) {
        }

        cb.setForeground(Theme.TEXT);
        cb.setBackground(Theme.CARD_2);
        cb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        cb.setMaximumRowCount(10);
        cb.setOpaque(false);

        cb.setRenderer(new BasicComboBoxRenderer() {
            @Override
            @SuppressWarnings("rawtypes")
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JComponent c = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                c.setOpaque(true);
                c.setFont(c.getFont().deriveFont(Font.BOLD, 13f));
                c.setForeground(isSelected ? Theme.TEXT : Theme.TEXT);
                c.setBackground(isSelected ? new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 120) : Theme.CARD);
                c.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                return c;
            }
        });

        // Make the closed state readable too
        cb.setRenderer(new ListCellRenderer<String>() {
            private final BasicComboBoxRenderer base = new BasicComboBoxRenderer();

            @Override
            public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
                JComponent c = (JComponent) base.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                c.setOpaque(true);
                c.setForeground(Theme.TEXT);
                c.setBackground(Theme.CARD_2);
                c.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                if (c instanceof javax.swing.JLabel) {
                    ((javax.swing.JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });

        // Help some Windows LAFs avoid white popup backgrounds
        try {
            UIManager.put("ComboBox.selectionBackground", new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 140));
            UIManager.put("ComboBox.selectionForeground", Theme.TEXT);
        } catch (Throwable ignored) {
        }
    }

    public static void table(JTable t) {
        t.setForeground(Theme.TEXT);
        t.setBackground(Theme.CARD);
        t.setGridColor(new Color(255, 255, 255, 18));
        t.setRowHeight(26);
        t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 90));
        t.setSelectionForeground(Theme.TEXT);
        t.setFillsViewportHeight(true);

        if (t.getTableHeader() != null) {
            t.getTableHeader().setOpaque(true);
            t.getTableHeader().setBackground(Theme.CARD_2);
            t.getTableHeader().setForeground(Theme.TEXT);
            t.getTableHeader().setReorderingAllowed(false);
            t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        }

        // Force consistent dark rendering (fixes random white row / header artifacts on Windows LAF)
        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setOpaque(true);
                c.setForeground(Theme.TEXT);

                Color zebra = (row % 2 == 0) ? Theme.CARD : new Color(Theme.CARD.getRed() + 2, Theme.CARD.getGreen() + 2, Theme.CARD.getBlue() + 6);
                if (isSelected) {
                    c.setBackground(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 110));
                } else {
                    c.setBackground(zebra);
                }
                c.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        t.setDefaultRenderer(Object.class, cell);
        t.setDefaultRenderer(Number.class, cell);

        if (t.getTableHeader() != null) {
            DefaultTableCellRenderer hdr = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    c.setOpaque(true);
                    c.setForeground(Theme.TEXT);
                    c.setBackground(Theme.CARD_2);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                    c.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                    return c;
                }
            };
            t.getTableHeader().setDefaultRenderer(hdr);
        }
    }

    public static void scrollPane(JScrollPane sp) {
        if (sp == null) return;
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        try {
            sp.getVerticalScrollBar().setUI(new DarkScrollBarUI());
            sp.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
            sp.getVerticalScrollBar().setOpaque(false);
            sp.getHorizontalScrollBar().setOpaque(false);
            sp.getVerticalScrollBar().setUnitIncrement(18);
            sp.getHorizontalScrollBar().setUnitIncrement(18);
        } catch (Throwable ignored) {
        }
    }
}
