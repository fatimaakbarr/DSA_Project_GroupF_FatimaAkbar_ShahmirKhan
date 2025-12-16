#pragma once

#include <string>
#include <vector>

#include "dsa_level1.h"
#include "dsa_min_heap.h"

struct AttendanceSummary {
  int roll = 0;
  std::string name;
  int present = 0;
  int total = 0;
  int percent = 0;
};

class AttendanceManager {
 public:
  AttendanceManager();

  void seedDefault();

  bool registerStudent(int roll, const std::string& name);
  bool markPresent(int roll);
  bool incrementTotalForAll();

  bool getSummary(int roll, AttendanceSummary& out) const;
  std::vector<AttendanceSummary> defaultersBelow(int minPercent) const;

 private:
  struct Entry {
    int roll = 0;
    std::string name;
    int present = 0;
    int total = 0;
  };

  // Level-1: HashMap (roll->index via string key), Queue for batch operations
  dsa::HashMap<int> idxOf_;
  std::vector<Entry> entries_;
  mutable dsa::Queue<int> markQueue_;

  static int calcPercent(int present, int total);
};
