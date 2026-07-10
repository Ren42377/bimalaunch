#include "core/WebsiteConfig.h"

#include <nlohmann/json.hpp>

namespace bimalaunch {

using json = nlohmann::json;

namespace {
void readStringMap(const json& root, const char* key, std::map<std::string, std::string>& out) {
    if (root.contains(key) && root[key].is_object()) {
        for (auto it = root[key].begin(); it != root[key].end(); ++it) {
            if (it.value().is_string()) {
                out[it.key()] = it.value().get<std::string>();
            }
        }
    }
}
}

WebsiteConfig WebsiteConfigParser::parse(const std::string& text) {
    WebsiteConfig config;
    json root = json::parse(text, nullptr, false);
    if (root.is_discarded() || !root.is_object()) {
        return config;
    }

    config.id = root.value("id", root.value("website", std::string()));
    config.loginUrl = root.value("loginUrl", std::string());
    config.userAgent = root.value("userAgent", std::string());
    config.userAgentMarker = root.value("userAgentMarker", std::string());
    config.cookiePolicy = root.value("cookiePolicy", std::string("accept_all"));

    readStringMap(root, "selectors", config.selectors);
    readStringMap(root, "headers", config.headers);

    if (root.contains("scripts") && root["scripts"].is_array()) {
        for (const auto& entry : root["scripts"]) {
            if (entry.is_string()) {
                config.scripts.push_back(entry.get<std::string>());
            }
        }
    }

    if (root.contains("autoLogin") && root["autoLogin"].is_object()) {
        const json& al = root["autoLogin"];
        config.autoLogin.enabled = al.value("enabled", false);
        config.autoLogin.usernameSelector = al.value("usernameSelector", std::string());
        config.autoLogin.passwordSelector = al.value("passwordSelector", std::string());
        config.autoLogin.submitSelector = al.value("submitSelector", std::string());
    }

    config.valid = !config.id.empty() || !config.loginUrl.empty();
    return config;
}

}
