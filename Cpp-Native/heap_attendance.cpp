#include "heap_attendance.h"

AttendanceManager::AttendanceManager() {
  seedDefault();
}

int AttendanceManager::calcPercent(int present, int total) {
  if (total <= 0) return 0;
  return static_cast<int>((present * 100) / total);
}

bool AttendanceManager::registerStudent(int roll, const std::string& name) {
  if (roll <= 0) return false;
  int idx;
  if (idxOf_.get(std::to_string(roll), idx)) {
    entries_[idx].name = name;
    return false;
  }
  int newIdx = static_cast<int>(entries_.size());
  entries_.push_back(Entry{roll, name, 0, 0});
  idxOf_.put(std::to_string(roll), newIdx);
  return true;
}

bool AttendanceManager::removeStudent(int roll) {
  int idx;
  std::string key = std::to_string(roll);
  if (!idxOf_.get(key, idx)) return false;

  // swap-remove to keep vector dense
  int last = (int)entries_.size() - 1;
  if (idx != last) {
    entries_[idx] = entries_[last];
    idxOf_.put(std::to_string(entries_[idx].roll), idx);
  }
  entries_.pop_back();
  idxOf_.erase(key);
  return true;
}

bool AttendanceManager::markPresent(int roll) {
  int idx;
  if (!idxOf_.get(std::to_string(roll), idx)) return false;
  // Level-1: Queue used as an event log (for visualization / batching)
  markQueue_.push(roll);
  entries_[idx].present += 1;
  return true;
}

bool AttendanceManager::incrementTotalForAll() {
  if (entries_.empty()) return false;
  for (auto& e : entries_) e.total += 1;
  return true;
}

bool AttendanceManager::getSummary(int roll, AttendanceSummary& out) const {
  int idx;
  if (!idxOf_.get(std::to_string(roll), idx)) return false;
  const auto& e = entries_[idx];
  out.roll = e.roll;
  out.name = e.name;
  out.present = e.present;
  out.total = e.total;
  out.percent = calcPercent(e.present, e.total);
  return true;
}

std::vector<AttendanceSummary> AttendanceManager::defaultersBelow(int minPercent) const {
  struct Item { int percent; int idx; };
  struct Less { bool operator()(const Item& a, const Item& b) const { return a.percent < b.percent; } };

  // Level-2: Min-Heap to pull lowest attendance quickly
  dsa::MinHeap<Item, Less> heap;
  for (int i = 0; i < (int)entries_.size(); i++) {
    const auto& e = entries_[i];
    heap.push(Item{calcPercent(e.present, e.total), i});
  }

  std::vector<AttendanceSummary> out;
  while (!heap.empty()) {
    Item it = heap.popMin();
    if (it.percent >= minPercent) break;
    const auto& e = entries_[it.idx];
    out.push_back(AttendanceSummary{e.roll, e.name, e.present, e.total, it.percent});
  }
  return out;
}

void AttendanceManager::seedDefault() {
  idxOf_ = dsa::HashMap<int>();
  entries_.clear();

  registerStudent(101, "Ayesha");
  registerStudent(102, "Hassan");
  registerStudent(103, "Zara");
  registerStudent(104, "Ali");

  // create some starting totals/presents
  for (int day = 0; day < 10; day++) {
    incrementTotalForAll();
  }

  // give slightly different attendance
  markPresent(101); markPresent(101); markPresent(101); markPresent(101); markPresent(101);
  markPresent(102); markPresent(102); markPresent(102); markPresent(102); markPresent(102); markPresent(102); markPresent(102);
  markPresent(103); markPresent(103); markPresent(103);
  markPresent(104); markPresent(104); markPresent(104); markPresent(104); markPresent(104); markPresent(104);
}
