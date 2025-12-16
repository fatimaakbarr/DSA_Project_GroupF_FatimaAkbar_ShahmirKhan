#pragma once

#include <cstddef>
#include <cstdint>
#include <functional>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

// Level-1 DSAs (custom): LinkedList, HashMap, Queue + helper sorting/searching.

namespace dsa {

// ---------------- LinkedList ----------------

template <typename T>
class LinkedList {
  struct Node {
    T value;
    Node* next;
    Node(const T& v, Node* n) : value(v), next(n) {}
  };

  Node* head_ = nullptr;
  Node* tail_ = nullptr;
  size_t size_ = 0;

 public:
  LinkedList() = default;
  LinkedList(const LinkedList&) = delete;
  LinkedList& operator=(const LinkedList&) = delete;
  LinkedList(LinkedList&& o) noexcept : head_(o.head_), tail_(o.tail_), size_(o.size_) {
    o.head_ = nullptr;
    o.tail_ = nullptr;
    o.size_ = 0;
  }
  LinkedList& operator=(LinkedList&& o) noexcept {
    if (this != &o) {
      clear();
      head_ = o.head_;
      tail_ = o.tail_;
      size_ = o.size_;
      o.head_ = nullptr;
      o.tail_ = nullptr;
      o.size_ = 0;
    }
    return *this;
  }

  ~LinkedList() { clear(); }

  void pushBack(const T& v) {
    Node* n = new Node(v, nullptr);
    if (!tail_) {
      head_ = tail_ = n;
    } else {
      tail_->next = n;
      tail_ = n;
    }
    size_++;
  }

  void clear() {
    Node* cur = head_;
    while (cur) {
      Node* next = cur->next;
      delete cur;
      cur = next;
    }
    head_ = tail_ = nullptr;
    size_ = 0;
  }

  size_t size() const { return size_; }

  struct Iterator {
    Node* p;
    bool operator!=(const Iterator& o) const { return p != o.p; }
    void operator++() { p = p->next; }
    T& operator*() const { return p->value; }
  };

  Iterator begin() { return Iterator{head_}; }
  Iterator end() { return Iterator{nullptr}; }

  struct ConstIterator {
    const Node* p;
    bool operator!=(const ConstIterator& o) const { return p != o.p; }
    void operator++() { p = p->next; }
    const T& operator*() const { return p->value; }
  };

  ConstIterator begin() const { return ConstIterator{head_}; }
  ConstIterator end() const { return ConstIterator{nullptr}; }
};

// ---------------- HashMap (open addressing) ----------------

inline uint64_t fnv1a64(const std::string& s) {
  uint64_t h = 1469598103934665603ULL;
  for (unsigned char c : s) {
    h ^= static_cast<uint64_t>(c);
    h *= 1099511628211ULL;
  }
  return h;
}

template <typename V>
class HashMap {
  enum class State : uint8_t { Empty = 0, Filled = 1, Deleted = 2 };

  struct Slot {
    std::string key;
    V value;
    State state = State::Empty;
  };

  std::vector<Slot> table_;
  size_t used_ = 0;     // filled slots
  size_t occupied_ = 0; // filled + deleted

  void rehash(size_t newCap) {
    std::vector<Slot> old = std::move(table_);
    table_.assign(newCap, Slot{});
    used_ = 0;
    occupied_ = 0;
    for (auto& s : old) {
      if (s.state == State::Filled) {
        put(s.key, s.value);
      }
    }
  }

  void ensureCapacity() {
    if (table_.empty()) rehash(16);
    // keep load factor under ~0.65 (occupied), rebuild on many tombstones
    if (occupied_ * 100 >= table_.size() * 65) rehash(table_.size() * 2);
    if (used_ * 100 <= table_.size() * 20 && table_.size() > 32) rehash(table_.size() / 2);
  }

 public:
  HashMap() { table_.assign(16, Slot{}); }

  size_t size() const { return used_; }

  bool contains(const std::string& key) const {
    V out;
    return get(key, out);
  }

  bool get(const std::string& key, V& out) const {
    if (table_.empty()) return false;
    uint64_t h = fnv1a64(key);
    size_t cap = table_.size();
    size_t idx = static_cast<size_t>(h % cap);
    for (size_t step = 0; step < cap; step++) {
      const Slot& s = table_[idx];
      if (s.state == State::Empty) return false;
      if (s.state == State::Filled && s.key == key) {
        out = s.value;
        return true;
      }
      idx = (idx + 1) % cap;
    }
    return false;
  }

  void put(const std::string& key, const V& value) {
    ensureCapacity();
    uint64_t h = fnv1a64(key);
    size_t cap = table_.size();
    size_t idx = static_cast<size_t>(h % cap);

    size_t firstDeleted = static_cast<size_t>(-1);

    for (size_t step = 0; step < cap; step++) {
      Slot& s = table_[idx];
      if (s.state == State::Filled) {
        if (s.key == key) {
          s.value = value;
          return;
        }
      } else if (s.state == State::Deleted) {
        if (firstDeleted == static_cast<size_t>(-1)) firstDeleted = idx;
      } else { // Empty
        if (firstDeleted != static_cast<size_t>(-1)) idx = firstDeleted;
        Slot& t = table_[idx];
        t.key = key;
        t.value = value;
        t.state = State::Filled;
        used_++;
        occupied_++;
        return;
      }
      idx = (idx + 1) % cap;
    }

    // fallback (shouldn't happen due to ensureCapacity)
    rehash(cap * 2);
    put(key, value);
  }

  bool erase(const std::string& key) {
    if (table_.empty()) return false;
    uint64_t h = fnv1a64(key);
    size_t cap = table_.size();
    size_t idx = static_cast<size_t>(h % cap);
    for (size_t step = 0; step < cap; step++) {
      Slot& s = table_[idx];
      if (s.state == State::Empty) return false;
      if (s.state == State::Filled && s.key == key) {
        s.state = State::Deleted;
        s.key.clear();
        used_--;
        return true;
      }
      idx = (idx + 1) % cap;
    }
    return false;
  }

  std::vector<std::string> keys() const {
    std::vector<std::string> out;
    out.reserve(used_);
    for (const auto& s : table_) {
      if (s.state == State::Filled) out.push_back(s.key);
    }
    return out;
  }
};

// ---------------- Queue (circular buffer) ----------------

template <typename T>
class Queue {
  std::vector<T> buf_;
  size_t head_ = 0;
  size_t tail_ = 0;
  size_t size_ = 0;

  void grow() {
    size_t newCap = buf_.empty() ? 16 : buf_.size() * 2;
    std::vector<T> nb(newCap);
    for (size_t i = 0; i < size_; i++) {
      nb[i] = buf_[(head_ + i) % buf_.size()];
    }
    buf_ = std::move(nb);
    head_ = 0;
    tail_ = size_;
  }

 public:
  size_t size() const { return size_; }
  bool empty() const { return size_ == 0; }

  void push(const T& v) {
    if (buf_.empty() || size_ == buf_.size()) grow();
    buf_[tail_] = v;
    tail_ = (tail_ + 1) % buf_.size();
    size_++;
  }

  T pop() {
    if (size_ == 0) throw std::runtime_error("Queue underflow");
    T v = buf_[head_];
    head_ = (head_ + 1) % buf_.size();
    size_--;
    return v;
  }
};

// ---------------- Merge sort (Level-1 algo) ----------------

template <typename T, typename Less>
void mergeSort(std::vector<T>& a, Less less) {
  if (a.size() < 2) return;
  std::vector<T> tmp(a.size());

  std::function<void(size_t, size_t)> sortRec = [&](size_t l, size_t r) {
    if (r - l <= 1) return;
    size_t m = (l + r) / 2;
    sortRec(l, m);
    sortRec(m, r);
    size_t i = l, j = m, k = l;
    while (i < m && j < r) {
      if (less(a[i], a[j])) tmp[k++] = a[i++];
      else tmp[k++] = a[j++];
    }
    while (i < m) tmp[k++] = a[i++];
    while (j < r) tmp[k++] = a[j++];
    for (size_t t = l; t < r; t++) a[t] = tmp[t];
  };

  sortRec(0, a.size());
}

} // namespace dsa
