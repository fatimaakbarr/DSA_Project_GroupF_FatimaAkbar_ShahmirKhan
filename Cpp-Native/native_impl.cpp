#include <jni.h>

#include <iostream>
#include <string>
#include <vector>

#include "NativeBridge.h"
#include "backend.h"
#include "utils_json.h"

extern "C" {

using jsonutil::Kv;

static jfieldID gHandleField = nullptr;

static Backend* getBackend(JNIEnv* env, jobject obj) {
  if (!obj) return nullptr;
  if (!gHandleField) {
    jclass cls = env->GetObjectClass(obj);
    gHandleField = env->GetFieldID(cls, "handle", "J");
  }
  jlong h = env->GetLongField(obj, gHandleField);
  return reinterpret_cast<Backend*>(h);
}

static void setBackend(JNIEnv* env, jobject obj, Backend* b) {
  if (!gHandleField) {
    jclass cls = env->GetObjectClass(obj);
    gHandleField = env->GetFieldID(cls, "handle", "J");
  }
  env->SetLongField(obj, gHandleField, reinterpret_cast<jlong>(b));
}

JNIEXPORT jstring JNICALL Java_NativeBridge_testConnection(JNIEnv* env, jobject) {
  return env->NewStringUTF("JNI Connected Successfully!");
}

JNIEXPORT jboolean JNICALL Java_NativeBridge_init(JNIEnv* env, jobject obj, jstring csvPath) {
  const char* p = env->GetStringUTFChars(csvPath, nullptr);
  std::string path = p ? std::string(p) : std::string("data/students.csv");
  env->ReleaseStringUTFChars(csvPath, p);

  Backend* cur = getBackend(env, obj);
  if (cur) {
    delete cur;
    setBackend(env, obj, nullptr);
  }

  Backend* b = new Backend(path);
  b->nav.seedDefault();
  StoreResult lr = b->students.load();
  (void)lr;

  // ensure some initial data if file had none
  if (b->students.count() == 0) {
    StudentRecord s1; s1.roll = 101; s1.name = "Ayesha"; s1.program = "BSCS"; s1.semester = 3; s1.present = 5; s1.total = 10;
    StudentRecord s2; s2.roll = 102; s2.name = "Hassan"; s2.program = "BBA";  s2.semester = 2; s2.present = 7; s2.total = 10;
    StudentRecord s3; s3.roll = 103; s3.name = "Zara";   s3.program = "BSSE"; s3.semester = 4; s3.present = 3; s3.total = 10;
    StudentRecord s4; s4.roll = 104; s4.name = "Ali";    s4.program = "BSAI"; s4.semester = 1; s4.present = 6; s4.total = 10;
    b->students.addStudent(s1);
    b->students.addStudent(s2);
    b->students.addStudent(s3);
    b->students.addStudent(s4);
  }

  setBackend(env, obj, b);
  return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_NativeBridge_close(JNIEnv* env, jobject obj) {
  Backend* b = getBackend(env, obj);
  if (b) {
    delete b;
    setBackend(env, obj, nullptr);
  }
}

JNIEXPORT jstring JNICALL Java_NativeBridge_sisImportCsv(JNIEnv* env, jobject obj, jstring csvPath) {
  Backend* bkend = getBackend(env, obj);
  if (!bkend) return env->NewStringUTF("{\"ok\":false,\"message\":\"Backend not initialized.\"}");
  const char* p = env->GetStringUTFChars(csvPath, nullptr);
  std::string path = p ? std::string(p) : std::string();
  env->ReleaseStringUTFChars(csvPath, p);

  StoreResult sr = bkend->students.switchToFile(path);
  std::vector<Kv> kv;
  kv.push_back(Kv{"ok", sr.ok ? "true" : "false"});
  kv.push_back(Kv{"message", jsonutil::quote(sr.message)});
  return env->NewStringUTF(jsonutil::obj(kv).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_sisExportCsv(JNIEnv* env, jobject obj, jstring csvPath) {
  Backend* bkend = getBackend(env, obj);
  if (!bkend) return env->NewStringUTF("{\"ok\":false,\"message\":\"Backend not initialized.\"}");
  const char* p = env->GetStringUTFChars(csvPath, nullptr);
  std::string path = p ? std::string(p) : std::string();
  env->ReleaseStringUTFChars(csvPath, p);

  StoreResult sr = bkend->students.exportTo(path);
  std::vector<Kv> kv;
  kv.push_back(Kv{"ok", sr.ok ? "true" : "false"});
  kv.push_back(Kv{"message", jsonutil::quote(sr.message)});
  return env->NewStringUTF(jsonutil::obj(kv).c_str());
}

// Backwards compatibility with earlier scaffold
JNIEXPORT jstring JNICALL Java_NativeBridge_getShortestPath(JNIEnv* env, jobject, jstring src, jstring dest) {
  const char* a = env->GetStringUTFChars(src, nullptr);
  const char* b = env->GetStringUTFChars(dest, nullptr);

  CampusGraph g;
  PathResult pr = g.dijkstraShortestPath(a ? a : "", b ? b : "");

  env->ReleaseStringUTFChars(src, a);
  env->ReleaseStringUTFChars(dest, b);

  using namespace jsonutil;
  std::vector<std::string> pathQuoted;
  for (const auto& s : pr.path) pathQuoted.push_back(quote(s));
  std::vector<std::string> visQuoted;
  for (const auto& s : pr.visitedOrder) visQuoted.push_back(quote(s));

  std::vector<Kv> kv;
  kv.push_back(Kv{"ok", pr.distance >= 0 ? "true" : "false"});
  kv.push_back(Kv{"algorithm", quote(pr.algorithm)});
  kv.push_back(Kv{"distance", std::to_string(pr.distance)});
  kv.push_back(Kv{"hops", std::to_string(pr.hops)});
  kv.push_back(Kv{"cost", std::to_string(pr.cost)});
  kv.push_back(Kv{"path", arr(pathQuoted)});
  kv.push_back(Kv{"visited", arr(visQuoted)});
  std::string out = obj(kv);
  return env->NewStringUTF(out.c_str());
}

JNIEXPORT jobjectArray JNICALL Java_NativeBridge_navLocations(JNIEnv* env, jobject obj) {
  Backend* b = getBackend(env, obj);
  CampusGraph* g = b ? &b->nav : nullptr;
  CampusGraph local;
  std::vector<std::string> locs = (g ? g->locations() : local.locations());
  jclass stringClass = env->FindClass("java/lang/String");
  jobjectArray arr = env->NewObjectArray((jsize)locs.size(), stringClass, env->NewStringUTF(""));
  for (jsize i = 0; i < (jsize)locs.size(); i++) {
    env->SetObjectArrayElement(arr, i, env->NewStringUTF(locs[(size_t)i].c_str()));
  }
  return arr;
}

JNIEXPORT jstring JNICALL Java_NativeBridge_navShortestPath(JNIEnv* env, jobject obj, jstring src, jstring dest, jstring algorithm) {
  Backend* bkend = getBackend(env, obj);
  CampusGraph* g = bkend ? &bkend->nav : nullptr;
  CampusGraph local;

  const char* a = env->GetStringUTFChars(src, nullptr);
  const char* b = env->GetStringUTFChars(dest, nullptr);
  const char* alg = env->GetStringUTFChars(algorithm, nullptr);

  std::string algs = alg ? std::string(alg) : std::string();
  PathResult pr = (algs == "BFS") ? (g ? g->bfsShortestPath(a ? a : "", b ? b : "") : local.bfsShortestPath(a ? a : "", b ? b : ""))
                                 : (g ? g->dijkstraShortestPath(a ? a : "", b ? b : "") : local.dijkstraShortestPath(a ? a : "", b ? b : ""));

  env->ReleaseStringUTFChars(src, a);
  env->ReleaseStringUTFChars(dest, b);
  env->ReleaseStringUTFChars(algorithm, alg);

  if (pr.distance < 0 || pr.path.empty()) {
    std::vector<Kv> kv;
    kv.push_back(Kv{"ok", "false"});
    kv.push_back(Kv{"error", jsonutil::quote("No route found (check locations).")});
    kv.push_back(Kv{"algorithm", jsonutil::quote(pr.algorithm)});
    std::string out = jsonutil::obj(kv);
    return env->NewStringUTF(out.c_str());
  }

  std::vector<std::string> pathQuoted;
  for (const auto& s : pr.path) pathQuoted.push_back(jsonutil::quote(s));
  std::vector<std::string> visQuoted;
  for (const auto& s : pr.visitedOrder) visQuoted.push_back(jsonutil::quote(s));

  // Edge weights along the returned path (for UI animation timing + explanation).
  std::vector<std::string> edgeWeights;
  if (!pr.path.empty()) {
    for (int i = 0; i + 1 < (int)pr.path.size(); i++) {
      int ai, bi;
      if (!(g ? g->resolve(pr.path[(size_t)i], ai) : local.resolve(pr.path[(size_t)i], ai))) { edgeWeights.push_back("0"); continue; }
      if (!(g ? g->resolve(pr.path[(size_t)i + 1], bi) : local.resolve(pr.path[(size_t)i + 1], bi))) { edgeWeights.push_back("0"); continue; }
      int w = (g ? g->edgeWeight(ai, bi) : local.edgeWeight(ai, bi));
      edgeWeights.push_back(std::to_string(w < 0 ? 0 : w));
    }
  }

  std::vector<Kv> kv;
  kv.push_back(Kv{"ok", "true"});
  kv.push_back(Kv{"algorithm", jsonutil::quote(pr.algorithm)});
  kv.push_back(Kv{"distance", std::to_string(pr.distance)});
  kv.push_back(Kv{"hops", std::to_string(pr.hops)});
  kv.push_back(Kv{"cost", std::to_string(pr.cost)});
  kv.push_back(Kv{"path", jsonutil::arr(pathQuoted)});
  kv.push_back(Kv{"visited", jsonutil::arr(visQuoted)});
  kv.push_back(Kv{"edgeWeights", jsonutil::arr(edgeWeights)});
  std::string out = jsonutil::obj(kv);
  return env->NewStringUTF(out.c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_navDivergenceReport(JNIEnv* env, jobject obj) {
  Backend* bkend = getBackend(env, obj);
  CampusGraph* g = bkend ? &bkend->nav : nullptr;
  CampusGraph local;
  CampusGraph* gg = g ? g : &local;

  int totalPairs = 0, divergedPairs = 0, percent = 0;
  gg->divergenceStats(totalPairs, divergedPairs, percent);

  std::vector<Kv> kv;
  kv.push_back(Kv{"totalPairs", std::to_string(totalPairs)});
  kv.push_back(Kv{"divergedPairs", std::to_string(divergedPairs)});
  kv.push_back(Kv{"percent", std::to_string(percent)});
  return env->NewStringUTF(jsonutil::obj(kv).c_str());
}

// Insert-only (prevents overwrite) per requirements.
JNIEXPORT jstring JNICALL Java_NativeBridge_sisUpsertStudent(JNIEnv* env, jobject obj, jint roll, jstring name, jstring program, jint year) {
  Backend* bkend = getBackend(env, obj);
  if (!bkend) {
    std::vector<Kv> kv;
    kv.push_back(Kv{"ok", "false"});
    kv.push_back(Kv{"message", jsonutil::quote("Backend not initialized. Restart app.")});
    return env->NewStringUTF(jsonutil::obj(kv).c_str());
  }

  const char* n = env->GetStringUTFChars(name, nullptr);
  const char* p = env->GetStringUTFChars(program, nullptr);

  StudentRecord r;
  r.roll = (int)roll;
  r.name = n ? std::string(n) : std::string();
  r.program = p ? std::string(p) : std::string();
  r.semester = (int)year;
  r.present = 0;
  r.total = 0;

  env->ReleaseStringUTFChars(name, n);
  env->ReleaseStringUTFChars(program, p);

  StoreResult sr = bkend->students.addStudent(r);
  std::vector<Kv> kv;
  kv.push_back(Kv{"ok", sr.ok ? "true" : "false"});
  kv.push_back(Kv{"message", jsonutil::quote(sr.message)});
  return env->NewStringUTF(jsonutil::obj(kv).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_sisGetStudent(JNIEnv* env, jobject obj, jint roll) {
  Backend* bkend = getBackend(env, obj);
  if (!bkend) return env->NewStringUTF("");
  StudentRecord r;
  StoreResult sr = bkend->students.getStudent((int)roll, r);
  if (!sr.ok) return env->NewStringUTF("");

  std::vector<Kv> kv;
  kv.push_back(Kv{"roll", std::to_string(r.roll)});
  kv.push_back(Kv{"name", jsonutil::quote(r.name)});
  kv.push_back(Kv{"program", jsonutil::quote(r.program)});
  kv.push_back(Kv{"year", std::to_string(r.semester)});
  kv.push_back(Kv{"present", std::to_string(r.present)});
  kv.push_back(Kv{"total", std::to_string(r.total)});
  std::string out = jsonutil::obj(kv);
  return env->NewStringUTF(out.c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_sisGetStudentTrace(JNIEnv* env, jobject obj, jint roll) {
  Backend* bkend = getBackend(env, obj);
  if (!bkend) return env->NewStringUTF("");
  StudentRecord r;
  std::vector<int> visited;
  StoreResult sr = bkend->students.getStudentTrace((int)roll, r, visited);
  if (!sr.ok) return env->NewStringUTF("");

  std::vector<std::string> visItems;
  visItems.reserve(visited.size());
  for (size_t i = 0; i < visited.size(); i++) visItems.push_back(std::to_string(visited[i]));

  std::vector<Kv> kv;
  kv.push_back(Kv{"roll", std::to_string(r.roll)});
  kv.push_back(Kv{"name", jsonutil::quote(r.name)});
  kv.push_back(Kv{"program", jsonutil::quote(r.program)});
  kv.push_back(Kv{"year", std::to_string(r.semester)});
  kv.push_back(Kv{"present", std::to_string(r.present)});
  kv.push_back(Kv{"total", std::to_string(r.total)});
  kv.push_back(Kv{"visited", jsonutil::arr(visItems)});
  return env->NewStringUTF(jsonutil::obj(kv).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_sisDeleteStudent(JNIEnv* env, jobject obj, jint roll) {
  Backend* bkend = getBackend(env, obj);
  if (!bkend) {
    std::vector<Kv> kv;
    kv.push_back(Kv{"ok", "false"});
    kv.push_back(Kv{"message", jsonutil::quote("Backend not initialized.")});
    return env->NewStringUTF(jsonutil::obj(kv).c_str());
  }

  StoreResult sr = bkend->students.deleteStudent((int)roll);
  std::vector<Kv> kv;
  kv.push_back(Kv{"ok", sr.ok ? "true" : "false"});
  kv.push_back(Kv{"message", jsonutil::quote(sr.message)});
  return env->NewStringUTF(jsonutil::obj(kv).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_sisListStudents(JNIEnv* env, jobject obj) {
  Backend* bkend = getBackend(env, obj);
  if (!bkend) return env->NewStringUTF("[]");
  std::vector<StudentRecord> all = bkend->students.listByRoll();
  std::vector<std::string> items;
  items.reserve(all.size());
  for (const auto& r : all) {
    std::vector<Kv> kv;
    kv.push_back(Kv{"roll", std::to_string(r.roll)});
    kv.push_back(Kv{"name", jsonutil::quote(r.name)});
    kv.push_back(Kv{"program", jsonutil::quote(r.program)});
    kv.push_back(Kv{"year", std::to_string(r.semester)});
    kv.push_back(Kv{"present", std::to_string(r.present)});
    kv.push_back(Kv{"total", std::to_string(r.total)});
    items.push_back(jsonutil::obj(kv));
  }
  return env->NewStringUTF(jsonutil::arr(items).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_attNewSessionDay(JNIEnv* env, jobject obj) {
  Backend* bkend = getBackend(env, obj);
  if (!bkend) {
    std::vector<Kv> kv;
    kv.push_back(Kv{"ok", "false"});
    kv.push_back(Kv{"message", jsonutil::quote("Backend not initialized.")});
    return env->NewStringUTF(jsonutil::obj(kv).c_str());
  }

  StoreResult sr = bkend->students.newDayForAll();
  std::vector<Kv> kv;
  kv.push_back(Kv{"ok", sr.ok ? "true" : "false"});
  kv.push_back(Kv{"message", jsonutil::quote(sr.message)});
  return env->NewStringUTF(jsonutil::obj(kv).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_attMarkPresent(JNIEnv* env, jobject obj, jint roll) {
  Backend* bkend = getBackend(env, obj);
  if (!bkend) {
    std::vector<Kv> kv;
    kv.push_back(Kv{"ok", "false"});
    kv.push_back(Kv{"message", jsonutil::quote("Backend not initialized.")});
    return env->NewStringUTF(jsonutil::obj(kv).c_str());
  }

  StoreResult sr = bkend->students.markPresent((int)roll);
  std::vector<Kv> kv;
  kv.push_back(Kv{"ok", sr.ok ? "true" : "false"});
  kv.push_back(Kv{"message", jsonutil::quote(sr.message)});
  return env->NewStringUTF(jsonutil::obj(kv).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_attGetSummary(JNIEnv* env, jobject obj, jint roll) {
  Backend* bkend = getBackend(env, obj);
  if (!bkend) return env->NewStringUTF("");
  StudentRecord r;
  StoreResult sr = bkend->students.getStudent((int)roll, r);
  if (!sr.ok) return env->NewStringUTF("");
  std::vector<Kv> kv;
  kv.push_back(Kv{"roll", std::to_string(r.roll)});
  kv.push_back(Kv{"name", jsonutil::quote(r.name)});
  kv.push_back(Kv{"present", std::to_string(r.present)});
  kv.push_back(Kv{"total", std::to_string(r.total)});
  int pct = (r.total > 0) ? (r.present * 100) / r.total : 0;
  kv.push_back(Kv{"percent", std::to_string(pct)});
  return env->NewStringUTF(jsonutil::obj(kv).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_attGetDefaulters(JNIEnv* env, jobject obj, jint minPercent) {
  Backend* bkend = getBackend(env, obj);
  if (!bkend) return env->NewStringUTF("[]");
  auto list = bkend->students.defaultersBelow((int)minPercent);
  std::vector<std::string> items;
  items.reserve(list.size());
  for (const auto& s : list) {
    std::vector<Kv> kv;
    kv.push_back(Kv{"roll", std::to_string(s.roll)});
    kv.push_back(Kv{"name", jsonutil::quote(s.name)});
    kv.push_back(Kv{"present", std::to_string(s.present)});
    kv.push_back(Kv{"total", std::to_string(s.total)});
    int pct = (s.total > 0) ? (s.present * 100) / s.total : 0;
    kv.push_back(Kv{"percent", std::to_string(pct)});
    items.push_back(jsonutil::obj(kv));
  }
  return env->NewStringUTF(jsonutil::arr(items).c_str());
}

} // extern "C"
