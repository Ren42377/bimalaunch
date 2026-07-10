(function () {
  const buttonId = "local-import-siswa-btn";
  const templateButtonId = "local-template-siswa-btn";
  const soalTemplateButtonId = "local-template-soal-btn";
  const inputId = "local-import-siswa-input";
  const soalInputId = "local-import-soal-input";
  const localBackdropId = "local-practice-modal-backdrop";

  function routeText() {
    return `${location.pathname}${location.search}${location.hash}`;
  }

  function isDataSiswaPage() {
    return routeText().includes("/admin/datasiswa");
  }

  function isDatabasePage() {
    return routeText().includes("/admin/database");
  }

  function isDashboardPage() {
    return routeText().includes("/admin/dashboard");
  }

  function isAdminRoute() {
    return routeText().includes("/admin");
  }

  function queryValue(name) {
    const hash = location.hash || "";
    const hashQuery = hash.includes("?") ? hash.slice(hash.indexOf("?") + 1) : "";
    const params = new URLSearchParams(hashQuery || location.search);
    return params.get(name) || "";
  }

  function currentCbtIndex() {
    return "0";
  }

  function currentTestMapel() {
    return localStorage.getItem("testsoal") || queryValue("kode") || queryValue("mapel");
  }

  function parseTokenPayload(token) {
    if (!token) return null;
    try {
      const encoded = token.split(".")[1] || "";
      const normalized = encoded.replace(/-/g, "+").replace(/_/g, "/");
      const padded = normalized + "=".repeat((4 - (normalized.length % 4 || 4)) % 4);
      return JSON.parse(atob(padded));
    } catch (error) {
      return null;
    }
  }

  function hasActiveAdminToken() {
    const payload = parseTokenPayload(localStorage.getItem("token") || "");
    if (!payload || payload.hakAkses !== "admin") return false;
    if (!payload.exp) return true;
    return payload.exp * 1000 > Date.now();
  }

  function setNativeValue(input, value) {
    const proto = input instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
    const setter = Object.getOwnPropertyDescriptor(proto, "value").set;
    setter.call(input, value);
    input.setAttribute("value", value);
    input.dispatchEvent(new Event("input", { bubbles: true }));
  }

  function downloadText(filename, text, type) {
    const blob = new Blob([text], { type: type || "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  function downloadWorkbook(filename, sheetName, rows, csvFallback) {
    if (!window.XLSX) {
      downloadText(filename.replace(/\.xlsx$/i, ".csv"), csvFallback, "text/csv;charset=utf-8");
      return;
    }

    const worksheet = window.XLSX.utils.aoa_to_sheet(rows);
    const workbook = window.XLSX.utils.book_new();
    window.XLSX.utils.book_append_sheet(workbook, worksheet, sheetName);
    window.XLSX.writeFile(workbook, filename);
  }

  function splitCsv(text, delimiter) {
    const rows = [];
    let row = [];
    let cell = "";
    let quoted = false;

    for (let i = 0; i < text.length; i += 1) {
      const char = text[i];
      const next = text[i + 1];

      if (char === '"') {
        if (quoted && next === '"') {
          cell += '"';
          i += 1;
        } else {
          quoted = !quoted;
        }
        continue;
      }

      if (!quoted && char === delimiter) {
        row.push(cell.trim());
        cell = "";
        continue;
      }

      if (!quoted && (char === "\n" || char === "\r")) {
        if (char === "\r" && next === "\n") i += 1;
        row.push(cell.trim());
        if (row.some(Boolean)) rows.push(row);
        row = [];
        cell = "";
        continue;
      }

      cell += char;
    }

    row.push(cell.trim());
    if (row.some(Boolean)) rows.push(row);
    return rows;
  }

  function countDelimiter(text, delimiter) {
    let quoted = false;
    let count = 0;
    for (let i = 0; i < text.length; i += 1) {
      const char = text[i];
      if (char === '"') quoted = !quoted;
      else if (!quoted && char === delimiter) count += 1;
    }
    return count;
  }

  function parseCsv(text) {
    const sample = text.split(/\r?\n/).slice(0, 5).join("\n");
    const delimiter = [",", ";", "\t"].sort(
      (a, b) => countDelimiter(sample, b) - countDelimiter(sample, a)
    )[0];
    return splitCsv(text.replace(/^\uFEFF/, ""), delimiter);
  }

  async function readTableRows(file) {
    if (/\.xlsx?$/i.test(file.name)) {
      if (!window.XLSX) throw new Error("Parser XLSX belum dimuat.");
      const workbook = window.XLSX.read(await file.arrayBuffer(), { type: "array" });
      const sheet = workbook.Sheets[workbook.SheetNames[0]];
      if (!sheet) throw new Error("Sheet pertama tidak ditemukan.");
      return window.XLSX.utils
        .sheet_to_json(sheet, { header: 1, defval: "", raw: false })
        .map((row) => row.map((cell) => String(cell || "").trim()))
        .filter((row) => row.some(Boolean));
    }

    return parseCsv(await file.text());
  }

  function normalizeHeader(value) {
    return String(value || "")
      .trim()
      .toLowerCase()
      .replace(/^\uFEFF/, "")
      .replace(/[^a-z0-9]/g, "");
  }

  function pick(row, names) {
    for (const name of names) {
      if (row[name]) return row[name];
    }
    return "";
  }

  function escapeHtml(value) {
    return String(value || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function toStudents(rows, mapel) {
    if (rows.length < 2) return { students: [], skipped: 0 };

    const headers = rows[0].map(normalizeHeader);
    const students = [];
    let skipped = 0;

    for (const values of rows.slice(1)) {
      const row = {};
      headers.forEach((header, index) => {
        row[header] = (values[index] || "").trim();
      });

      const user = pick(row, ["user", "username", "userid", "login", "nopeserta", "nomorpeserta"]);
      const pass = pick(row, ["pass", "password", "sandi", "katasandi"]);
      const nama = pick(row, ["nama", "namalengkap", "name"]);

      if (!user || !pass || !nama) {
        skipped += 1;
        continue;
      }

      students.push({
        mapel: pick(row, ["mapel", "kode", "kodemapel"]) || mapel,
        user,
        username: user,
        pass,
        password: pass,
        nama,
        nis: pick(row, ["nis", "nisn", "nomorinduk"]),
        nik: pick(row, ["nik"]),
        nik2: pick(row, ["nik2"]),
        server: pick(row, ["server", "idserver"]) || "SRV-LOCAL",
        sesi: pick(row, ["sesi"]) || "1"
      });
    }

    return { students, skipped };
  }

  async function importSiswaFile(file) {
    const mapel = queryValue("kode") || queryValue("mapel");
    if (!mapel) {
      alert("Kode mapel tidak ditemukan.");
      return;
    }

    const rows = await readTableRows(file);
    const result = toStudents(rows, mapel);
    if (!result.students.length) {
      alert("Tidak ada siswa valid. Kolom wajib: nama, user, pass.");
      return;
    }

    const response = await fetch("/api/admin?cbtindex=0&_=%2Fsiswa%2Fadd", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        data: {
          mapel,
          students: result.students
        }
      })
    });

    if (!response.ok) {
      throw new Error(`Import gagal (${response.status})`);
    }

    alert(`Import selesai: ${result.students.length} siswa${result.skipped ? `, ${result.skipped} dilewati` : ""}.`);
    location.reload();
  }

  function normalizeOption(option, index) {
    const labels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    if (typeof option === "string") {
      return { optionasli: labels[index] || String(index + 1), answer: option };
    }
    return {
      optionasli: option.optionasli || option.label || option.kode || labels[index] || String(index + 1),
      answer: option.answer || option.text || option.jawaban || option.value || ""
    };
  }

  function normalizeQuestion(question, index, fromCsv) {
    const labels = ["a", "b", "c", "d", "e"];
    let options = Array.isArray(question.options)
      ? question.options.map(normalizeOption)
      : labels
          .map((label, optionIndex) => {
            const answer =
              question[label] ||
              question[`opsi${label}`] ||
              question[`option${label}`] ||
              question[`pilihan${label}`] ||
              question[`jawaban${label}`];
            return answer
              ? {
                  optionasli: label.toUpperCase(),
                  answer: fromCsv ? escapeHtml(answer) : answer
                }
              : null;
          })
          .filter(Boolean);

    options = options.filter((option) => option.answer);
    if (!options.length) return null;

    const soal = question.soal || question.question || question.pertanyaan || question.text || "";
    if (!soal) return null;

    return {
      ...question,
      noasli: Number(question.noasli || question.no || index + 1),
      soal: fromCsv ? `<p>${escapeHtml(soal)}</p>` : soal,
      options
    };
  }

  function readJsonQuestions(text) {
    const parsed = JSON.parse(text);
    const source =
      Array.isArray(parsed) ? parsed :
      Array.isArray(parsed.soal) ? parsed.soal :
      Array.isArray(parsed.questions) ? parsed.questions :
      parsed.data && Array.isArray(parsed.data.soal) ? parsed.data.soal :
      parsed.data && typeof parsed.data.soal === "string" ? JSON.parse(parsed.data.soal) :
      null;

    if (!Array.isArray(source)) throw new Error("Format JSON soal tidak dikenali.");
    return source.map((item, index) => normalizeQuestion(item, index, false)).filter(Boolean);
  }

  function readCsvQuestions(text) {
    return readRowsQuestions(parseCsv(text));
  }

  function readRowsQuestions(rows) {
    if (rows.length < 2) return [];
    const headers = rows[0].map(normalizeHeader);
    return rows
      .slice(1)
      .map((values, index) => {
        const row = {};
        headers.forEach((header, valueIndex) => {
          row[header] = (values[valueIndex] || "").trim();
        });
        return normalizeQuestion(row, index, true);
      })
      .filter(Boolean);
  }

  async function importSoal(file, mapel) {
    const questions = /\.json$/i.test(file.name)
      ? readJsonQuestions(await file.text())
      : readRowsQuestions(await readTableRows(file));
    if (!questions.length) {
      alert("Tidak ada soal valid. Kolom minimal: soal,a,b,c,d,e.");
      return;
    }

    const response = await fetch(`/api/soal/${currentCbtIndex()}/${encodeURIComponent(mapel)}`, {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ soal: questions })
    });

    if (!response.ok) {
      throw new Error(`Import soal gagal (${response.status})`);
    }

    alert(`Import soal selesai: ${questions.length} soal.`);
  }

  function ensureInput() {
    let input = document.getElementById(inputId);
    if (input) return input;

    input = document.createElement("input");
    input.id = inputId;
    input.type = "file";
    input.accept = ".csv,.xls,.xlsx,text/csv,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    input.hidden = true;
    input.addEventListener("change", async () => {
      const file = input.files && input.files[0];
      input.value = "";
      if (!file) return;

      try {
        await importSiswaFile(file);
      } catch (error) {
        alert(error && error.message ? error.message : "Import gagal.");
      }
    });
    document.body.appendChild(input);
    return input;
  }

  function ensureSoalInput() {
    let input = document.getElementById(soalInputId);
    if (input) return input;

    input = document.createElement("input");
    input.id = soalInputId;
    input.type = "file";
    input.accept = ".json,.csv,.xls,.xlsx,application/json,text/csv,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    input.hidden = true;
    input.addEventListener("change", async () => {
      const file = input.files && input.files[0];
      const mapel = input.dataset.mapel || "";
      input.value = "";
      input.dataset.mapel = "";
      if (!file || !mapel) return;

      try {
        await importSoal(file, mapel);
      } catch (error) {
        alert(error && error.message ? error.message : "Import soal gagal.");
      }
    });
    document.body.appendChild(input);
    return input;
  }

  function targetHeader() {
    const cardHeaders = Array.from(document.querySelectorAll(".card-header"));
    const match = cardHeaders.find((element) => /Data Siswa MAPEL/i.test(element.textContent || ""));
    if (match) return match;

    const containers = Array.from(document.querySelectorAll(".container"));
    return containers.find((element) => /Data Siswa MAPEL/i.test(element.textContent || ""));
  }

  function databaseHeader() {
    const cardHeaders = Array.from(document.querySelectorAll(".card-header"));
    return cardHeaders.find((element) => /^Database\b/i.test((element.textContent || "").trim()));
  }

  function rewriteLocalAccountFields() {
    const origin = location.origin;
    const fields = [
      ["urlExcel", `${origin}/api/admin?cbtindex=${currentCbtIndex()}`],
      ["urlWord", `${origin}/api/soal/${currentCbtIndex()}/KODE_MAPEL`],
      ["usernameWord", "local"],
      ["passwordWord", "local"]
    ];

    for (const [id, value] of fields) {
      const input = document.getElementById(id);
      if (input && input.value !== value) setNativeValue(input, value);
    }
  }

  function rewriteTestSoalButton() {
    const buttons = Array.from(document.querySelectorAll("button"));
    for (const button of buttons) {
      if (/Cek Soal Wordpress/i.test(button.textContent || "")) {
        button.dataset.localTestSoal = "1";
        button.title = "Cek Soal Lokal";
        for (const node of Array.from(button.childNodes)) {
          if (node.nodeType === Node.TEXT_NODE && /Cek Soal Wordpress/i.test(node.textContent || "")) {
            node.textContent = "Cek Soal Lokal";
          }
        }
      }
    }
  }

  async function fetchJson(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) throw new Error(`Request gagal (${response.status})`);
    return response.json();
  }

  let dashboardBootstrapState = "idle";
  let dashboardRetryAt = 0;

  function isElementVisible(element) {
    if (!element) return false;
    const style = window.getComputedStyle(element);
    if (style.display === "none" || style.visibility === "hidden" || style.opacity === "0") return false;
    return element.getClientRects().length > 0;
  }

  function redirectToDashboard() {
    const targetHash = "#/admin/dashboard";
    if (location.hash !== targetHash) {
      location.hash = targetHash;
    }
  }

  async function requestDashboardBypass() {
    return fetchJson("/api/admin?cbtindex=0&_=%2Fuser%2FloginAdmin", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        data: {
          username: "",
          password: ""
        }
      })
    });
  }

  async function ensureDashboardBypass() {
    if (dashboardBootstrapState === "running") return;
    if (Date.now() < dashboardRetryAt) return;

    if (hasActiveAdminToken() && isDashboardPage()) return;

    dashboardBootstrapState = "running";
    try {
      const data = await requestDashboardBypass();
      if (!data || data.status !== "ok" || !data.token) {
        dashboardBootstrapState = "idle";
        dashboardRetryAt = Date.now() + 3000;
        return;
      }

      localStorage.setItem("localPracticeDashboardBypass", "1");
      localStorage.setItem("token", data.token);
      if (data.examkey) localStorage.setItem("examkey", data.examkey);
      dashboardBootstrapState = "enabled";
      redirectToDashboard();
      setTimeout(() => {
        if (!isDashboardPage()) redirectToDashboard();
      }, 180);
    } catch (error) {
      dashboardBootstrapState = "idle";
      dashboardRetryAt = Date.now() + 3000;
    }
  }

  function ensureLocalModalBackdrop() {
    const nativeBackdropVisible = Array.from(document.querySelectorAll(".modal-backdrop")).some(isElementVisible);
    const dialog =
      document.querySelector(".modal.show") ||
      document.querySelector(".modal[style*='display: block']") ||
      document.querySelector("#daftarSoalModal.show") ||
      document.querySelector("#daftarSoalModal[style*='display: block']");
    let backdrop = document.getElementById(localBackdropId);

    if (dialog && isElementVisible(dialog) && !nativeBackdropVisible) {
      if (!backdrop) {
        backdrop = document.createElement("div");
        backdrop.id = localBackdropId;
        backdrop.className = "local-modal-backdrop";
      }
      if (backdrop.parentElement !== document.body) {
        document.body.appendChild(backdrop);
      }
      backdrop.classList.add("show");
      return;
    }

    if (backdrop) backdrop.classList.remove("show");
  }

  async function setBroadcastMessage(message) {
    return fetchJson("/api/admin?cbtindex=0&_=%2Fbroadcast%2Fset", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ data: { message } })
    });
  }

  async function loadBroadcastMessage() {
    return fetchJson("/api/admin?cbtindex=0&_=%2Fbroadcast%2Fget", {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json"
      },
      body: "{}"
    });
  }

  function ensureBroadcastMessage() {
    if (!isDashboardPage()) return;
    const textarea = document.getElementById("pesan");
    if (!textarea || textarea.dataset.localBroadcastLoaded === "1") return;

    textarea.dataset.localBroadcastLoaded = "1";
    loadBroadcastMessage()
      .then((data) => {
        if (!textarea.value && data && data.message) setNativeValue(textarea, data.message);
      })
      .catch(() => {});
  }

  function openLocalSoal() {
    const mapel = currentTestMapel();
    if (!mapel) {
      alert("Kode mapel tidak ditemukan.");
      return;
    }
    window.open(`/api/soal/${currentCbtIndex()}/${encodeURIComponent(mapel)}`, "_blank");
  }

  function ensureButton() {
    if (!isDataSiswaPage() || document.getElementById(buttonId)) return;

    const target = targetHeader();
    if (!target) return;

    if (!document.getElementById(templateButtonId)) {
      const templateButton = document.createElement("button");
      templateButton.id = templateButtonId;
      templateButton.type = "button";
      templateButton.className = "btn btn-info btn-sm float-right ml-2";
      templateButton.innerHTML = '<i class="bi bi-filetype-csv mr-1"></i>TEMPLATE SISWA';
      templateButton.addEventListener("click", () => {
        const rows = [
          ["nama", "user", "pass", "nis", "nik", "nik2", "server", "sesi"],
          ["Budi Santoso", "USER02", "PASS02", "12345", "", "", "SRV-LOCAL", "1"]
        ];
        downloadWorkbook(
          "template-import-siswa.xlsx",
          "siswa",
          rows,
          "nama,user,pass,nis,nik,nik2,server,sesi\nBudi Santoso,USER02,PASS02,12345,,,SRV-LOCAL,1\n"
        );
      });
      target.appendChild(templateButton);
    }

    const button = document.createElement("button");
    button.id = buttonId;
    button.type = "button";
    button.className = "btn btn-success btn-sm float-right ml-2";
    button.innerHTML = '<i class="bi bi-upload mr-1"></i>IMPORT FILE';
    button.addEventListener("click", () => ensureInput().click());
    target.appendChild(button);
  }

  function ensureSoalTemplateButton() {
    if (!isDatabasePage() || document.getElementById(soalTemplateButtonId)) return;

    const target = databaseHeader();
    if (!target) return;

    const button = document.createElement("button");
    button.id = soalTemplateButtonId;
    button.type = "button";
    button.className = "btn btn-info btn-sm float-right ml-2";
    button.innerHTML = '<i class="bi bi-filetype-csv mr-1"></i>TEMPLATE SOAL';
    button.addEventListener("click", () => {
      const rows = [
        ["soal", "a", "b", "c", "d", "e"],
        ["Pertanyaan contoh?", "Pilihan A", "Pilihan B", "Pilihan C", "Pilihan D", "Pilihan E"]
      ];
      downloadWorkbook(
        "template-import-soal.xlsx",
        "soal",
        rows,
        "soal,a,b,c,d,e\nPertanyaan contoh?,Pilihan A,Pilihan B,Pilihan C,Pilihan D,Pilihan E\n"
      );
    });
    target.appendChild(button);
  }

  function ensureSoalButtons() {
    if (!isDatabasePage()) return;

    const rows = Array.from(document.querySelectorAll("tr[id^='row-']"));
    for (const row of rows) {
      const mapel = row.id.replace(/^row-/, "") || (row.children[1] && row.children[1].textContent.trim());
      if (!mapel || row.querySelector("[data-local-import-soal]")) continue;

      const buttonGroup = row.querySelector("td:last-child .btn-group") || row.querySelector("td:last-child");
      if (!buttonGroup) continue;

      const button = document.createElement("button");
      button.type = "button";
      button.className = "btn btn-secondary btn-sm";
      button.title = "Import Soal File";
      button.dataset.localImportSoal = mapel;
      button.innerHTML = '<i class="bi bi-file-earmark-arrow-up"></i>';
      button.addEventListener("click", () => {
        const input = ensureSoalInput();
        input.dataset.mapel = mapel;
        input.click();
      });
      buttonGroup.appendChild(button);
    }
  }

  function applyLocalOverrides() {
    ensureDashboardBypass();
    ensureLocalModalBackdrop();
    ensureButton();
    ensureSoalTemplateButton();
    ensureSoalButtons();
    rewriteLocalAccountFields();
    rewriteTestSoalButton();
    ensureBroadcastMessage();
  }

  document.addEventListener(
    "click",
    (event) => {
      const button = event.target && event.target.closest && event.target.closest("button[data-local-test-soal='1']");
      if (!button) return;
      event.preventDefault();
      event.stopPropagation();
      event.stopImmediatePropagation();
      openLocalSoal();
    },
    true
  );

  document.addEventListener(
    "click",
    async (event) => {
      const button = event.target && event.target.closest && event.target.closest("button");
      if (!button || !isDashboardPage()) return;

      const label = (button.textContent || "").trim().toUpperCase();
      if (label !== "KIRIM" && label !== "HAPUS") return;

      const textarea = document.getElementById("pesan");
      if (!textarea) return;

      event.preventDefault();
      event.stopPropagation();
      event.stopImmediatePropagation();

      try {
        const message = label === "HAPUS" ? "" : textarea.value;
        await setBroadcastMessage(message);
        if (label === "HAPUS") setNativeValue(textarea, "");
        alert(label === "HAPUS" ? "Broadcast message dihapus." : "Broadcast message tersimpan.");
      } catch (error) {
        alert(error && error.message ? error.message : "Broadcast message gagal.");
      }
    },
    true
  );

  const observer = new MutationObserver(applyLocalOverrides);
  observer.observe(document.documentElement, { childList: true, subtree: true });
  window.addEventListener("hashchange", applyLocalOverrides);
  window.addEventListener("popstate", applyLocalOverrides);
  document.addEventListener("DOMContentLoaded", applyLocalOverrides);
  setTimeout(applyLocalOverrides, 500);
})();
