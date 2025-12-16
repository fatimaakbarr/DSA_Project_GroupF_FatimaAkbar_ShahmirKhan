#pragma once

#include <string>
#include <vector>

struct StudentRecord {
  int roll = 0;
  std::string name;
  std::string program;
  int year = 1;
};

class AvlStudentDB {
 public:
  AvlStudentDB() = default;
  AvlStudentDB(const AvlStudentDB&) = delete;
  AvlStudentDB& operator=(const AvlStudentDB&) = delete;
  ~AvlStudentDB();

  bool upsert(const StudentRecord& r); // insert or update
  bool remove(int roll);
  bool find(int roll, StudentRecord& out) const;
  std::vector<StudentRecord> inorder() const;

  // For visualization: returns nodes in level order as tuples [roll, leftRollOr0, rightRollOr0]
  std::vector<std::vector<int>> snapshotEdges() const;

 private:
  struct Node {
    StudentRecord rec;
    Node* left = nullptr;
    Node* right = nullptr;
    int h = 1;
    explicit Node(const StudentRecord& r) : rec(r) {}
  };

  Node* root_ = nullptr;

  static int height(Node* n);
  static int bf(Node* n);
  static void update(Node* n);

  static Node* rotateRight(Node* y);
  static Node* rotateLeft(Node* x);
  static Node* balance(Node* n);

  static Node* insertOrUpdate(Node* n, const StudentRecord& r, bool& insertedNew);
  static Node* erase(Node* n, int roll, bool& removed);
  static Node* minNode(Node* n);

  static void destroy(Node* n);
  static void inorderCollect(Node* n, std::vector<StudentRecord>& out);

  static void snapshot(Node* n, std::vector<std::vector<int>>& out);
};
