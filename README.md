# SmartCampus DSA Project

A Data Structures & Algorithms course project built using **Java (Frontend GUI)** and **C++ (Backend DSA processing)**.  
The system runs as a unified application using **JNI (Java Native Interface)** to bridge Java ↔ C++ directly.

---

## 👥 Group Members

| Name | Role / Contributions |
|------|--------------------|
| **Fatima Akbar** | Lead developer: Oversees module integration, implements most Level-1 and Level-2 DSAs, sets up JNI bridges, designs Java GUI screens |
| **Shahmir Khan** | Support developer: Implements secondary DSA operations, assists in GUI development, handles testing and debugging for each module |

---

## 🔥 System Modules

| Module | Level-1 DSA | Level-2 DSA | Functionality | Primary Contributor |
|-------|-------------|-------------|--------------|------------------|
| Campus Navigator | Linked List / Hash Map | Graph + BFS / Dijkstra | Finds shortest path inside campus | Fatima Akbar |
| Student Information System | Searching + Sorting | AVL Tree | Insert, search, and update student records | Fatima Akbar |
| Attendance Management | Array / Queue | Min-Heap / Priority Queue | Marks attendance & alerts shortages | Shahmir Khan (assisted by Fatima) |

**Notes:**  
- Each module contains a Java GUI screen for input and display.  
- Each module implements required DSA logic in C++.  
- Integration between Java ↔ C++ is handled using JNI.  

---

## 🧠 Technology Stack

| Component | Used For |
|-----------|-----------|
| **Java Swing (NetBeans Project)** | GUI/User interaction |
| **C++ Backend** | All core DSA implementations |
| **JNI Integration** | Direct Java → C++ function calls |
| **GitHub** | Version tracking & collaboration |

---

## 📁 Project Structure

SmartCampus-DSA-Project/
│── README.md
│
├── SCNS-Java/ # NetBeans Java GUI Project
│ └── src/
│ ├── MainMenu.java
│ ├── NativeBridge.java <- JNI native method declarations
│ ├── NavigatorUI.java
│ ├── StudentInfoUI.java
│ └── AttendanceUI.java
│
└── Cpp-Native/ # C++ backend & JNI implementation
├── native_impl.cpp <- Implements JNI functions
├── graph.cpp <- Dijkstra/BFS for Campus Navigator
├── avl_tree.cpp <- Student records management
├── heap_attendance.cpp <- Attendance module
└── build.bat/.sh <- Build shared library

arduino
Copy code

---

## 🔗 Java ↔ C++ Integration (JNI)

Native Java methods:

```java
public native String getShortestPath(String src, String dest);
public native String addStudent(String name, int roll);
public native String checkAttendance(int roll);
Integration Flow:

Java GUI collects input from user.

Input is sent via JNI to corresponding C++ function.

C++ backend processes data using DSAs and returns results.

Java GUI displays output and provides feedback/errors.

🛠 How to Run
Clone Repository:

bash
Copy code
git clone https://github.com/<your-org>/DSA_Project_GroupX_FatimaAkbar_ShahmirKhan.git
cd SmartCampus-DSA-Project
Build C++ JNI Library:

Run build.bat (Windows) or build.sh (Linux/macOS) to compile the shared library.

Run Java GUI:

bash
Copy code
cd SCNS-Java/src
javac *.java
java MainMenu
Ensure the compiled C++ shared library is in the correct path for JNI to load.

📊 GitHub Collaboration
Both students contributed actively.

Branches were used for feature development and old JNI versions.

Fatima Akbar led core development and module integration; Shahmir Khan implemented secondary DSAs, assisted in GUI, and tested modules.

Commit history clearly reflects contributions for Level-1 and Level-2 DSAs per student.

Note for Instructor:

Old non-working JNI version is available in branch old-jni.

Current working version is in main branch.

