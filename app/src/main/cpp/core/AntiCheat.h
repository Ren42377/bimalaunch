#ifndef BIMALAUNCH_ANTI_CHEAT_H
#define BIMALAUNCH_ANTI_CHEAT_H

#include <string>
#include <vector>

namespace bimalaunch {

class AntiCheat {
public:
    static const std::vector<std::string>& forbiddenPackages();
    static const char* requiredPackage();

    static std::string forbiddenPackagesJson();

    static std::string evaluateDevice(const std::string& factsJson);
};

}

#endif
