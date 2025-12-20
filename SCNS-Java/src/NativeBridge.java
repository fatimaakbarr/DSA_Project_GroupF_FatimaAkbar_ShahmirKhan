public class NativeBridge {

    // Native backend handle (no global state on C++ side)
    private long handle = 0;

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
    public native boolean init(String csvPath);
    public native void close();

    // Navigator (Graph + BFS/Dijkstra)
    public native String[] navLocations();
    public native String navShortestPath(String src, String dest, String algorithm);
    public native String navDivergenceReport();

    // Student Information System (AVL + searching/sorting)
    public native String sisUpsertStudent(int roll, String name, String program, int year);
    public native String sisGetStudent(int roll);
    public native String sisGetStudentTrace(int roll);
    public native String sisDeleteStudent(int roll);
    public native String sisListStudents();
    public native String sisImportCsv(String csvPath);
    public native String sisExportCsv(String csvPath);
    // Attendance (stored in student records)
    public native String attNewSessionDay();
    public native String attMarkPresent(int roll);
    public native String attGetSummary(int roll);
    public native String attGetDefaulters(int minPercent);

    public NativeBridge() {
        // Default data file (try to locate repo-root /data/students.csv)
        String path = "data/students.csv";
        try {
            java.io.File dir = new java.io.File(System.getProperty("user.dir"));
            java.io.File found = null;
            for (int i = 0; i < 6 && dir != null; i++) {
                java.io.File candidate = new java.io.File(new java.io.File(dir, "data"), "students.csv");
                if (candidate.exists()) { found = candidate; break; }
                dir = dir.getParentFile();
            }
            if (found != null) {
                path = found.getAbsolutePath();
            } else {
                java.io.File f = new java.io.File(path);
                java.io.File parent = f.getParentFile();
                if (parent != null) parent.mkdirs();
                path = f.getAbsolutePath();
            }
        } catch (Throwable ignored) {}
        init(path);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { close(); } catch (Throwable ignored) {}
        }));
    }
}


