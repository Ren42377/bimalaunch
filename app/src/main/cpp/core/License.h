#ifndef BIMALAUNCH_LICENSE_H
#define BIMALAUNCH_LICENSE_H

#include <string>

namespace bimalaunch {

struct LicenseVerdict {
    bool valid = false;
    std::string errorType;
};

class LicenseProvider {
public:
    virtual ~LicenseProvider() = default;
    virtual LicenseVerdict verify(const std::string& licenseKey) = 0;
    virtual const char* mode() const = 0;
};

class DummyLicenseProvider : public LicenseProvider {
public:
    LicenseVerdict verify(const std::string& licenseKey) override;
    const char* mode() const override { return "dummy"; }
};

}

#endif
