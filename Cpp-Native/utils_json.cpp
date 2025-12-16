#include "utils_json.h"

#include <sstream>

namespace jsonutil {

std::string escapeJsonString(const std::string& s) {
  std::ostringstream oss;
  for (char c : s) {
    switch (c) {
      case '"': oss << "\\\""; break;
      case '\\': oss << "\\\\"; break;
      case '\b': oss << "\\b"; break;
      case '\f': oss << "\\f"; break;
      case '\n': oss << "\\n"; break;
      case '\r': oss << "\\r"; break;
      case '\t': oss << "\\t"; break;
      default:
        if (static_cast<unsigned char>(c) < 0x20) {
          oss << "\\u";
          const char* hex = "0123456789abcdef";
          oss << '0' << '0' << hex[(c >> 4) & 0xF] << hex[c & 0xF];
        } else {
          oss << c;
        }
    }
  }
  return oss.str();
}

std::string quote(const std::string& s) { return std::string("\"") + escapeJsonString(s) + "\""; }

std::string obj(const std::vector<std::pair<std::string, std::string>>& kv) {
  std::ostringstream oss;
  oss << '{';
  for (size_t i = 0; i < kv.size(); i++) {
    if (i) oss << ',';
    oss << quote(kv[i].first) << ':' << kv[i].second;
  }
  oss << '}';
  return oss.str();
}

std::string arr(const std::vector<std::string>& items) {
  std::ostringstream oss;
  oss << '[';
  for (size_t i = 0; i < items.size(); i++) {
    if (i) oss << ',';
    oss << items[i];
  }
  oss << ']';
  return oss.str();
}

} // namespace jsonutil
