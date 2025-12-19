public class MainMenu {
    public static void main(String[] args) {
        // In CI/headless environments we can still validate JNI wiring.
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            NativeBridge nb = new NativeBridge();
            System.out.println(nb.testConnection());
            System.out.println("Divergence: " + nb.navDivergenceReport());
            System.out.println("BFS Gate->Library: " + nb.navShortestPath("Gate", "Library", "BFS"));
            System.out.println("Dij Gate->Library: " + nb.navShortestPath("Gate", "Library", "Dijkstra"));
            System.out.println(nb.navShortestPath("Gate", "Hostel", "Dijkstra"));
            System.out.println(nb.sisListStudents());
            System.out.println(nb.attGetDefaulters(75));
            return;
        }

        javax.swing.SwingUtilities.invokeLater(() -> {
            NativeBridge nb = new NativeBridge();
            SmartCampusFrame frame = new SmartCampusFrame(nb);
            frame.setVisible(true);
        });
    }
}
