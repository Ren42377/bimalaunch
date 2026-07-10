#include "core/Registry.h"

#include <nlohmann/json.hpp>

namespace bimalaunch {

using json = nlohmann::json;

void Registry::configure(const std::string& appName, const WebsiteConfig& website) {
    appName_ = appName.empty() ? "Bimalaunch" : appName;
    website_ = website;
    rebuildFeatures();
}

void Registry::rebuildFeatures() {
    features_.clear();

    features_.push_back(Feature{
        "webview", "Buka Ujian", "Jalankan situs ujian dari konfigurasi",
        "card", "open_webview", true});

    features_.push_back(Feature{
        "clawd", "Clawd", "Asisten AI vision untuk membaca soal",
        "card", "open_clawd", false});

    features_.push_back(Feature{
        "ai", "AI Assistant", "Tanya jawab dengan model AI",
        "card", "open_ai", false});

    features_.push_back(Feature{
        "floating", "Floating Bubble", "Menu mengambang di atas ujian",
        "card", "open_floating", false});

    features_.push_back(Feature{
        "settings", "Pengaturan", "Konfigurasi aplikasi",
        "card", "open_settings", true});

    features_.push_back(Feature{
        "license", "Lisensi", "Status aktivasi perangkat",
        "card", "license_info", true});
}

bool Registry::hasFeature(const std::string& id) const {
    for (const auto& feature : features_) {
        if (feature.id == id) {
            return true;
        }
    }
    return false;
}

std::string Registry::buildManifest() const {
    json children = json::array();
    for (const auto& feature : features_) {
        json node;
        node["id"] = feature.id;
        node["component"] = feature.component;
        node["title"] = feature.title;
        node["subtitle"] = feature.subtitle;
        node["action"] = feature.action;
        node["props"] = {{"available", feature.available}};
        children.push_back(node);
    }

    json website;
    website["id"] = website_.id;
    website["loginUrl"] = website_.loginUrl;
    website["configured"] = website_.valid;

    json root;
    root["id"] = "main";
    root["component"] = "menu";
    root["title"] = appName_;
    root["subtitle"] = website_.valid ? website_.id : std::string("Universal exam launcher");
    root["children"] = children;

    json manifest;
    manifest["version"] = 1;
    manifest["appName"] = appName_;
    manifest["website"] = website;
    manifest["root"] = root;

    return manifest.dump();
}

}
