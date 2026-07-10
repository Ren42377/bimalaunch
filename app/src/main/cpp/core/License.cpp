#include "core/License.h"

#include <algorithm>
#include <cctype>

namespace bimalaunch {

namespace {
std::string trim(const std::string& input) {
    size_t start = 0;
    size_t end = input.size();
    while (start < end && std::isspace(static_cast<unsigned char>(input[start]))) {
        ++start;
    }
    while (end > start && std::isspace(static_cast<unsigned char>(input[end - 1]))) {
        --end;
    }
    return input.substr(start, end - start);
}
}

LicenseVerdict DummyLicenseProvider::verify(const std::string& licenseKey) {
    LicenseVerdict verdict;
    const std::string normalized = trim(licenseKey);
    if (normalized == "developer") {
        verdict.valid = true;
        return verdict;
    }
    verdict.valid = false;
    verdict.errorType = normalized.empty() ? "empty_key" : "invalid_key";
    return verdict;
}

}
