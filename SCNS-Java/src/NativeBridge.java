public class NativeBridge {

    static {
    System.load("C:\\Users\\DELL\\Desktop\\SmartCampus-DSA-Project\\Cpp-Native\\campus_backend.dll");
}

    // Temporary test functions for Phase 1
    public native String testConnection();
    public native String getShortestPath(String src, String dest);
}


