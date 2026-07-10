#ifndef BIMALAUNCH_RUNTIME_STATE_H
#define BIMALAUNCH_RUNTIME_STATE_H

#include <map>
#include <string>

namespace bimalaunch {

class RuntimeState {
public:
    void setLicenseVerified(bool value) { licenseVerified_ = value; }
    bool licenseVerified() const { return licenseVerified_; }

    void putSetting(const std::string& key, const std::string& value) { settings_[key] = value; }
    std::string setting(const std::string& key, const std::string& fallback = std::string()) const {
        auto it = settings_.find(key);
        return it != settings_.end() ? it->second : fallback;
    }
    bool boolSetting(const std::string& key, bool fallback) const {
        auto it = settings_.find(key);
        if (it == settings_.end()) {
            return fallback;
        }
        return it->second == "true" || it->second == "1";
    }

    const std::map<std::string, std::string>& settings() const { return settings_; }

private:
    bool licenseVerified_ = false;
    std::map<std::string, std::string> settings_;
};

}

#endif
