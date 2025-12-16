#include <jni.h>

#include <string>
#include <vector>

#include "NativeBridge.h"
#include "backend_state.h"
#include "utils_json.h"

extern "C" {

JNIEXPORT jstring JNICALL Java_NativeBridge_testConnection(JNIEnv* env, jobject) {
  return env->NewStringUTF("JNI Connected Successfully!");
}

JNIEXPORT void JNICALL Java_NativeBridge_seedDemoData(JNIEnv*, jobject) {
  backend().seedDemoData();
}

// Backwards compatibility with earlier scaffold
JNIEXPORT jstring JNICALL Java_NativeBridge_getShortestPath(JNIEnv* env, jobject, jstring src, jstring dest) {
  const char* a = env->GetStringUTFChars(src, nullptr);
  const char* b = env->GetStringUTFChars(dest, nullptr);

  PathResult pr = backend().graph.dijkstraShortestPath(a ? a : "", b ? b : "");

  env->ReleaseStringUTFChars(src, a);
  env->ReleaseStringUTFChars(dest, b);

  using namespace jsonutil;
  std::vector<std::string> pathQuoted;
  for (const auto& s : pr.path) pathQuoted.push_back(quote(s));
  std::vector<std::string> visQuoted;
  for (const auto& s : pr.visitedOrder) visQuoted.push_back(quote(s));

  std::string out = obj({
      {"ok", pr.distance >= 0 ? "true" : "false"},
      {"algorithm", quote(pr.algorithm)},
      {"distance", std::to_string(pr.distance)},
      {"path", arr(pathQuoted)},
      {"visited", arr(visQuoted)},
  });
  return env->NewStringUTF(out.c_str());
}

JNIEXPORT jobjectArray JNICALL Java_NativeBridge_navLocations(JNIEnv* env, jobject) {
  std::vector<std::string> locs = backend().graph.locations();
  jclass stringClass = env->FindClass("java/lang/String");
  jobjectArray arr = env->NewObjectArray((jsize)locs.size(), stringClass, env->NewStringUTF(""));
  for (jsize i = 0; i < (jsize)locs.size(); i++) {
    env->SetObjectArrayElement(arr, i, env->NewStringUTF(locs[(size_t)i].c_str()));
  }
  return arr;
}

JNIEXPORT jstring JNICALL Java_NativeBridge_navShortestPath(JNIEnv* env, jobject, jstring src, jstring dest, jstring algorithm) {
  const char* a = env->GetStringUTFChars(src, nullptr);
  const char* b = env->GetStringUTFChars(dest, nullptr);
  const char* alg = env->GetStringUTFChars(algorithm, nullptr);

  std::string algs = alg ? std::string(alg) : std::string();
  PathResult pr = (algs == "BFS") ? backend().graph.bfsShortestPath(a ? a : "", b ? b : "")
                                 : backend().graph.dijkstraShortestPath(a ? a : "", b ? b : "");

  env->ReleaseStringUTFChars(src, a);
  env->ReleaseStringUTFChars(dest, b);
  env->ReleaseStringUTFChars(algorithm, alg);

  using namespace jsonutil;
  if (pr.distance < 0 || pr.path.empty()) {
    std::string out = obj({
        {"ok", "false"},
        {"error", quote("No route found (check locations).")},
        {"algorithm", quote(pr.algorithm)},
    });
    return env->NewStringUTF(out.c_str());
  }

  std::vector<std::string> pathQuoted;
  for (const auto& s : pr.path) pathQuoted.push_back(quote(s));
  std::vector<std::string> visQuoted;
  for (const auto& s : pr.visitedOrder) visQuoted.push_back(quote(s));

  std::string out = obj({
      {"ok", "true"},
      {"algorithm", quote(pr.algorithm)},
      {"distance", std::to_string(pr.distance)},
      {"path", arr(pathQuoted)},
      {"visited", arr(visQuoted)},
  });
  return env->NewStringUTF(out.c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_sisUpsertStudent(JNIEnv* env, jobject, jint roll, jstring name, jstring program, jint year) {
  const char* n = env->GetStringUTFChars(name, nullptr);
  const char* p = env->GetStringUTFChars(program, nullptr);

  StudentRecord r;
  r.roll = (int)roll;
  r.name = n ? std::string(n) : std::string();
  r.program = p ? std::string(p) : std::string();
  r.year = (int)year;

  env->ReleaseStringUTFChars(name, n);
  env->ReleaseStringUTFChars(program, p);

  bool inserted = backend().students.upsert(r);
  backend().attendance.registerStudent(r.roll, r.name);

  using namespace jsonutil;
  std::string out = obj({
      {"ok", "true"},
      {"action", quote(inserted ? "inserted" : "updated")},
  });
  return env->NewStringUTF(out.c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_sisGetStudent(JNIEnv* env, jobject, jint roll) {
  StudentRecord r;
  if (!backend().students.find((int)roll, r)) return env->NewStringUTF("");

  using namespace jsonutil;
  std::string out = obj({
      {"roll", std::to_string(r.roll)},
      {"name", quote(r.name)},
      {"program", quote(r.program)},
      {"year", std::to_string(r.year)},
  });
  return env->NewStringUTF(out.c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_sisDeleteStudent(JNIEnv* env, jobject, jint roll) {
  bool removed = backend().students.remove((int)roll);
  if (removed) {
    backend().attendance.removeStudent((int)roll);
  }
  using namespace jsonutil;
  std::string out = obj({
      {"ok", removed ? "true" : "false"},
      {"message", quote(removed ? "Student removed." : "Student not found.")},
  });
  return env->NewStringUTF(out.c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_sisListStudents(JNIEnv* env, jobject) {
  using namespace jsonutil;
  std::vector<StudentRecord> all = backend().students.inorder();
  std::vector<std::string> items;
  items.reserve(all.size());
  for (const auto& r : all) {
    items.push_back(obj({
        {"roll", std::to_string(r.roll)},
        {"name", quote(r.name)},
        {"program", quote(r.program)},
        {"year", std::to_string(r.year)},
    }));
  }
  return env->NewStringUTF(arr(items).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_sisTreeSnapshot(JNIEnv* env, jobject) {
  using namespace jsonutil;
  auto edges = backend().students.snapshotEdges();
  std::vector<std::string> items;
  items.reserve(edges.size());
  for (const auto& e : edges) {
    items.push_back(arr({std::to_string(e[0]), std::to_string(e[1]), std::to_string(e[2])}));
  }
  return env->NewStringUTF(arr(items).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_attRegisterStudent(JNIEnv* env, jobject, jint roll, jstring name) {
  const char* n = env->GetStringUTFChars(name, nullptr);
  bool inserted = backend().attendance.registerStudent((int)roll, n ? std::string(n) : std::string());
  env->ReleaseStringUTFChars(name, n);

  using namespace jsonutil;
  return env->NewStringUTF(obj({
      {"ok", "true"},
      {"action", quote(inserted ? "registered" : "updated")},
  }).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_attNewSessionDay(JNIEnv* env, jobject) {
  bool ok = backend().attendance.incrementTotalForAll();
  using namespace jsonutil;
  return env->NewStringUTF(obj({
      {"ok", ok ? "true" : "false"},
      {"message", quote(ok ? "New class day recorded." : "No students registered.")},
  }).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_attMarkPresent(JNIEnv* env, jobject, jint roll) {
  bool ok = backend().attendance.markPresent((int)roll);
  using namespace jsonutil;
  return env->NewStringUTF(obj({
      {"ok", ok ? "true" : "false"},
      {"message", quote(ok ? "Marked present." : "Roll not found.")},
  }).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_attGetSummary(JNIEnv* env, jobject, jint roll) {
  AttendanceSummary s;
  if (!backend().attendance.getSummary((int)roll, s)) return env->NewStringUTF("");

  using namespace jsonutil;
  return env->NewStringUTF(obj({
      {"roll", std::to_string(s.roll)},
      {"name", quote(s.name)},
      {"present", std::to_string(s.present)},
      {"total", std::to_string(s.total)},
      {"percent", std::to_string(s.percent)},
  }).c_str());
}

JNIEXPORT jstring JNICALL Java_NativeBridge_attGetDefaulters(JNIEnv* env, jobject, jint minPercent) {
  using namespace jsonutil;
  auto list = backend().attendance.defaultersBelow((int)minPercent);
  std::vector<std::string> items;
  items.reserve(list.size());
  for (const auto& s : list) {
    items.push_back(obj({
        {"roll", std::to_string(s.roll)},
        {"name", quote(s.name)},
        {"present", std::to_string(s.present)},
        {"total", std::to_string(s.total)},
        {"percent", std::to_string(s.percent)},
    }));
  }
  return env->NewStringUTF(arr(items).c_str());
}

} // extern "C"
