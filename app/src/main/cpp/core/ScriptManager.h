#ifndef BIMALAUNCH_SCRIPT_MANAGER_H
#define BIMALAUNCH_SCRIPT_MANAGER_H

#include <string>
#include <vector>

#include "core/WebsiteConfig.h"

namespace bimalaunch {

class ScriptManager {
public:
    void configure(const WebsiteConfig& website);

    std::string scriptById(const std::string& id) const;
    std::string scrapeScript() const;
    std::string autoLoginScript(const std::string& username, const std::string& password) const;
    std::string scriptsJson() const;

private:
    WebsiteConfig website_;

    std::string selectorOr(const std::string& key, const std::string& fallback) const;
    static std::string jsEscape(const std::string& value);
};

}

#endif
