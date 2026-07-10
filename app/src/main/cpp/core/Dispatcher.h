#ifndef BIMALAUNCH_DISPATCHER_H
#define BIMALAUNCH_DISPATCHER_H

#include <string>

#include "core/License.h"
#include "core/Registry.h"
#include "core/RuntimeState.h"
#include "core/ScriptManager.h"
#include "core/WebsiteConfig.h"

namespace bimalaunch {

class Dispatcher {
public:
    Dispatcher(Registry& registry,
               LicenseProvider& license,
               ScriptManager& scripts,
               RuntimeState& state,
               const WebsiteConfig& website);

    std::string dispatch(const std::string& action, const std::string& payload);

private:
    Registry& registry_;
    LicenseProvider& license_;
    ScriptManager& scripts_;
    RuntimeState& state_;
    const WebsiteConfig& website_;

    std::string handleVerifyLicense(const std::string& payload);
    std::string handleLicenseInfo();
    std::string handleWebViewConfig();
    std::string handleGetScript(const std::string& payload);
    std::string handleAutoLoginScript(const std::string& payload);
    std::string handleAntiCheatConfig();
    std::string handleEvaluateDevice(const std::string& payload);
    std::string handleGetSettingsSchema();
    std::string handleSetSetting(const std::string& payload);
    std::string handleGetSetting(const std::string& payload);
    std::string handleFloatingMenu();
    std::string handleClawdConfig();
    std::string handleNavigate(const std::string& target);
    std::string handleComingSoon(const std::string& action);
};

}

#endif
