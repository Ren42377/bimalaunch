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

std::string Registry::buildFloatingMenu() const {
    json items = json::array();
    items.push_back({{"id", "clawd"}, {"title", "Clawd"}, {"icon", "ic_clawd_icon"}, {"action", "open_clawd"}});
    items.push_back({{"id", "ai"}, {"title", "AI"}, {"icon", "ic_ri_claude_fill"}, {"action", "open_ai"}});
    items.push_back({{"id", "settings"}, {"title", "Pengaturan"}, {"icon", "ic_lsicon_settings_outline"}, {"action", "open_settings"}});

    json geometry;
    geometry["bubbleSize"] = 60;
    geometry["bubbleInset"] = 2;
    geometry["edgeMargin"] = 12;
    geometry["verticalMargin"] = 48;
    geometry["panelCornerRadius"] = 24;
    geometry["baseMenuWidth"] = 160;
    geometry["baseMenuHeight"] = 300;
    geometry["defaultBubbleYFraction"] = 0.33;
    geometry["snapDurationMillis"] = 300;
    geometry["discardStartOffset"] = 330;
    geometry["discardRange"] = 260;

    json result;
    result["title"] = "Menu";
    result["items"] = items;
    result["geometry"] = geometry;
    return result.dump();
}

std::string Registry::buildSettingsSchema(const RuntimeState& state) const {
    json sections = json::array();

    json mainRows = json::array();
    mainRows.push_back({{"type", "menu"}, {"id", "ai"}, {"title", "Menu AI"}, {"body", "Kunci API, model, dan prompt AI"}});
    mainRows.push_back({{"type", "menu"}, {"id", "clawd"}, {"title", "Clawd"}, {"body", "Auto mode, model AI, runtime, dan thread"}});
    mainRows.push_back({{"type", "menu"}, {"id", "practice"}, {"title", "Practice Mode"}, {"body", "Halaman ujian offline"}});
    sections.push_back({{"id", "main"}, {"title", "Pengaturan"}, {"rows", mainRows}});

    json aiRows = json::array();
    aiRows.push_back({{"type", "text"}, {"id", "gemini_api_key"}, {"title", "Kunci Gemini"},
        {"password", true}, {"value", state.setting("gemini_api_key")}});
    aiRows.push_back({{"type", "text"}, {"id", "ai_prompt"}, {"title", "Prompt AI"},
        {"password", false}, {"value", state.setting("ai_prompt")}});
    sections.push_back({{"id", "ai"}, {"title", "Menu AI"}, {"rows", aiRows}});

    json clawdRows = json::array();
    clawdRows.push_back({{"type", "choice"}, {"id", "clawd_auto_mode"}, {"title", "Auto Mode"},
        {"selected", state.setting("clawd_auto_mode", "balance")},
        {"options", json::array({
            json{{"id", "stable"}, {"label", "Stable"}},
            json{{"id", "balance"}, {"label", "Balance"}},
            json{{"id", "fast"}, {"label", "Fast"}}})}});
    clawdRows.push_back({{"type", "choice"}, {"id", "clawd_runtime"}, {"title", "Runtime"},
        {"selected", state.setting("clawd_runtime", "cpu")},
        {"options", json::array({
            json{{"id", "cpu"}, {"label", "CPU"}},
            json{{"id", "gpu"}, {"label", "GPU"}}})}});
    clawdRows.push_back({{"type", "switch"}, {"id", "clawd_always_on"}, {"title", "Always On"},
        {"body", "Jaga Clawd tetap aktif"}, {"value", state.boolSetting("clawd_always_on", false)}});
    clawdRows.push_back({{"type", "switch"}, {"id", "clawd_draw_box"}, {"title", "Draw Box"},
        {"body", "Tampilkan kotak deteksi"}, {"value", state.boolSetting("clawd_draw_box", false)}});
    sections.push_back({{"id", "clawd"}, {"title", "Clawd"}, {"rows", clawdRows}});

    json practiceRows = json::array();
    practiceRows.push_back({{"type", "switch"}, {"id", "practice_mode_enabled"}, {"title", "Practice Mode"},
        {"body", "Gunakan halaman ujian offline"}, {"value", state.boolSetting("practice_mode_enabled", false)}});
    sections.push_back({{"id", "practice"}, {"title", "Practice Mode"}, {"rows", practiceRows}});

    json schema;
    schema["sections"] = sections;
    return schema.dump();
}

}
