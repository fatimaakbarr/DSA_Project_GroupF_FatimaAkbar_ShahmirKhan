#pragma once

#include <string>
#include <vector>

struct StudentRecord {
  int roll = 0;
  std::string name;
  std::string program;
  int semester = 1;
  int present = 0;
  int total = 0;
};

class AvlStudentDB {
 public:
  AvlStudentDB() = default;
  AvlStudentDB(const AvlStudentDB&) = delete;
  AvlStudentDB& operator=(const AvlStudentDB&) = delete;
  ~AvlStudentDB();

  // Practical DB semantics:
  // - insert: fails if roll already exists (prevents overwrite)
  // - update: fails if roll does not exist
  bool insert(const StudentRecord& r);
  bool update(const StudentRecord& r);
  bool remove(int roll);
  bool find(int roll, StudentRecord& out) const;
  // Like find(), but also returns the visited node keys (for UI search animation).
  bool findTrace(int roll, StudentRecord& out, std::vector<int>& visited) const;
  std::vector<StudentRecord> inorder() const;

  int size() const { return size_; }
  void clear();

 private:
  struct Node {
    StudentRecord rec;
    Node* left = nullptr;
    Node* right = nullptr;
    int h = 1;
    explicit Node(const StudentRecord& r) : rec(r) {}
  };

  Node* root_ = nullptr;
  int size_ = 0;

  static int height(Node* n);
  static int bf(Node* n);
  static void update(Node* n);

  static Node* rotateRight(Node* y);
  static Node* rotateLeft(Node* x);
  static Node* balance(Node* n);

  static Node* insertOnly(Node* n, const StudentRecord& r, bool& insertedNew);
  static Node* updateOnly(Node* n, const StudentRecord& r, bool& updated);
  static Node* erase(Node* n, int roll, bool& removed);
  static Node* minNode(Node* n);

  static void destroy(Node* n);
  static void inorderCollect(Node* n, std::vector<StudentRecord>& out);
};
