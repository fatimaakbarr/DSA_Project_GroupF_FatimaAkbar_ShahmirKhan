public class MainMenu {
    public static void main(String[] args) {
        NativeBridge nb = new NativeBridge();
        System.out.println(nb.testConnection());
        System.out.println(nb.getShortestPath("A", "B"));
    }
}
