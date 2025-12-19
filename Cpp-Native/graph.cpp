#include "graph.h"

// Only <string>, <vector>, <iostream> are allowed by course rules.

CampusGraph::CampusGraph() {
  seedDefault();
}

bool CampusGraph::resolve(const std::string& name, int& idx) const {
  return indexOf_.get(name, idx);
}

int CampusGraph::edgeWeight(int fromIdx, int toIdx) const {
  if (fromIdx < 0 || toIdx < 0) return -1;
  if (fromIdx >= (int)adjW_.size() || toIdx >= (int)adjW_.size()) return -1;
  for (auto it = adjW_[fromIdx].begin(); it != adjW_[fromIdx].end(); ++it) {
    const EdgeW& e = *it;
    if (e.to == toIdx) return e.w;
  }
  return -1;
}

bool CampusGraph::addLocation(const std::string& name) {
  int existing;
  if (indexOf_.get(name, existing)) return false;
  int idx = static_cast<int>(nameOf_.size());
  nameOf_.push_back(name);
  adjBfs_.emplace_back();
  adjW_.emplace_back();
  indexOf_.put(name, idx);
  return true;
}

bool CampusGraph::addEdgeBfs(const std::string& a, const std::string& b) {
  int ia, ib;
  if (!resolve(a, ia) || !resolve(b, ib)) return false;
  adjBfs_[ia].pushBack(EdgeU{ib});
  adjBfs_[ib].pushBack(EdgeU{ia});
  return true;
}

bool CampusGraph::addEdgeDijkstra(const std::string& a, const std::string& b, int w) {
  if (w <= 0) return false;
  int ia, ib;
  if (!resolve(a, ia) || !resolve(b, ib)) return false;
  adjW_[ia].pushBack(EdgeW{ib, w});
  adjW_[ib].pushBack(EdgeW{ia, w});
  return true;
}

bool CampusGraph::addEdge(const std::string& a, const std::string& b, int w) {
  // Convenience: add to both graphs with same weight.
  bool ok1 = addEdgeBfs(a, b);
  bool ok2 = addEdgeDijkstra(a, b, w);
  return ok1 && ok2;
}

void CampusGraph::seedDefault() {
  nameOf_.clear();
  adjBfs_.clear();
  adjW_.clear();
  indexOf_ = dsa::HashMap<int>();

  // Default campus map (can be extended from GUI later)
  const char* nodes[] = {
      "Gate", "Admin", "Library", "Cafeteria", "Block-A", "Block-B", "Lab", "Ground", "Hostel"};
  for (auto n : nodes) addLocation(n);

  // ==========================================================
  // NON-NEGOTIABLE DEMO CASE (must differ):
  //
  // Locations: Gate, Admin, Library, Ground, Cafeteria
  //
  // BFS graph (unweighted edges):
  //   Gate-Admin
  //   Admin-Library
  //   Gate-Ground
  //   Ground-Cafeteria
  //   Cafeteria-Library
  //
  // Dijkstra graph (weights):
  //   Gate-Admin=12, Admin-Library=12  (total 24, fewer hops)
  //   Gate-Ground=3, Ground-Cafeteria=3, Cafeteria-Library=3 (total 9, more hops)
  //
  // Expected Gate -> Library:
  //   BFS:      Gate -> Admin -> Library
  //   Dijkstra: Gate -> Ground -> Cafeteria -> Library
  // ==========================================================
  addEdgeBfs("Gate", "Admin");
  addEdgeBfs("Admin", "Library");
  addEdgeBfs("Gate", "Ground");
  addEdgeBfs("Ground", "Cafeteria");
  addEdgeBfs("Cafeteria", "Library");

  addEdgeDijkstra("Gate", "Admin", 12);
  addEdgeDijkstra("Admin", "Library", 12);
  addEdgeDijkstra("Gate", "Ground", 3);
  addEdgeDijkstra("Ground", "Cafeteria", 3);
  addEdgeDijkstra("Cafeteria", "Library", 3);

  // Extra campus edges (kept realistic, but DO NOT create a shorter-hop alternative Gate->Library)
  addEdge("Admin", "Block-A", 6);
  addEdge("Admin", "Block-B", 8);
  addEdge("Block-A", "Lab", 5);
  addEdge("Block-B", "Lab", 4);
  addEdge("Lab", "Hostel", 9);
  addEdge("Ground", "Hostel", 7);
}

std::vector<std::string> CampusGraph::locations() const {
  return nameOf_;
}

PathResult CampusGraph::bfsShortestPath(const std::string& src, const std::string& dst) {
  PathResult res;
  res.algorithm = "BFS";

  int s, t;
  if (!resolve(src, s) || !resolve(dst, t)) return res;

  int n = static_cast<int>(nameOf_.size());
  std::vector<int> prev(n, -1);
  const int INF = 1000000000;
  std::vector<int> dist(n, INF);
  std::vector<bool> vis(n, false);

  // Level-1: Queue
  dsa::Queue<int> q;
  q.push(s);
  dist[s] = 0;
  vis[s] = true;

  while (!q.empty()) {
    int u = q.pop();
    res.visitedOrder.push_back(nameOf_[u]);
    if (u == t) break;
    for (auto it = adjBfs_[u].begin(); it != adjBfs_[u].end(); ++it) {
      const EdgeU& e = *it;
      if (!vis[e.to]) {
        vis[e.to] = true;
        prev[e.to] = u;
        dist[e.to] = dist[u] + 1; // unweighted hop count
        q.push(e.to);
      }
    }
  }

  if (!vis[t]) return res;

  std::vector<std::string> path;
  for (int cur = t; cur != -1; cur = prev[cur]) path.push_back(nameOf_[cur]);
  // manual reverse
  for (size_t i = 0, j = path.size() ? path.size() - 1 : 0; i < j; i++, j--) {
    std::string tmp = path[i];
    path[i] = path[j];
    path[j] = tmp;
  }

  res.path = std::move(path);
  res.hops = dist[t];
  res.distance = res.hops; // compatibility

  // Compute weighted cost for the found BFS path (may be larger than Dijkstra).
  int cost = 0;
  for (int i = 0; i + 1 < (int)res.path.size(); i++) {
    int aIdx, bIdx;
    if (!resolve(res.path[(size_t)i], aIdx) || !resolve(res.path[(size_t)i + 1], bIdx)) { cost = -1; break; }
    int w = edgeWeight(aIdx, bIdx);
    if (w < 0) { cost = -1; break; }
    cost += w;
  }
  res.cost = cost;
  return res;
}

PathResult CampusGraph::dijkstraShortestPath(const std::string& src, const std::string& dst) {
  PathResult res;
  res.algorithm = "Dijkstra";

  int s, t;
  if (!resolve(src, s) || !resolve(dst, t)) return res;

  int n = static_cast<int>(nameOf_.size());
  std::vector<int> prev(n, -1);
  const int INF = 1000000000;
  std::vector<int> dist(n, INF);

  struct NodeDist { int d; int v; };
  struct Less { bool operator()(const NodeDist& a, const NodeDist& b) const { return a.d < b.d; } };

  // Level-2: MinHeap priority queue
  dsa::MinHeap<NodeDist, Less> pq;
  dist[s] = 0;
  pq.push(NodeDist{0, s});

  std::vector<bool> settled(n, false);

  while (!pq.empty()) {
    NodeDist nd = pq.popMin();
    int u = nd.v;
    if (settled[u]) continue;
    settled[u] = true;
    res.visitedOrder.push_back(nameOf_[u]);
    if (u == t) break;

    for (auto it = adjW_[u].begin(); it != adjW_[u].end(); ++it) {
      const EdgeW& e = *it;
      if (dist[u] != INF && dist[u] + e.w < dist[e.to]) {
        dist[e.to] = dist[u] + e.w;
        prev[e.to] = u;
        pq.push(NodeDist{dist[e.to], e.to});
      }
    }
  }

  if (dist[t] == INF) return res;

  std::vector<std::string> path;
  for (int cur = t; cur != -1; cur = prev[cur]) path.push_back(nameOf_[cur]);
  // manual reverse
  for (size_t i = 0, j = path.size() ? path.size() - 1 : 0; i < j; i++, j--) {
    std::string tmp = path[i];
    path[i] = path[j];
    path[j] = tmp;
  }

  res.path = std::move(path);
  res.cost = dist[t];
  res.distance = res.cost; // compatibility
  res.hops = (int)res.path.size() > 0 ? (int)res.path.size() - 1 : -1;
  return res;
}
