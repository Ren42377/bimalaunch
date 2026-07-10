#ifndef BIMALAUNCH_WEBSITE_CONFIG_H
#define BIMALAUNCH_WEBSITE_CONFIG_H

#include <string>
#include <vector>
#include <map>

namespace bimalaunch {

struct WebsiteConfig {
    std::string id;
    std::string loginUrl;
    std::string userAgent;
    std::map<std::string, std::string> selectors;
    std::vector<std::string> scripts;

    bool valid = false;
};

class WebsiteConfigParser {
public:
    static WebsiteConfig parse(const std::string& json);
};

}

#endif
