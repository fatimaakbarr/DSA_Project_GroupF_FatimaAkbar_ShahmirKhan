public class NativeBridge {

    static {
        // Cross-platform JNI loader:
        // - Windows: Cpp-Native/campus_backend.dll
        // - Linux:   Cpp-Native/libcampus_backend.so
        // You can also set -Djava.library.path=Cpp-Native and use System.loadLibrary.
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String libFile = os.contains("win") ? "campus_backend.dll" : "libcampus_backend.so";
            // Try to locate Cpp-Native relative to current working dir (repo root or NetBeans run dir).
            java.io.File dir = new java.io.File(System.getProperty("user.dir"));
            java.io.File found = null;
            for (int i = 0; i < 6 && dir != null; i++) {
                java.io.File candidate = new java.io.File(new java.io.File(dir, "Cpp-Native"), libFile);
                if (candidate.exists()) {
                    found = candidate;
                    break;
                }
                dir = dir.getParentFile();
            }

            if (found != null) {
                System.load(found.getAbsolutePath());
            } else {
                // fallback (requires -Djava.library.path=Cpp-Native or OS-level lib path)
                System.loadLibrary("campus_backend");
            }
        } catch (Throwable t) {
            System.err.println("Failed to load JNI library: " + t);
            throw t;
        }
}

    // Core
    public native String testConnection();
    public native void seedDemoData();

    // Navigator (Graph + BFS/Dijkstra)
    public native String[] navLocations();
    public native String navShortestPath(String src, String dest, String algorithm);

    // Student Information System (AVL + searching/sorting)
    public native String sisUpsertStudent(int roll, String name, String program, int year);
    public native String sisGetStudent(int roll);
    public native String sisDeleteStudent(int roll);
    public native String sisListStudents();
    public native String sisTreeSnapshot();

    // Attendance (Queue/Array + Min-Heap)
    public native String attRegisterStudent(int roll, String name);
    public native String attNewSessionDay();
    public native String attMarkPresent(int roll);
    public native String attGetSummary(int roll);
    public native String attGetDefaulters(int minPercent);
}


