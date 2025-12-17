#pragma once

#include <vector>

// Level-2 DSA: Min-Heap / Priority Queue (custom)

namespace dsa {

template <typename T>
inline void swapT(T& a, T& b) {
  T tmp = a;
  a = b;
  b = tmp;
}

template <typename T, typename Less>
class MinHeap {
  std::vector<T> a_;
  Less less_;

  void siftUp(size_t i) {
    while (i > 0) {
      size_t p = (i - 1) / 2;
      if (!less_(a_[i], a_[p])) break;
      swapT(a_[i], a_[p]);
      i = p;
    }
  }

  void siftDown(size_t i) {
    size_t n = a_.size();
    for (;;) {
      size_t l = i * 2 + 1;
      size_t r = i * 2 + 2;
      size_t best = i;
      if (l < n && less_(a_[l], a_[best])) best = l;
      if (r < n && less_(a_[r], a_[best])) best = r;
      if (best == i) break;
      swapT(a_[i], a_[best]);
      i = best;
    }
  }

 public:
  MinHeap() = default;

  bool empty() const { return a_.empty(); }
  size_t size() const { return a_.size(); }

  void push(const T& v) {
    a_.push_back(v);
    siftUp(a_.size() - 1);
  }

  T popMin() {
    // Precondition: not empty (callers ensure this).
    T out = a_.front();
    a_.front() = a_.back();
    a_.pop_back();
    if (!a_.empty()) siftDown(0);
    return out;
  }
};

} // namespace dsa
