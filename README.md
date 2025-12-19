## SmartCampus DSA Project

A **Data Structures & Algorithms** final project built as **one unified application**:

- **Java (Swing GUI)**: interaction, validation, visualization
- **C++ (Core DSAs + Algorithms)**: all data processing, storage, and logic
- **JNI (Java Native Interface)**: direct Java ↔ C++ bridging (no separate mini-programs)

---

### Group Members (2 Students)

| Name | Role / Contributions |
|---|---|
| **Fatima Akbar** | Lead developer: module integration, core DSAs, JNI bridges, Java UI screens |
| **Shahmir Khan** | Support developer: secondary DSA operations, assists in GUI, testing & debugging |

---

### Modules (Integrated)

| Module | Level‑1 DSA(s) | Level‑2 DSA(s) | What it does |
|---|---|---|---|
| **Home** | — | — | Animated dashboard shell + module switcher |
| **Campus Navigator** | Linked List, Hash Map, Queue | Graph + BFS / Dijkstra | **Algorithm Race** + route explanation (Fewest Stops vs Shortest Distance) |
| **Student Information System** | — | AVL Tree | Practical student records (**persistent CSV**) + fast search + sorted listing |
| **Attendance Management** | — | Min‑Heap / Priority Queue | Live attendance ring + defaulters list (min‑heap) stored per student |

---

### DSA Usage (Per Module)

#### Campus Navigator (C++: `graph.cpp`, `graph.h`)
- **Level‑1: Linked List**: adjacency list storage (fast edge iteration, memory-efficient)
- **Level‑1: Hash Map**: location name → node index lookup (fast O(1) average)
- **Level‑1: Queue**: BFS traversal order (unweighted)
- **Level‑2: Graph + Algorithms**:
  - **BFS graph (unweighted)**: optimizes **minimum hops**
  - **Dijkstra graph (weighted)**: optimizes **minimum total weight** using a custom **Min‑Heap**

**Non‑negotiable test case (hardcoded + verified):**

Locations: `Gate, Admin, Library, Ground, Cafeteria`

**BFS (unweighted adjacency list):**
- Gate → Admin
- Admin → Library
- Gate → Ground
- Ground → Cafeteria
- Cafeteria → Library

**Dijkstra (weighted adjacency list):**
- Gate → Admin = 12
- Admin → Library = 12
- Gate → Ground = 3
- Ground → Cafeteria = 3
- Cafeteria → Library = 3

**Expected (Gate → Library):**
- **BFS** → `Gate → Admin → Library`
- **Dijkstra** → `Gate → Ground → Cafeteria → Library`

Operations used: insert nodes/edges, traverse, BFS, Dijkstra, path reconstruction.

#### Student Information System (C++: `student_store.cpp/.h`, `avl_tree.cpp/.h`)
- **Level‑2: AVL Tree**: practical in‑memory index (key = roll) for **O(log n)** search/insert/delete
- **Persistent storage (fstream)**: CSV file is the permanent datastore (no data loss on restart)
- **Sorting**: AVL **in‑order traversal** lists students sorted by roll
- **Duplicate prevention**: inserting a student with an existing roll is rejected (no overwrite)

CSV format:

`roll,name,program,semester,present,total`

Default dataset: `data/students.csv` (100+ realistic records).

#### Attendance Management (stored per student)
- **Stored per student record**: attendance is part of `StudentRecord` (`present/total`)
- **Level‑2: Min‑Heap**: defaulters list (pull lowest attendance quickly)

Operations used: increment totals (new day), mark present, compute percentage, heap push/pop for defaulters.

---

### Integration Layer (Java ↔ C++ via JNI)

**Flow (all modules):**

1. Java GUI validates user input.
2. Java calls a `native` method in `NativeBridge.java`.
3. C++ processes data using DSAs/algorithms.
4. C++ returns results as compact JSON strings (or `String[]` for locations).
5. Java parses results and updates UI/visualizations.

JNI entry points are implemented in **`Cpp-Native/native_impl.cpp`**.

---

### C++ Header Restrictions (Course Rule)

C++ sources in `Cpp-Native/` are written to comply with the restriction:

- Allowed: `<iostream> <fstream> <vector> <string>` + required JNI headers (`jni.h`) + project headers
- Avoids other standard headers (e.g., `<algorithm>`, `<sstream>`, etc.)

---

### Project Structure

```
/workspace
  /data
    students.csv                 (100+ realistic records; default datastore)

  /SCNS-Java/src
    MainMenu.java               (entry point)
    NativeBridge.java           (JNI native methods)
    SmartCampusFrame.java       (main animated UI shell)
    NavigatorUI.java            (module UI)
    StudentInfoUI.java          (module UI)
    AttendanceUI.java           (module UI)
    GraphView.java              (route visualization)
    ProgressRing.java           (attendance visualization)
    Theme.java, Anim.java, Toast.java, JsonMini.java, ModernButton.java

  /Cpp-Native
    build.sh                    (Linux build)
    build.bat                   (Windows build)
    native_impl.cpp             (JNI layer)
    graph.cpp / graph.h         (Navigator DSAs)
    student_store.cpp/.h        (Student persistence + attendance)
    avl_tree.cpp / avl_tree.h   (AVL index)
    dsa_level1.h                (LinkedList/HashMap/Queue)
    dsa_min_heap.h              (MinHeap)
    utils_json.cpp/.h           (JSON helpers)

  run.sh                        (build + run)
```

---

### How to Run (Linux)

1) **Build C++ JNI library**

```bash
./Cpp-Native/build.sh
```

2) **Compile Java**

```bash
javac SCNS-Java/src/*.java
```

3) **Run** (run from repo root so JNI can find `./Cpp-Native/libcampus_backend.so`)

```bash
java -cp SCNS-Java/src MainMenu
```

Or use the one-shot helper:

```bash
./run.sh
```

---

### How to Run (Windows)

#### 1) Build the C++ JNI DLL

- Install **JDK 17+** and **MinGW g++**
- Ensure `JAVA_HOME` is set, then:

```bat
cd Cpp-Native
build.bat
```

This produces `Cpp-Native\\campus_backend.dll`.

#### 2) Compile + run Java

```bat
cd SCNS-Java\src
javac *.java
java MainMenu
```

---

### Data Import/Export

In **Student Info**:

- **Import CSV**: switches the app to the chosen CSV file and rebuilds the AVL index
- **Export CSV**: writes the current dataset to a chosen CSV path

CSV format must be:

`roll,name,program,semester,present,total`

---

### Work Allocation (Per Module)

- **Campus Navigator**
  - Lead: **Fatima Akbar**
  - Assist: **Shahmir Khan**
  - DSA focus: Graph + BFS/Dijkstra, linked-list adjacency, hash map mapping, JNI methods, UI visualization

- **Student Information System**
  - Lead: **Fatima Akbar**
  - Assist: **Shahmir Khan**
  - DSA focus: AVL index + persistent CSV storage + validation + import/export

- **Attendance Management**
  - Lead: **Shahmir Khan**
  - Assist: **Fatima Akbar**
  - DSA focus: attendance operations stored per student + min-heap defaulters + UI + testing
