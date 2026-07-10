#ifndef BIMALAUNCH_REGISTRY_H
#define BIMALAUNCH_REGISTRY_H

#include <string>
#include <vector>

#include "core/WebsiteConfig.h"

namespace bimalaunch {

struct Feature {
    std::string id;
    std::string title;
    std::string subtitle;
    std::string component;
    std::string action;
    bool available = true;
};

class Registry {
public:
    void configure(const std::string& appName, const WebsiteConfig& website);
    std::string buildManifest() const;
    bool hasFeature(const std::string& id) const;

private:
    std::string appName_;
    WebsiteConfig website_;
    std::vector<Feature> features_;

    void rebuildFeatures();
};

}

#endif
