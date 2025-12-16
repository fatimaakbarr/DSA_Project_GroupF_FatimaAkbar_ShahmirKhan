#pragma once

#include "avl_tree.h"
#include "graph.h"
#include "heap_attendance.h"

// Single unified backend state shared across modules.
struct BackendState {
  CampusGraph graph;
  AvlStudentDB students;
  AttendanceManager attendance;

  void seedDemoData() {
    graph.seedDefault();

    students.upsert(StudentRecord{101, "Ayesha", "BSCS", 3});
    students.upsert(StudentRecord{102, "Hassan", "BBA", 2});
    students.upsert(StudentRecord{103, "Zara", "BSSE", 4});
    students.upsert(StudentRecord{104, "Ali", "BSAI", 1});

    attendance.seedDefault();
  }
};

inline BackendState& backend() {
  static BackendState s;
  return s;
}
