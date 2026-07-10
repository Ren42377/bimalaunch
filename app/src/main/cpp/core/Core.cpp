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
    dispatcher_ = std::make_unique<Dispatcher>(registry_, *license_, website_);
    initialized_ = true;

    json status;
    status["ok"] = true;
    status["initialized"] = true;
    status["licenseMode"] = license_->mode();
    status["websiteConfigured"] = website_.valid;
    return status.dump();
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
