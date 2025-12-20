#include "avl_tree.h"

// Only <string>, <vector>, <iostream> are allowed by course rules.

static inline int imax(int a, int b) { return a > b ? a : b; }

AvlStudentDB::~AvlStudentDB() {
  clear();
}

void AvlStudentDB::clear() {
  destroy(root_);
  root_ = nullptr;
  size_ = 0;
}

int AvlStudentDB::height(Node* n) { return n ? n->h : 0; }

int AvlStudentDB::bf(Node* n) { return n ? height(n->left) - height(n->right) : 0; }

void AvlStudentDB::update(Node* n) {
  if (!n) return;
  n->h = 1 + imax(height(n->left), height(n->right));
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

AvlStudentDB::Node* AvlStudentDB::insertOnly(Node* n, const StudentRecord& r, bool& insertedNew) {
  if (!n) {
    insertedNew = true;
    return new Node(r);
  }
  if (r.roll < n->rec.roll) n->left = insertOnly(n->left, r, insertedNew);
  else if (r.roll > n->rec.roll) n->right = insertOnly(n->right, r, insertedNew);
  else insertedNew = false; // prevent overwrite
  return balance(n);
}

AvlStudentDB::Node* AvlStudentDB::updateOnly(Node* n, const StudentRecord& r, bool& updated) {
  if (!n) return nullptr;
  if (r.roll < n->rec.roll) n->left = updateOnly(n->left, r, updated);
  else if (r.roll > n->rec.roll) n->right = updateOnly(n->right, r, updated);
  else {
    n->rec = r;
    updated = true;
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

bool AvlStudentDB::remove(int roll) {
  bool removed = false;
  root_ = erase(root_, roll, removed);
  if (removed) size_--;
  return removed;
}

bool AvlStudentDB::insert(const StudentRecord& r) {
  bool insertedNew = false;
  root_ = insertOnly(root_, r, insertedNew);
  if (insertedNew) size_++;
  return insertedNew;
}

bool AvlStudentDB::update(const StudentRecord& r) {
  bool updated = false;
  root_ = updateOnly(root_, r, updated);
  return updated;
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

bool AvlStudentDB::findTrace(int roll, StudentRecord& out, std::vector<int>& visited) const {
  visited.clear();
  Node* cur = root_;
  while (cur) {
    visited.push_back(cur->rec.roll);
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
