#include "core/WebsiteConfig.h"

#include <nlohmann/json.hpp>

namespace bimalaunch {

using json = nlohmann::json;

WebsiteConfig WebsiteConfigParser::parse(const std::string& text) {
    WebsiteConfig config;
    json root = json::parse(text, nullptr, false);
    if (root.is_discarded() || !root.is_object()) {
        return config;
    }

    config.id = root.value("id", root.value("website", std::string()));
    config.loginUrl = root.value("loginUrl", std::string());
    config.userAgent = root.value("userAgent", std::string());

    if (root.contains("selectors") && root["selectors"].is_object()) {
        for (auto it = root["selectors"].begin(); it != root["selectors"].end(); ++it) {
            if (it.value().is_string()) {
                config.selectors[it.key()] = it.value().get<std::string>();
            }
        }
    }

    if (root.contains("scripts") && root["scripts"].is_array()) {
        for (const auto& entry : root["scripts"]) {
            if (entry.is_string()) {
                config.scripts.push_back(entry.get<std::string>());
            }
        }
    }

    config.valid = !config.id.empty() || !config.loginUrl.empty();
    return config;
}

}
