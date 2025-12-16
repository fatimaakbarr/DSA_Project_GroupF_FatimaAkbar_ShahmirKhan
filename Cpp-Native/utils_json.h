#pragma once

#include <string>
#include <vector>

namespace jsonutil {

std::string escapeJsonString(const std::string& s);
std::string quote(const std::string& s);

std::string obj(const std::vector<std::pair<std::string, std::string>>& kv);
std::string arr(const std::vector<std::string>& items);

} // namespace jsonutil
