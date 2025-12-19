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
  if (fromIdx >= (int)adj_.size() || toIdx >= (int)adj_.size()) return -1;
  for (auto it = adj_[fromIdx].begin(); it != adj_[fromIdx].end(); ++it) {
    const Edge& e = *it;
    if (e.to == toIdx) return e.w;
  }
  return -1;
}

bool CampusGraph::addLocation(const std::string& name) {
  int existing;
  if (indexOf_.get(name, existing)) return false;
  int idx = static_cast<int>(nameOf_.size());
  nameOf_.push_back(name);
  adj_.emplace_back();
  indexOf_.put(name, idx);
  return true;
}

bool CampusGraph::addEdge(const std::string& a, const std::string& b, int w) {
  if (w <= 0) return false;
  int ia, ib;
  if (!resolve(a, ia) || !resolve(b, ib)) return false;
  adj_[ia].pushBack(Edge{ib, w});
  adj_[ib].pushBack(Edge{ia, w});
  return true;
}

void CampusGraph::seedDefault() {
  nameOf_.clear();
  adj_.clear();
  indexOf_ = dsa::HashMap<int>();

  // Default campus map (can be extended from GUI later)
  const char* nodes[] = {
      "Gate", "Admin", "Library", "Cafeteria", "Block-A", "Block-B", "Lab", "Ground", "Hostel"};
  for (auto n : nodes) addLocation(n);

  // Weights are \"walking time\" units (lower is better).
  // Designed so BFS (fewest hops) and Dijkstra (lowest total weight) can differ.
  //
  // Example from Gate -> Library:
  // - BFS: Gate -> Library (1 hop, but heavy weight)
  // - Dijkstra: Gate -> Ground -> Cafeteria -> Library (3 hops, but lower total)
  addEdge("Gate", "Library", 15);      // direct but \"crowded\" path
  addEdge("Gate", "Admin", 3);
  addEdge("Gate", "Ground", 2);

  addEdge("Ground", "Cafeteria", 2);
  addEdge("Cafeteria", "Library", 2);

  addEdge("Admin", "Block-A", 4);
  addEdge("Admin", "Block-B", 6);
  addEdge("Block-A", "Lab", 3);
  addEdge("Block-B", "Lab", 2);
  addEdge("Lab", "Hostel", 5);
  addEdge("Ground", "Hostel", 4);
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
    for (auto it = adj_[u].begin(); it != adj_[u].end(); ++it) {
      const Edge& e = *it;
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

    for (auto it = adj_[u].begin(); it != adj_[u].end(); ++it) {
      const Edge& e = *it;
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
