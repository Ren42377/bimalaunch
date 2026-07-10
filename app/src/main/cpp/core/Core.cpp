#include "core/Core.h"

#include <nlohmann/json.hpp>

namespace bimalaunch {

using json = nlohmann::json;

Core& Core::instance() {
    static Core core;
    return core;
}

std::string Core::initialize(const std::string& configJson) {
    json root = json::parse(configJson, nullptr, false);
    std::string appName = "Bimalaunch";
    WebsiteConfig website;

    if (!root.is_discarded() && root.is_object()) {
        appName = root.value("appName", appName);
        if (root.contains("website") && root["website"].is_object()) {
            website = WebsiteConfigParser::parse(root["website"].dump());
        }
    }

    website_ = website;
    license_ = std::make_unique<DummyLicenseProvider>();
    registry_.configure(appName, website_);
    scripts_.configure(website_);
    applyInitialSettings(configJson);
    dispatcher_ = std::make_unique<Dispatcher>(registry_, *license_, scripts_, state_, website_);
    initialized_ = true;

    json status;
    status["ok"] = true;
    status["initialized"] = true;
    status["licenseMode"] = license_->mode();
    status["websiteConfigured"] = website_.valid;
    return status.dump();
}

void Core::applyInitialSettings(const std::string& configJson) {
    json root = json::parse(configJson, nullptr, false);
    if (root.is_discarded() || !root.is_object()) {
        return;
    }
    if (root.contains("settings") && root["settings"].is_object()) {
        for (auto it = root["settings"].begin(); it != root["settings"].end(); ++it) {
            if (it.value().is_string()) {
                state_.putSetting(it.key(), it.value().get<std::string>());
            } else if (it.value().is_boolean()) {
                state_.putSetting(it.key(), it.value().get<bool>() ? "true" : "false");
            } else if (it.value().is_number_integer()) {
                state_.putSetting(it.key(), std::to_string(it.value().get<long long>()));
            }
        }
    }
}

std::string Core::manifest() const {
    if (!initialized_) {
        json error;
        error["version"] = 1;
        error["error"] = "not_initialized";
        return error.dump();
    }
    return registry_.buildManifest();
}

std::string Core::dispatch(const std::string& action, const std::string& payload) {
    if (!initialized_ || !dispatcher_) {
        json error;
        error["ok"] = false;
        error["effect"] = "toast";
        error["message"] = "Core not initialized";
        return error.dump();
    }
    return dispatcher_->dispatch(action, payload);
}

void Core::shutdown() {
    dispatcher_.reset();
    license_.reset();
    initialized_ = false;
}

}
