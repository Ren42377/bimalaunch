#include "core/Dispatcher.h"

#include <nlohmann/json.hpp>

#include "core/AntiCheat.h"

namespace bimalaunch {

using json = nlohmann::json;

namespace {
std::string payloadString(const std::string& payload, const char* key) {
    json body = json::parse(payload, nullptr, false);
    if (!body.is_discarded() && body.is_object() && body.contains(key) && body[key].is_string()) {
        return body[key].get<std::string>();
    }
    return std::string();
}

int parseIntOr(const std::string& value, int fallback) {
    if (value.empty()) return fallback;
    int result = 0;
    for (char c : value) {
        if (c < '0' || c > '9') return fallback;
        result = result * 10 + (c - '0');
    }
    return result;
}
}

Dispatcher::Dispatcher(Registry& registry,
                       LicenseProvider& license,
                       ScriptManager& scripts,
                       RuntimeState& state,
                       const WebsiteConfig& website)
    : registry_(registry), license_(license), scripts_(scripts), state_(state), website_(website) {}

std::string Dispatcher::dispatch(const std::string& action, const std::string& payload) {
    if (action == "verify_license") return handleVerifyLicense(payload);
    if (action == "license_info") return handleLicenseInfo();
    if (action == "webview_config") return handleWebViewConfig();
    if (action == "get_script") return handleGetScript(payload);
    if (action == "auto_login_script") return handleAutoLoginScript(payload);
    if (action == "anticheat_config") return handleAntiCheatConfig();
    if (action == "anticheat_is_device") return handleEvaluateDevice(payload);
    if (action == "settings_schema") return handleGetSettingsSchema();
    if (action == "set_setting") return handleSetSetting(payload);
    if (action == "get_setting") return handleGetSetting(payload);
    if (action == "floating_menu") return handleFloatingMenu();
    if (action == "clawd_detector_config") return handleClawdConfig();
    if (action == "open_settings") return handleNavigate("settings");
    if (action == "open_webview") return handleNavigate("webview");
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
    const std::string key = payloadString(payload, "key");
    LicenseVerdict verdict = license_.verify(key);
    json result;
    result["ok"] = verdict.valid;
    if (verdict.valid) {
        state_.setLicenseVerified(true);
        result["effect"] = "license_verified";
        result["message"] = "Lisensi valid";
    } else {
        result["effect"] = "error";
        result["errorType"] = verdict.errorType;
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

std::string Dispatcher::handleWebViewConfig() {
    json selectors = json::object();
    for (const auto& entry : website_.selectors) {
        selectors[entry.first] = entry.second;
    }
    json headers = json::object();
    for (const auto& entry : website_.headers) {
        headers[entry.first] = entry.second;
    }

    bool practiceMode = state_.boolSetting("practice_mode_enabled", false);

    json config;
    config["id"] = website_.id;
    config["loginUrl"] = website_.loginUrl;
    config["userAgent"] = website_.userAgent;
    config["userAgentMarker"] = website_.userAgentMarker;
    config["selectors"] = selectors;
    config["headers"] = headers;
    config["cookiePolicy"] = website_.cookiePolicy;
    config["autoLoginEnabled"] = website_.autoLogin.enabled;
    config["practiceMode"] = practiceMode;
    config["configured"] = website_.valid;

    json result;
    result["ok"] = true;
    result["effect"] = "data";
    result["data"] = config;
    return result.dump();
}

std::string Dispatcher::handleGetScript(const std::string& payload) {
    const std::string id = payloadString(payload, "id");
    json result;
    result["ok"] = true;
    result["effect"] = "data";
    result["data"] = {{"id", id}, {"script", scripts_.scriptById(id)}};
    return result.dump();
}

std::string Dispatcher::handleAutoLoginScript(const std::string& payload) {
    const std::string username = state_.setting("auto_username", payloadString(payload, "username"));
    const std::string password = state_.setting("auto_password", payloadString(payload, "password"));
    json result;
    result["ok"] = true;
    result["effect"] = "data";
    result["data"] = {{"script", scripts_.autoLoginScript(username, password)}};
    return result.dump();
}

std::string Dispatcher::handleAntiCheatConfig() {
    json result;
    result["ok"] = true;
    result["effect"] = "data";
    result["data"] = json::parse(AntiCheat::forbiddenPackagesJson(), nullptr, false);
    return result.dump();
}

std::string Dispatcher::handleEvaluateDevice(const std::string& payload) {
    json result;
    result["ok"] = true;
    result["effect"] = "data";
    result["data"] = json::parse(AntiCheat::evaluateDevice(payload), nullptr, false);
    return result.dump();
}

std::string Dispatcher::handleGetSettingsSchema() {
    json result;
    result["ok"] = true;
    result["effect"] = "data";
    result["data"] = json::parse(registry_.buildSettingsSchema(state_), nullptr, false);
    return result.dump();
}

std::string Dispatcher::handleSetSetting(const std::string& payload) {
    json body = json::parse(payload, nullptr, false);
    json result;
    if (body.is_discarded() || !body.is_object() || !body.contains("key")) {
        result["ok"] = false;
        result["effect"] = "toast";
        result["message"] = "Invalid setting payload";
        return result.dump();
    }
    const std::string key = body.value("key", std::string());
    std::string value;
    if (body.contains("value")) {
        if (body["value"].is_string()) {
            value = body["value"].get<std::string>();
        } else if (body["value"].is_boolean()) {
            value = body["value"].get<bool>() ? "true" : "false";
        } else if (body["value"].is_number_integer()) {
            value = std::to_string(body["value"].get<long long>());
        }
    }
    state_.putSetting(key, value);
    result["ok"] = true;
    result["effect"] = "noop";
    return result.dump();
}

std::string Dispatcher::handleGetSetting(const std::string& payload) {
    const std::string key = payloadString(payload, "key");
    json result;
    result["ok"] = true;
    result["effect"] = "data";
    result["data"] = {{"key", key}, {"value", state_.setting(key)}};
    return result.dump();
}

std::string Dispatcher::handleFloatingMenu() {
    json result;
    result["ok"] = true;
    result["effect"] = "data";
    result["data"] = json::parse(registry_.buildFloatingMenu(), nullptr, false);
    return result.dump();
}

std::string Dispatcher::handleClawdConfig() {
    json labels = json::array({
        "question_area", "a", "b", "c", "d", "e",
        "button_prev", "button_next", "question_list_button",
        "question_list_popup_close", "question_number",
        "question_number_answered", "question_number_unanswered",
        "question_number_current"
    });

    json config;
    config["modelAsset"] = "clawd/clawd_v2_f16.tflite";
    config["labels"] = labels;
    config["confidenceThreshold"] = 0.25;
    config["nmsIouThreshold"] = 0.45;
    config["globalDuplicateIouThreshold"] = 0.38;
    config["runtime"] = state_.setting("clawd_runtime", "cpu");
    config["cpuThreads"] = parseIntOr(state_.setting("clawd_cpu_threads", "2"), 2);
    config["alwaysOnDefault"] = state_.boolSetting("clawd_always_on", false);
    config["drawBoxDefault"] = state_.boolSetting("clawd_draw_box", false);
    config["pollIntervalMs"] = 1500;

    json result;
    result["ok"] = true;
    result["effect"] = "data";
    result["data"] = config;
    return result.dump();
}

std::string Dispatcher::handleNavigate(const std::string& target) {
    json result;
    result["ok"] = true;
    result["effect"] = "navigate";
    result["target"] = target;
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
