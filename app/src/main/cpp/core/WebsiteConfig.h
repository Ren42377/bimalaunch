#ifndef BIMALAUNCH_WEBSITE_CONFIG_H
#define BIMALAUNCH_WEBSITE_CONFIG_H

#include <string>
#include <vector>
#include <map>

namespace bimalaunch {

struct AutoLoginConfig {
    bool enabled = false;
    std::string usernameSelector;
    std::string passwordSelector;
    std::string submitSelector;
};

struct WebsiteConfig {
    std::string id;
    std::string loginUrl;
    std::string userAgent;
    std::string userAgentMarker;
    std::map<std::string, std::string> selectors;
    std::map<std::string, std::string> headers;
    std::vector<std::string> scripts;
    std::string cookiePolicy;
    AutoLoginConfig autoLogin;

    bool valid = false;
};

class WebsiteConfigParser {
public:
    static WebsiteConfig parse(const std::string& json);
};

}

#endif
