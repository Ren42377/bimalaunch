#include "core/AntiCheat.h"

#include <nlohmann/json.hpp>

namespace bimalaunch {

using json = nlohmann::json;

namespace {

const std::vector<std::string> kForbiddenPackages = {
    "com.lwi.android.flapps",
    "com.lwi.android.flappsfull",
    "com.glgjing.floating.apps.assistive.touch.pro",
    "com.jsvmsoft.stickynotes",
    "com.github.ericytsang.multiwindow.app.android",
    "com.split.screen.shortcut.overview.accessibility.notification",
    "any.splitscreen",
    "comspli.exaspli.splitscspli",
    "net.geekstools.floatshort.PRO",
    "com.dvg.multivideoplayer",
    "com.fb.splitscreenlauncher",
    "com.excelliance.multiaccounts",
    "com.dualbrowser.splitscreen.multi",
    "com.ccssoftwareinc.splitscreenlauncher",
    "com.clone.android.dual.space",
    "com.pspace.vandroid"
};

bool startsWith(const std::string& value, const std::string& prefix) {
    return value.size() >= prefix.size() && value.compare(0, prefix.size(), prefix) == 0;
}

bool contains(const std::string& value, const std::string& needle) {
    return value.find(needle) != std::string::npos;
}

std::string str(const json& root, const char* key) {
    if (root.contains(key) && root[key].is_string()) {
        return root[key].get<std::string>();
    }
    return std::string();
}

}

const std::vector<std::string>& AntiCheat::forbiddenPackages() {
    return kForbiddenPackages;
}

const char* AntiCheat::requiredPackage() {
    return "com.android.vending";
}

std::string AntiCheat::forbiddenPackagesJson() {
    json list = json::array();
    for (const auto& pkg : kForbiddenPackages) {
        list.push_back(pkg);
    }
    json result;
    result["packages"] = list;
    result["required"] = requiredPackage();
    return result.dump();
}

std::string AntiCheat::evaluateDevice(const std::string& factsJson) {
    json root = json::parse(factsJson, nullptr, false);
    json result;
    result["value"] = false;
    if (root.is_discarded() || !root.is_object()) {
        return result.dump();
    }

    const std::string fingerprint = str(root, "fingerprint");
    const std::string model = str(root, "model");
    const std::string manufacturer = str(root, "manufacturer");
    const std::string host = str(root, "host");
    const std::string brand = str(root, "brand");
    const std::string device = str(root, "device");
    const std::string product = str(root, "product");
    const std::string hardware = str(root, "hardware");
    const std::string operatorName = str(root, "networkOperator");
    const bool qemuFileExists = root.value("qemuFileExists", false);
    const bool playStoreInstalled = root.value("playStoreInstalled", true);

    bool emulator = false;
    if (startsWith(fingerprint, "google/sdk_gphone_") ||
        startsWith(fingerprint, "generic") ||
        startsWith(fingerprint, "unknown")) {
        emulator = true;
    } else if (contains(model, "Emulator") || contains(model, "Android SDK built for")) {
        emulator = true;
    } else if (contains(manufacturer, "Genymotion")) {
        emulator = true;
    } else if (startsWith(host, "Build")) {
        emulator = true;
    } else if (startsWith(brand, "generic") && startsWith(device, "generic")) {
        emulator = true;
    } else if (product == "google_sdk") {
        emulator = true;
    } else if (operatorName == "Android" || operatorName == "Unknown") {
        emulator = true;
    } else if (contains(hardware, "goldfish") || contains(hardware, "ranchu") ||
        contains(product, "sdk") || contains(product, "google_sdk") ||
        contains(product, "sdk_x86") || contains(product, "sdk_gphone64")) {
        emulator = true;
    } else if (qemuFileExists) {
        emulator = true;
    } else if (!playStoreInstalled) {
        emulator = true;
    }

    result["value"] = emulator;
    return result.dump();
}

}
