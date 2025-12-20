#include "utils_json.h"

namespace jsonutil {

std::string escapeJsonString(const std::string& s) {
  std::string out;
  out.reserve(s.size() + 8);
  const char* hex = "0123456789abcdef";

  for (size_t i = 0; i < s.size(); i++) {
    char c = s[i];
    switch (c) {
      case '"': out += "\\\""; break;
      case '\\': out += "\\\\"; break;
      case '\b': out += "\\b"; break;
      case '\f': out += "\\f"; break;
      case '\n': out += "\\n"; break;
      case '\r': out += "\\r"; break;
      case '\t': out += "\\t"; break;
      default:
        if ((unsigned char)c < 0x20) {
          out += "\\u00";
          out.push_back(hex[((unsigned char)c >> 4) & 0xF]);
          out.push_back(hex[(unsigned char)c & 0xF]);
        } else {
          out.push_back(c);
        }
    }
  }
  return out;
}

std::string quote(const std::string& s) { return std::string("\"") + escapeJsonString(s) + "\""; }

std::string obj(const std::vector<Kv>& kv) {
  std::string out;
  out.reserve(32 + kv.size() * 16);
  out.push_back('{');
  for (size_t i = 0; i < kv.size(); i++) {
    if (i) out.push_back(',');
    out += quote(kv[i].k);
    out.push_back(':');
    out += kv[i].v;
  }
  out.push_back('}');
  return out;
}

std::string arr(const std::vector<std::string>& items) {
  std::string out;
  out.reserve(16 + items.size() * 8);
  out.push_back('[');
  for (size_t i = 0; i < items.size(); i++) {
    if (i) out.push_back(',');
    out += items[i];
  }
  out.push_back(']');
  return out;
}

} // namespace jsonutil
