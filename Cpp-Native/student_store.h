#pragma once

#include <fstream>
#include <string>
#include <vector>

#include "avl_tree.h"
#include "dsa_min_heap.h"

// Practical persistence layer:
// - CSV file is the permanent store
// - AVL tree provides O(log n) search/insert/delete by roll
//
// CSV format:
// roll,name,program,semester,present,total
// (no commas inside fields)

struct StoreResult {
  bool ok = false;
  std::string message;
};

class StudentStore {
 public:
  explicit StudentStore(const std::string& csvPath);

  StoreResult load();
  StoreResult switchToFile(const std::string& csvPath); // replace dataset + persistence path
  StoreResult addStudent(const StudentRecord& r);       // insert only, no overwrite
  StoreResult deleteStudent(int roll);                 // remove + safe rewrite
  StoreResult getStudent(int roll, StudentRecord& out) const;
  StoreResult getStudentTrace(int roll, StudentRecord& out, std::vector<int>& visited) const;

  StoreResult newDayForAll();                          // total++ for all
  StoreResult markPresent(int roll);                   // present++ for roll

  std::vector<StudentRecord> listByRoll() const;       // AVL inorder
  std::vector<StudentRecord> listByName() const;       // merge sort on name

  std::vector<StudentRecord> defaultersBelow(int minPercent) const; // min-heap

  int count() const { return db_.size(); }

  // Export current state to another CSV path.
  StoreResult exportTo(const std::string& outCsvPath) const;

 private:
  std::string path_;
  AvlStudentDB db_;

  static int percent(int present, int total);
  static bool parseLine(const std::string& line, StudentRecord& out);
  static std::string toLine(const StudentRecord& r);

  StoreResult ensureFileExists();
  StoreResult rewriteAll(const std::vector<StudentRecord>& all) const;

  static void mergeSortByName(std::vector<StudentRecord>& a);
};
