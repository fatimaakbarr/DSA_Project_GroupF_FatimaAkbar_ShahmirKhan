#pragma once

#include <string>
#include <vector>

namespace jsonutil {

struct Kv {
  std::string k;
  std::string v; // already JSON encoded value
};

std::string escapeJsonString(const std::string& s);
std::string quote(const std::string& s);

std::string obj(const std::vector<Kv>& kv);
std::string arr(const std::vector<std::string>& items);

} // namespace jsonutil
