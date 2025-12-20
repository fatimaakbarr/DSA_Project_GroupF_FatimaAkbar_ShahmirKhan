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
  adjW_.emplace_back();
  indexOf_.put(name, idx);
  return true;
}

bool CampusGraph::addEdge(const std::string& a, const std::string& b, int w) {
  if (w <= 0) return false;
  int ia, ib;
  if (!resolve(a, ia) || !resolve(b, ib)) return false;
  adjW_[ia].pushBack(EdgeW{ib, w});
  adjW_[ib].pushBack(EdgeW{ia, w});
  return true;
}

void CampusGraph::seedDefault() {
  nameOf_.clear();
  adjW_.clear();
  indexOf_ = dsa::HashMap<int>();

  // Default campus map (can be extended from GUI later)
  const char* nodes[] = {
      "Gate", "Admin", "Library", "Ground", "Cafeteria",
      "Block-A", "Block-B", "Lab", "Gym", "Dorms", "Hostel"};
  for (auto n : nodes) addLocation(n);

  // ==========================================================
  // NON-NEGOTIABLE DEMO CASE (must differ) on the SAME graph:
  //
  // Locations: Gate, Admin, Library, Ground, Cafeteria
  //
  // Edges (weights):
  //   Gate-Admin=12, Admin-Library=12  (total 24, fewer hops)
  //   Gate-Ground=3, Ground-Cafeteria=3, Cafeteria-Library=3 (total 9, more hops)
  //
  // Expected Gate -> Library:
  //   BFS:      Gate -> Admin -> Library
  //   Dijkstra: Gate -> Ground -> Cafeteria -> Library
  // ==========================================================
  addEdge("Gate", "Admin", 12);
  addEdge("Admin", "Library", 12);
  addEdge("Gate", "Ground", 3);
  addEdge("Ground", "Cafeteria", 3);
  addEdge("Cafeteria", "Library", 3);

  // Main roads (low weight, longer hop chains)
  addEdge("Ground", "Admin", 2);
  addEdge("Admin", "Block-A", 2);
  addEdge("Admin", "Block-B", 2);
  addEdge("Block-A", "Lab", 2);
  addEdge("Block-B", "Lab", 2);
  addEdge("Lab", "Gym", 2);
  addEdge("Gym", "Dorms", 2);
  addEdge("Dorms", "Hostel", 2);
  addEdge("Ground", "Hostel", 3);

  // Global design rule:
  // - Main roads are cheap but usually require more hops.
  // - Shortcuts are expensive but reduce hops.
  // This makes BFS (min hops) and Dijkstra (min cost) differ for MOST pairs.

  // Add expensive shortcuts between most pairs (but keep Gate restricted so the required test case stays stable).
  int gateIdx = -1, adminIdx = -1, groundIdx = -1;
  (void)resolve("Gate", gateIdx);
  (void)resolve("Admin", adminIdx);
  (void)resolve("Ground", groundIdx);

  for (int i = 0; i < (int)nameOf_.size(); i++) {
    for (int j = i + 1; j < (int)nameOf_.size(); j++) {
      // Skip if an edge already exists.
      if (edgeWeight(i, j) >= 0) continue;

      // Keep Gate from having direct shortcuts (except required Gate-Admin and Gate-Ground already added).
      if (i == gateIdx || j == gateIdx) continue;

      // Add expensive shortcut.
      addEdge(nameOf_[(size_t)i], nameOf_[(size_t)j], 50);
    }
  }
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
    for (auto it = adjW_[u].begin(); it != adjW_[u].end(); ++it) {
      const EdgeW& e = *it;
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

void CampusGraph::divergenceStats(int& totalPairs, int& divergedPairs, int& percent) const {
  totalPairs = 0;
  divergedPairs = 0;
  percent = 0;
  int n = (int)nameOf_.size();
  if (n <= 1) return;

  // unordered pairs
  for (int i = 0; i < n; i++) {
    for (int j = i + 1; j < n; j++) {
      totalPairs++;
      const std::string& a = nameOf_[(size_t)i];
      const std::string& b = nameOf_[(size_t)j];

      // Use const_cast to call existing methods without changing signatures.
      CampusGraph* self = const_cast<CampusGraph*>(this);
      PathResult bfs = self->bfsShortestPath(a, b);
      PathResult dij = self->dijkstraShortestPath(a, b);
      if (bfs.path.empty() || dij.path.empty()) continue;

      bool differentPath = bfs.path != dij.path;
      bool bfsFewerHops = bfs.hops >= 0 && dij.hops >= 0 && bfs.hops < dij.hops;
      bool bfsHigherCost = bfs.cost >= 0 && dij.cost >= 0 && bfs.cost > dij.cost;
      if (differentPath && bfsFewerHops && bfsHigherCost) divergedPairs++;
    }
  }
  percent = (totalPairs > 0) ? (divergedPairs * 100) / totalPairs : 0;
}
