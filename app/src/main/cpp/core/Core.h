#ifndef BIMALAUNCH_CORE_H
#define BIMALAUNCH_CORE_H

#include <memory>
#include <string>

#include "core/Dispatcher.h"
#include "core/License.h"
#include "core/Registry.h"
#include "core/WebsiteConfig.h"

namespace bimalaunch {

class Core {
public:
    static Core& instance();

    std::string initialize(const std::string& configJson);
    std::string manifest() const;
    std::string dispatch(const std::string& action, const std::string& payload);
    void shutdown();

private:
    Core() = default;

    bool initialized_ = false;
    WebsiteConfig website_;
    Registry registry_;
    std::unique_ptr<LicenseProvider> license_;
    std::unique_ptr<Dispatcher> dispatcher_;
};

}

#endif
