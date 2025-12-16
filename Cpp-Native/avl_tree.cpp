#include "avl_tree.h"

#include <algorithm>

AvlStudentDB::~AvlStudentDB() {
  destroy(root_);
  root_ = nullptr;
}

int AvlStudentDB::height(Node* n) { return n ? n->h : 0; }

int AvlStudentDB::bf(Node* n) { return n ? height(n->left) - height(n->right) : 0; }

void AvlStudentDB::update(Node* n) {
  if (!n) return;
  n->h = 1 + std::max(height(n->left), height(n->right));
}

AvlStudentDB::Node* AvlStudentDB::rotateRight(Node* y) {
  Node* x = y->left;
  Node* t2 = x->right;
  x->right = y;
  y->left = t2;
  update(y);
  update(x);
  return x;
}

AvlStudentDB::Node* AvlStudentDB::rotateLeft(Node* x) {
  Node* y = x->right;
  Node* t2 = y->left;
  y->left = x;
  x->right = t2;
  update(x);
  update(y);
  return y;
}

AvlStudentDB::Node* AvlStudentDB::balance(Node* n) {
  update(n);
  int b = bf(n);
  if (b > 1) {
    if (bf(n->left) < 0) n->left = rotateLeft(n->left);
    return rotateRight(n);
  }
  if (b < -1) {
    if (bf(n->right) > 0) n->right = rotateRight(n->right);
    return rotateLeft(n);
  }
  return n;
}

AvlStudentDB::Node* AvlStudentDB::insertOrUpdate(Node* n, const StudentRecord& r, bool& insertedNew) {
  if (!n) {
    insertedNew = true;
    return new Node(r);
  }
  if (r.roll < n->rec.roll) n->left = insertOrUpdate(n->left, r, insertedNew);
  else if (r.roll > n->rec.roll) n->right = insertOrUpdate(n->right, r, insertedNew);
  else {
    // update
    n->rec = r;
    insertedNew = false;
    return n;
  }
  return balance(n);
}

AvlStudentDB::Node* AvlStudentDB::minNode(Node* n) {
  Node* cur = n;
  while (cur && cur->left) cur = cur->left;
  return cur;
}

AvlStudentDB::Node* AvlStudentDB::erase(Node* n, int roll, bool& removed) {
  if (!n) return nullptr;
  if (roll < n->rec.roll) n->left = erase(n->left, roll, removed);
  else if (roll > n->rec.roll) n->right = erase(n->right, roll, removed);
  else {
    removed = true;
    if (!n->left || !n->right) {
      Node* child = n->left ? n->left : n->right;
      delete n;
      return child;
    }
    Node* succ = minNode(n->right);
    n->rec = succ->rec;
    n->right = erase(n->right, succ->rec.roll, removed);
  }
  return balance(n);
}

void AvlStudentDB::destroy(Node* n) {
  if (!n) return;
  destroy(n->left);
  destroy(n->right);
  delete n;
}

void AvlStudentDB::inorderCollect(Node* n, std::vector<StudentRecord>& out) {
  if (!n) return;
  inorderCollect(n->left, out);
  out.push_back(n->rec);
  inorderCollect(n->right, out);
}

bool AvlStudentDB::upsert(const StudentRecord& r) {
  bool insertedNew = false;
  root_ = insertOrUpdate(root_, r, insertedNew);
  return insertedNew;
}

bool AvlStudentDB::remove(int roll) {
  bool removed = false;
  root_ = erase(root_, roll, removed);
  return removed;
}

bool AvlStudentDB::find(int roll, StudentRecord& out) const {
  Node* cur = root_;
  while (cur) {
    if (roll < cur->rec.roll) cur = cur->left;
    else if (roll > cur->rec.roll) cur = cur->right;
    else {
      out = cur->rec;
      return true;
    }
  }
  return false;
}

std::vector<StudentRecord> AvlStudentDB::inorder() const {
  std::vector<StudentRecord> out;
  inorderCollect(root_, out);
  return out;
}

void AvlStudentDB::snapshot(Node* n, std::vector<std::vector<int>>& out) {
  if (!n) return;
  int left = n->left ? n->left->rec.roll : 0;
  int right = n->right ? n->right->rec.roll : 0;
  out.push_back({n->rec.roll, left, right});
  snapshot(n->left, out);
  snapshot(n->right, out);
}

std::vector<std::vector<int>> AvlStudentDB::snapshotEdges() const {
  std::vector<std::vector<int>> out;
  snapshot(root_, out);
  return out;
}
