#include "graph.h"

#include <algorithm>
#include <climits>

CampusGraph::CampusGraph() {
  seedDefault();
}

bool CampusGraph::resolve(const std::string& name, int& idx) const {
  return indexOf_.get(name, idx);
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

  addEdge("Gate", "Admin", 3);
  addEdge("Gate", "Library", 5);
  addEdge("Admin", "Block-A", 4);
  addEdge("Admin", "Block-B", 6);
  addEdge("Library", "Cafeteria", 2);
  addEdge("Cafeteria", "Ground", 2);
  addEdge("Block-A", "Lab", 3);
  addEdge("Block-B", "Lab", 2);
  addEdge("Ground", "Hostel", 4);
  addEdge("Lab", "Hostel", 5);
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
  std::vector<int> dist(n, INT_MAX);
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
  std::reverse(path.begin(), path.end());

  res.path = std::move(path);
  res.distance = dist[t];
  return res;
}

PathResult CampusGraph::dijkstraShortestPath(const std::string& src, const std::string& dst) {
  PathResult res;
  res.algorithm = "Dijkstra";

  int s, t;
  if (!resolve(src, s) || !resolve(dst, t)) return res;

  int n = static_cast<int>(nameOf_.size());
  std::vector<int> prev(n, -1);
  std::vector<int> dist(n, INT_MAX);

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
      if (dist[u] != INT_MAX && dist[u] + e.w < dist[e.to]) {
        dist[e.to] = dist[u] + e.w;
        prev[e.to] = u;
        pq.push(NodeDist{dist[e.to], e.to});
      }
    }
  }

  if (dist[t] == INT_MAX) return res;

  std::vector<std::string> path;
  for (int cur = t; cur != -1; cur = prev[cur]) path.push_back(nameOf_[cur]);
  std::reverse(path.begin(), path.end());

  res.path = std::move(path);
  res.distance = dist[t];
  return res;
}
