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
| **Campus Navigator** | Linked List, Hash Map, Queue | Graph + BFS / Dijkstra | Shortest route between campus locations + live visualization |
| **Student Information System** | Searching/Sorting (listing) | AVL Tree | Insert/search/update/delete students + AVL visualization |
| **Attendance Management** | Array / Queue, Hash Map | Min‑Heap / Priority Queue | Mark attendance, compute %, list defaulters (min‑heap priority) |

---

### DSA Usage (Per Module)

#### Campus Navigator (C++: `graph.cpp`, `graph.h`)
- **Level‑1: Linked List**: adjacency list storage (fast edge iteration, memory-efficient)
- **Level‑1: Hash Map**: location name → node index lookup (fast O(1) average)
- **Level‑1: Queue**: BFS traversal order
- **Level‑2: Graph + Algorithms**:
  - **BFS**: shortest path by *number of hops*
  - **Dijkstra** (with custom Min‑Heap priority queue): shortest weighted path

Operations used: insert nodes/edges, traverse, BFS, Dijkstra, path reconstruction.

#### Student Information System (C++: `avl_tree.cpp`, `avl_tree.h`)
- **Level‑2: AVL Tree**: self-balancing BST for fast search/insert/delete (O(log n))
- **Level‑1: Searching/Sorting**: sorted listing by inorder traversal (and prepared sorting helpers)

Operations used: insert, update (upsert), search, delete, inorder traversal, rotations.

#### Attendance Management (C++: `heap_attendance.cpp`, `heap_attendance.h`)
- **Level‑1: Array**: stores student attendance entries
- **Level‑1: Queue**: attendance mark events (supports batching/visual feedback)
- **Level‑1: Hash Map**: roll → entry index lookup
- **Level‑2: Min‑Heap**: defaulters list (pull lowest attendance quickly)

Operations used: enqueue attendance mark, increment totals, heap push/pop, filtering.

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

### Project Structure

```
/workspace
  /SCNS-Java/src
    MainMenu.java               (entry point)
    NativeBridge.java           (JNI native methods)
    SmartCampusFrame.java       (main animated UI shell)
    NavigatorUI.java            (module UI)
    StudentInfoUI.java          (module UI)
    AttendanceUI.java           (module UI)
    GraphView.java              (route visualization)
    TreeView.java               (AVL visualization)
    ProgressRing.java           (attendance visualization)
    Theme.java, Anim.java, Toast.java, JsonMini.java, ModernButton.java

  /Cpp-Native
    build.sh                    (Linux build)
    native_impl.cpp             (JNI layer)
    graph.cpp / graph.h         (Navigator DSAs)
    avl_tree.cpp / avl_tree.h   (Student DSAs)
    heap_attendance.cpp/.h      (Attendance DSAs)
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

### Work Allocation (Per Module)

- **Campus Navigator**
  - Lead: **Fatima Akbar**
  - Assist: **Shahmir Khan**
  - DSA focus: Graph + BFS/Dijkstra, linked-list adjacency, hash map mapping, JNI methods, UI visualization

- **Student Information System**
  - Lead: **Fatima Akbar**
  - Assist: **Shahmir Khan**
  - DSA focus: AVL insert/search/update/delete + visualization + JNI integration

- **Attendance Management**
  - Lead: **Shahmir Khan**
  - Assist: **Fatima Akbar**
  - DSA focus: Queue/array tracking, min-heap defaulters, JNI methods, UI + testing
