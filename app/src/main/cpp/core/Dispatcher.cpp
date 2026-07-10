#include "core/Dispatcher.h"

#include <nlohmann/json.hpp>

namespace bimalaunch {

using json = nlohmann::json;

Dispatcher::Dispatcher(Registry& registry, LicenseProvider& license, const WebsiteConfig& website)
    : registry_(registry), license_(license), website_(website) {}

std::string Dispatcher::dispatch(const std::string& action, const std::string& payload) {
    if (action == "verify_license") {
        return handleVerifyLicense(payload);
    }
    if (action == "license_info") {
        return handleLicenseInfo();
    }
    if (action == "open_webview") {
        return handleOpenWebView();
    }
    if (action == "open_settings") {
        return handleOpenSettings();
    }
    if (action == "open_clawd" || action == "open_ai" || action == "open_floating") {
        return handleComingSoon(action);
    }

    json result;
    result["ok"] = false;
    result["effect"] = "toast";
    result["message"] = "Unknown action: " + action;
    return result.dump();
}

std::string Dispatcher::handleVerifyLicense(const std::string& payload) {
    json body = json::parse(payload, nullptr, false);
    std::string key;
    if (!body.is_discarded() && body.is_object()) {
        key = body.value("key", std::string());
    }

    LicenseVerdict verdict = license_.verify(key);
    json result;
    result["ok"] = verdict.valid;
    if (verdict.valid) {
        result["effect"] = "navigate";
        result["target"] = "main";
        result["message"] = "Lisensi valid";
    } else {
        result["effect"] = "error";
        result["errorType"] = verdict.errorType;
        result["message"] = "Lisensi tidak valid";
    }
    return result.dump();
}

std::string Dispatcher::handleLicenseInfo() {
    json result;
    result["ok"] = true;
    result["effect"] = "dialog";
    result["title"] = "Lisensi";
    result["message"] = std::string("Mode lisensi: ") + license_.mode() +
        ". Gunakan kode 'developer' untuk aktivasi pengembang.";
    return result.dump();
}

std::string Dispatcher::handleOpenWebView() {
    json result;
    if (!website_.valid) {
        result["ok"] = false;
        result["effect"] = "dialog";
        result["title"] = "Belum dikonfigurasi";
        result["message"] = "Konfigurasi website belum tersedia.";
        return result.dump();
    }

    json selectors = json::object();
    for (const auto& entry : website_.selectors) {
        selectors[entry.first] = entry.second;
    }
    json scripts = json::array();
    for (const auto& script : website_.scripts) {
        scripts.push_back(script);
    }

    json config;
    config["id"] = website_.id;
    config["loginUrl"] = website_.loginUrl;
    config["userAgent"] = website_.userAgent;
    config["selectors"] = selectors;
    config["scripts"] = scripts;

    result["ok"] = true;
    result["effect"] = "navigate";
    result["target"] = "webview";
    result["data"] = config;
    return result.dump();
}

std::string Dispatcher::handleOpenSettings() {
    json result;
    result["ok"] = true;
    result["effect"] = "navigate";
    result["target"] = "settings";
    return result.dump();
}

std::string Dispatcher::handleComingSoon(const std::string& action) {
    json result;
    result["ok"] = true;
    result["effect"] = "dialog";
    result["title"] = "Segera hadir";
    result["message"] = "Fitur ini sedang dimigrasikan ke Native Core (" + action + ").";
    return result.dump();
}

}
