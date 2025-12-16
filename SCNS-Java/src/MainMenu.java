public class MainMenu {
    public static void main(String[] args) {
        // In CI/headless environments we can still validate JNI wiring.
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            NativeBridge nb = new NativeBridge();
            nb.seedDemoData();
            System.out.println(nb.testConnection());
            System.out.println(nb.navShortestPath("Gate", "Hostel", "Dijkstra"));
            System.out.println(nb.sisListStudents());
            System.out.println(nb.attGetDefaulters(75));
            return;
        }

        javax.swing.SwingUtilities.invokeLater(() -> {
            NativeBridge nb = new NativeBridge();
            nb.seedDemoData();
            SmartCampusFrame frame = new SmartCampusFrame(nb);
            frame.setVisible(true);
        });
    }
}
