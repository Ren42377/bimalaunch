#ifndef BIMALAUNCH_DISPATCHER_H
#define BIMALAUNCH_DISPATCHER_H

#include <string>

#include "core/License.h"
#include "core/Registry.h"
#include "core/WebsiteConfig.h"

namespace bimalaunch {

class Dispatcher {
public:
    Dispatcher(Registry& registry, LicenseProvider& license, const WebsiteConfig& website);

    std::string dispatch(const std::string& action, const std::string& payload);

private:
    Registry& registry_;
    LicenseProvider& license_;
    const WebsiteConfig& website_;

    std::string handleVerifyLicense(const std::string& payload);
    std::string handleLicenseInfo();
    std::string handleOpenWebView();
    std::string handleComingSoon(const std::string& action);
    std::string handleOpenSettings();
};

}

#endif
