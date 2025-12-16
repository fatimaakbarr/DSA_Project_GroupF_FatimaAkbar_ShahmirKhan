#pragma once

#include <string>
#include <vector>

#include "dsa_level1.h"
#include "dsa_min_heap.h"

struct PathResult {
  std::vector<std::string> path;
  int distance = -1;
  std::string algorithm;
  std::vector<std::string> visitedOrder; // for BFS visualization
};

class CampusGraph {
 public:
  CampusGraph();

  void seedDefault();
  std::vector<std::string> locations() const;

  bool addLocation(const std::string& name);
  bool addEdge(const std::string& a, const std::string& b, int w);

  PathResult bfsShortestPath(const std::string& src, const std::string& dst);
  PathResult dijkstraShortestPath(const std::string& src, const std::string& dst);

 private:
  struct Edge { int to; int w; };

  // Level-1: HashMap for name->index, LinkedList adjacency lists
  dsa::HashMap<int> indexOf_;
  std::vector<std::string> nameOf_;
  std::vector<dsa::LinkedList<Edge>> adj_;

  bool resolve(const std::string& name, int& idx) const;
};
