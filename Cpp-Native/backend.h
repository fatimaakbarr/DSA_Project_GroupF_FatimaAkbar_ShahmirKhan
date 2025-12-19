#pragma once

#include <string>

#include "graph.h"
#include "student_store.h"

// Backend object (no global variables): one instance per Java NativeBridge.
class Backend {
 public:
  Backend(const std::string& studentCsvPath) : students(studentCsvPath) {}

  CampusGraph nav;
  StudentStore students;
};
