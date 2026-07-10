#include "core/ScriptManager.h"

#include <nlohmann/json.hpp>

namespace bimalaunch {

using json = nlohmann::json;

namespace {
const char* kScrapeTemplate = R"JS((function() {
  try {
    var soals = Array.from(document.querySelectorAll('__Q_CONTAINER__')).sort(function(a, b) {
      return Number(a.id.replace(/[^0-9]/g, '')) - Number(b.id.replace(/[^0-9]/g, ''));
    });
    if (soals.length === 0) return "NOT_TEST_PAGE";
    function cleanText(value) { return (value || "").trim().replace(/\s+/g, " "); }
    function absoluteUrl(value) {
      if (!value) return "";
      try { return new URL(value, document.baseURI).href; } catch(e) { return value; }
    }
    function textWithImages(root, questionNumber, area, choice) {
      if (!root) return { text: "", images: [] };
      var clone = root.cloneNode(true);
      var images = [];
      Array.from(clone.querySelectorAll('img')).forEach(function(img, imageIndex) {
        try {
          var url = absoluteUrl(img.getAttribute('src') || img.src || "");
          var index = imageIndex + 1;
          var imageKey = "q" + questionNumber + "." + (area === "choice" ? "choice." + choice : "question") + ".img" + index;
          var placeholder = "[image: " + imageKey + " url=" + url + "]";
          var span = document.createElement('span');
          span.innerText = " " + placeholder + " ";
          if (img.parentNode) img.parentNode.replaceChild(span, img);
          images.push({ imageKey: imageKey, url: url, questionNumber: questionNumber, area: area, choice: choice || null, index: index });
        } catch(e) {}
      });
      return { text: cleanText(clone.innerText || clone.textContent || ""), images: images };
    }
    var result = [];
    for (var i = 0; i < soals.length; i++) {
      var s = soals[i];
      var questionNumber = i + 1;
      var questionData = textWithImages(s.querySelector('__Q_BODY__'), questionNumber, "question", null);
      var choices = {};
      var images = questionData.images.slice();
      var optionsList = s.querySelectorAll('__OPTIONS__');
      for (var j = 0; j < optionsList.length; j++) {
        var opt = optionsList[j];
        var innerOpt = opt.querySelector('__OPTION_LETTER__');
        var letter = innerOpt ? cleanText(innerOpt.innerText).toUpperCase().charAt(0) : "";
        if (letter) {
          var choiceData = textWithImages(opt, questionNumber, "choice", letter);
          choices[letter] = choiceData.text;
          images = images.concat(choiceData.images);
        }
      }
      result.push({ number: questionNumber, question: questionData.text, choices: choices, images: images });
    }
    return JSON.stringify(result);
  } catch(err) { return "ERROR: " + err.message; }
})();)JS";

std::string replaceAll(std::string subject, const std::string& from, const std::string& to) {
    if (from.empty()) {
        return subject;
    }
    size_t pos = 0;
    while ((pos = subject.find(from, pos)) != std::string::npos) {
        subject.replace(pos, from.length(), to);
        pos += to.length();
    }
    return subject;
}
}

void ScriptManager::configure(const WebsiteConfig& website) {
    website_ = website;
}

std::string ScriptManager::selectorOr(const std::string& key, const std::string& fallback) const {
    auto it = website_.selectors.find(key);
    if (it != website_.selectors.end() && !it->second.empty()) {
        return it->second;
    }
    return fallback;
}

std::string ScriptManager::jsEscape(const std::string& value) {
    std::string out;
    out.reserve(value.size() + 8);
    for (char c : value) {
        switch (c) {
            case '\\': out += "\\\\"; break;
            case '\'': out += "\\'"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            default: out += c; break;
        }
    }
    return out;
}

std::string ScriptManager::scrapeScript() const {
    std::string script = kScrapeTemplate;
    script = replaceAll(script, "__Q_CONTAINER__", selectorOr("question", "[id^=\"soal-\"]"));
    script = replaceAll(script, "__Q_BODY__", selectorOr("questionBody", ".q"));
    script = replaceAll(script, "__OPTIONS__", selectorOr("options", ".options"));
    script = replaceAll(script, "__OPTION_LETTER__", selectorOr("optionLetter", ".inneroption"));
    return script;
}

std::string ScriptManager::autoLoginScript(const std::string& username, const std::string& password) const {
    if (!website_.autoLogin.enabled) {
        return std::string();
    }
    std::string user = jsEscape(username);
    std::string pass = jsEscape(password);
    std::string userSel = jsEscape(website_.autoLogin.usernameSelector);
    std::string passSel = jsEscape(website_.autoLogin.passwordSelector);
    std::string submitSel = jsEscape(website_.autoLogin.submitSelector);
    std::string script;
    script += "(function(){try{";
    script += "var u=document.querySelector('" + userSel + "');";
    script += "var p=document.querySelector('" + passSel + "');";
    script += "if(u){u.value='" + user + "';u.dispatchEvent(new Event('input',{bubbles:true}));}";
    script += "if(p){p.value='" + pass + "';p.dispatchEvent(new Event('input',{bubbles:true}));}";
    if (!website_.autoLogin.submitSelector.empty()) {
        script += "var b=document.querySelector('" + submitSel + "');if(b){b.click();}";
    }
    script += "return 'OK';}catch(e){return 'ERROR: '+e.message;}})();";
    return script;
}

std::string ScriptManager::scriptById(const std::string& id) const {
    if (id == "scrape") {
        return scrapeScript();
    }
    size_t index = 0;
    for (char c : id) {
        if (c < '0' || c > '9') {
            return std::string();
        }
        index = index * 10 + static_cast<size_t>(c - '0');
    }
    if (index < website_.scripts.size()) {
        return website_.scripts[index];
    }
    return std::string();
}

std::string ScriptManager::scriptsJson() const {
    json ids = json::array();
    ids.push_back("scrape");
    for (size_t i = 0; i < website_.scripts.size(); ++i) {
        ids.push_back(std::to_string(i));
    }
    return ids.dump();
}

}
