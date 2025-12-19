#include "student_store.h"

#include <iostream>

static inline std::string trim(const std::string& s) {
  size_t a = 0;
  while (a < s.size() && (s[a] == ' ' || s[a] == '\t' || s[a] == '\r' || s[a] == '\n')) a++;
  size_t b = s.size();
  while (b > a && (s[b - 1] == ' ' || s[b - 1] == '\t' || s[b - 1] == '\r' || s[b - 1] == '\n')) b--;
  return s.substr(a, b - a);
}

static inline bool split6(const std::string& line, std::string out[6]) {
  int idx = 0;
  std::string cur;
  for (size_t i = 0; i < line.size(); i++) {
    char c = line[i];
    if (c == ',') {
      if (idx >= 6) return false;
      out[idx++] = cur;
      cur.clear();
    } else {
      cur.push_back(c);
    }
  }
  if (idx != 5) return false;
  out[idx] = cur;
  return true;
}

static inline int toInt(const std::string& s, int def) {
  int sign = 1;
  size_t i = 0;
  std::string t = trim(s);
  if (t.empty()) return def;
  if (t[0] == '-') { sign = -1; i = 1; }
  long long v = 0;
  for (; i < t.size(); i++) {
    char c = t[i];
    if (c < '0' || c > '9') return def;
    v = v * 10 + (c - '0');
    if (v > 2000000000LL) break;
  }
  return (int)(sign * v);
}

StudentStore::StudentStore(const std::string& csvPath) : path_(csvPath) {}

int StudentStore::percent(int present, int total) {
  if (total <= 0) return 0;
  return (present * 100) / total;
}

StoreResult StudentStore::ensureFileExists() {
  std::ifstream in(path_.c_str());
  if (in.good()) return StoreResult{true, "OK"};
  in.close();

  std::ofstream out(path_.c_str(), std::ios::out);
  if (!out.good()) return StoreResult{false, "Failed to create data file."};
  out << "roll,name,program,semester,present,total\n";
  out.close();
  return StoreResult{true, "Created data file."};
}

bool StudentStore::parseLine(const std::string& line, StudentRecord& out) {
  if (line.empty()) return false;
  // skip header
  if (line.size() >= 4 && line.substr(0, 4) == "roll") return false;

  std::string parts[6];
  if (!split6(line, parts)) return false;

  out.roll = toInt(parts[0], -1);
  out.name = trim(parts[1]);
  out.program = trim(parts[2]);
  out.semester = toInt(parts[3], 1);
  out.present = toInt(parts[4], 0);
  out.total = toInt(parts[5], 0);

  if (out.roll <= 0) return false;
  if (out.name.empty()) return false;
  if (out.program.empty()) return false;
  if (out.semester < 1) out.semester = 1;
  if (out.present < 0) out.present = 0;
  if (out.total < 0) out.total = 0;
  if (out.present > out.total) out.present = out.total;

  return true;
}

std::string StudentStore::toLine(const StudentRecord& r) {
  std::string s;
  s.reserve(64);
  s += std::to_string(r.roll);
  s += ',';
  s += r.name;
  s += ',';
  s += r.program;
  s += ',';
  s += std::to_string(r.semester);
  s += ',';
  s += std::to_string(r.present);
  s += ',';
  s += std::to_string(r.total);
  s += '\n';
  return s;
}

StoreResult StudentStore::load() {
  StoreResult ok = ensureFileExists();
  if (!ok.ok) return ok;

  std::ifstream in(path_.c_str());
  if (!in.good()) return StoreResult{false, "Failed to open data file."};

  std::string line;
  int loaded = 0;
  while (std::getline(in, line)) {
    StudentRecord r;
    if (!parseLine(line, r)) continue;
    // insert only, duplicates in file are skipped
    if (db_.insert(r)) loaded++;
  }
  in.close();

  return StoreResult{true, "Loaded " + std::to_string(loaded) + " students."};
}

StoreResult StudentStore::switchToFile(const std::string& csvPath) {
  if (csvPath.empty()) return StoreResult{false, "Invalid path."};
  path_ = csvPath;
  db_.clear();
  return load();
}

StoreResult StudentStore::addStudent(const StudentRecord& r) {
  if (r.roll <= 0) return StoreResult{false, "Invalid roll."};
  if (r.name.empty()) return StoreResult{false, "Name required."};
  if (r.program.empty()) return StoreResult{false, "Program required."};

  StudentRecord existing;
  if (db_.find(r.roll, existing)) {
    return StoreResult{false, "Roll already exists. Use a different roll."};
  }

  if (!db_.insert(r)) return StoreResult{false, "Insert failed."};

  std::ofstream out(path_.c_str(), std::ios::app);
  if (!out.good()) return StoreResult{false, "Inserted in memory, but failed to write to file."};
  out << toLine(r);
  out.close();

  return StoreResult{true, "Student added."};
}

StoreResult StudentStore::rewriteAll(const std::vector<StudentRecord>& all) const {
  // Course header restrictions: avoid filesystem ops (rename/remove).
  // Safe-ish rewrite:
  // 1) write a .bak snapshot first (so recovery is possible)
  // 2) then overwrite the main file in one pass
  std::string bak = path_ + ".bak";
  {
    std::ofstream bout(bak.c_str(), std::ios::out | std::ios::trunc);
    if (!bout.good()) return StoreResult{false, "Failed to write backup file."};
    bout << "roll,name,program,semester,present,total\n";
    for (size_t i = 0; i < all.size(); i++) bout << toLine(all[i]);
    bout.close();
  }

  std::ofstream out(path_.c_str(), std::ios::out | std::ios::trunc);
  if (!out.good()) return StoreResult{false, "Failed to rewrite data file."};
  out << "roll,name,program,semester,present,total\n";
  for (size_t i = 0; i < all.size(); i++) out << toLine(all[i]);
  out.close();
  return StoreResult{true, "OK"};
}

StoreResult StudentStore::deleteStudent(int roll) {
  if (roll <= 0) return StoreResult{false, "Invalid roll."};
  bool removed = db_.remove(roll);
  if (!removed) return StoreResult{false, "Student not found."};

  std::vector<StudentRecord> all = db_.inorder();
  StoreResult wr = rewriteAll(all);
  if (!wr.ok) return wr;
  return StoreResult{true, "Student deleted."};
}

StoreResult StudentStore::getStudent(int roll, StudentRecord& out) const {
  if (roll <= 0) return StoreResult{false, "Invalid roll."};
  if (!db_.find(roll, out)) return StoreResult{false, "Not found."};
  return StoreResult{true, "OK"};
}

StoreResult StudentStore::getStudentTrace(int roll, StudentRecord& out, std::vector<int>& visited) const {
  if (roll <= 0) return StoreResult{false, "Invalid roll."};
  if (!db_.findTrace(roll, out, visited)) return StoreResult{false, "Not found."};
  return StoreResult{true, "OK"};
}

StoreResult StudentStore::newDayForAll() {
  std::vector<StudentRecord> all = db_.inorder();
  if (all.empty()) return StoreResult{false, "No students registered."};

  for (size_t i = 0; i < all.size(); i++) {
    all[i].total += 1;
    if (all[i].present > all[i].total) all[i].present = all[i].total;
  }

  // rebuild AVL (practical choice: keep AVL as index; rewrite with updated values)
  db_.clear();
  for (size_t i = 0; i < all.size(); i++) db_.insert(all[i]);

  StoreResult wr = rewriteAll(all);
  if (!wr.ok) return wr;
  return StoreResult{true, "New day recorded."};
}

StoreResult StudentStore::markPresent(int roll) {
  StudentRecord r;
  StoreResult g = getStudent(roll, r);
  if (!g.ok) return g;

  // Must have a day to mark
  if (r.total <= 0) r.total = 1;
  if (r.present < r.total) r.present += 1;

  if (!db_.update(r)) return StoreResult{false, "Update failed."};

  std::vector<StudentRecord> all = db_.inorder();
  StoreResult wr = rewriteAll(all);
  if (!wr.ok) return wr;

  return StoreResult{true, "Marked present."};
}

std::vector<StudentRecord> StudentStore::listByRoll() const {
  return db_.inorder();
}

void StudentStore::mergeSortByName(std::vector<StudentRecord>& a) {
  if (a.size() < 2) return;
  std::vector<StudentRecord> tmp;
  tmp.resize(a.size());

  // iterative mergesort (no extra headers)
  for (size_t width = 1; width < a.size(); width *= 2) {
    for (size_t i = 0; i < a.size(); i += 2 * width) {
      size_t l = i;
      size_t m = i + width;
      size_t r = i + 2 * width;
      if (m > a.size()) m = a.size();
      if (r > a.size()) r = a.size();

      size_t p = l, q = m, k = l;
      while (p < m && q < r) {
        if (a[p].name <= a[q].name) tmp[k++] = a[p++];
        else tmp[k++] = a[q++];
      }
      while (p < m) tmp[k++] = a[p++];
      while (q < r) tmp[k++] = a[q++];
    }
    for (size_t j = 0; j < a.size(); j++) a[j] = tmp[j];
  }
}

std::vector<StudentRecord> StudentStore::listByName() const {
  std::vector<StudentRecord> out = db_.inorder();
  mergeSortByName(out);
  return out;
}

std::vector<StudentRecord> StudentStore::defaultersBelow(int minPercent) const {
  struct Item { int percent; StudentRecord rec; };
  struct Less { bool operator()(const Item& a, const Item& b) const { return a.percent < b.percent; } };

  std::vector<StudentRecord> all = db_.inorder();
  dsa::MinHeap<Item, Less> heap;
  for (size_t i = 0; i < all.size(); i++) {
    int p = percent(all[i].present, all[i].total);
    heap.push(Item{p, all[i]});
  }

  std::vector<StudentRecord> out;
  while (!heap.empty()) {
    Item it = heap.popMin();
    if (it.percent >= minPercent) break;
    out.push_back(it.rec);
  }
  return out;
}

StoreResult StudentStore::exportTo(const std::string& outCsvPath) const {
  std::ofstream out(outCsvPath.c_str(), std::ios::out);
  if (!out.good()) return StoreResult{false, "Failed to open export path."};

  std::vector<StudentRecord> all = db_.inorder();
  out << "roll,name,program,semester,present,total\n";
  for (size_t i = 0; i < all.size(); i++) out << toLine(all[i]);
  out.close();

  return StoreResult{true, "Exported."};
}
