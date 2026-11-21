(function () {
  "use strict";
  const $ = (id) => document.getElementById(id);
  const esc = (s) => window.escapeHtml(s == null ? "" : String(s));
  const NAMES = { openai: "OpenAI", claude: "Anthropic Claude", gemini: "Google Gemini", none: "None of them" };
  let token = "";
  try { token = sessionStorage.getItem("adminToken") || ""; } catch (e) { /* storage unavailable */ }

  async function get(url) {
    const res = await fetch(url, { headers: { "X-Admin-Token": token } });
    if (!res.ok) {
      let msg = "Request failed (" + res.status + ")";
      try { const d = await res.json(); msg = d.detail || msg; } catch (e) { /* no body */ }
      throw new Error(msg);
    }
    return res;
  }

  const pct = (x) => x == null ? "–" : (x * 100).toFixed(1) + "%";
  const pval = (p) => p == null ? "–" : (p < 0.001 ? "< 0.001" : p.toFixed(3));

  function split(s) {
    const t = s.total || 1;
    return '<div class="split" title="' + s.transformed + " / " + s.raw + " / " + s.tie + " / " + s.neither + '">' +
      '<i class="t" style="width:' + (100 * s.transformed / t) + '%"></i>' +
      '<i class="r" style="width:' + (100 * s.raw / t) + '%"></i>' +
      '<i class="tie" style="width:' + (100 * s.tie / t) + '%"></i>' +
      '<i class="n" style="width:' + (100 * s.neither / t) + '%"></i></div>';
  }

  function prefRows(label, s) {
    return "<tr><td>" + esc(label) + "</td><td>" + split(s) + "</td><td>" + s.transformed + "</td><td>" + s.raw +
      "</td><td>" + s.tie + "</td><td>" + s.neither + "</td><td>" + s.total + "</td><td><strong>" +
      pct(s.transformedWinRate) + "</strong></td><td>" +
      (s.ciLow == null ? "–" : pct(s.ciLow) + " – " + pct(s.ciHigh)) + "</td><td>" + pval(s.signTestP) + "</td></tr>";
  }
  const PREF_HEAD = "<thead><tr><th></th><th>Split</th><th>Transformed</th><th>Raw</th><th>Equal</th><th>Both poor</th>" +
    "<th>Votes</th><th>Transformed win rate</th><th>95% CI</th><th>Sign test p</th></tr></thead>";

  function render(s) {
    $("totals").innerHTML = [
      [s.participants, "participants"], [s.trials, "questions asked"],
      [s.evaluatedTrials, "questions rated"], [s.votes, "pairwise votes"]
    ].map(([n, l]) => '<div class="stat"><b>' + n + "</b><span>" + l + "</span></div>").join("");

    let rows = prefRows("All models", s.overall);
    Object.entries(s.byProvider).forEach(([p, v]) => { rows += prefRows(NAMES[p] || p, v); });
    $("pref-table").innerHTML = PREF_HEAD + "<tbody>" + rows + "</tbody>";

    rows = "";
    Object.entries(s.byTaskCategory).forEach(([c, v]) => { rows += prefRows(c, v); });
    $("cat-table").innerHTML = PREF_HEAD + "<tbody>" + (rows || '<tr><td colspan="10">No votes yet.</td></tr>') + "</tbody>";

    const bestTotal = Object.values(s.bestModelCounts).reduce((a, b) => a + b, 0) || 1;
    $("best-table").innerHTML = "<thead><tr><th>Model</th><th>Chosen as best</th><th>Share</th></tr></thead><tbody>" +
      (Object.entries(s.bestModelCounts).map(([p, n]) =>
        "<tr><td>" + esc(NAMES[p] || p) + "</td><td>" + n + "</td><td>" + pct(n / bestTotal) + "</td></tr>").join("") ||
        '<tr><td colspan="3">No ratings yet.</td></tr>') + "</tbody>";

    $("call-table").innerHTML = "<thead><tr><th>Provider</th><th>Condition</th><th>Calls</th><th>Failures</th>" +
      "<th>Avg latency</th><th>Avg answer length</th></tr></thead><tbody>" +
      (s.calls.map(c => "<tr><td>" + esc(NAMES[c.provider] || c.provider) + "</td><td>" + esc(c.condition.toLowerCase()) +
        "</td><td>" + c.calls + "</td><td>" + c.failures + "</td><td>" + (c.avgLatencyMs / 1000).toFixed(1) +
        " s</td><td>" + Math.round(c.avgResponseChars) + " chars</td></tr>").join("") ||
        '<tr><td colspan="6">No calls yet.</td></tr>') + "</tbody>";

    $("reason-table").innerHTML = "<thead><tr><th>When</th><th>Model</th><th>Task</th><th>Preferred</th><th>Reason</th></tr></thead><tbody>" +
      (s.recentReasons.map(r => "<tr><td>" + esc(new Date(r.at).toLocaleString()) + "</td><td>" + esc(NAMES[r.provider] || r.provider) +
        "</td><td>" + esc(r.taskCategory) + "</td><td>" + esc(r.preference.toLowerCase()) + "</td><td>" + esc(r.reason) + "</td></tr>").join("") ||
        '<tr><td colspan="5">No reasons yet.</td></tr>') + "</tbody>";
  }

  async function load() {
    const s = await (await get("/api/admin/stats")).json();
    render(s);
    $("login").classList.add("hidden");
    $("results").classList.remove("hidden");
    ["refresh", "dl-votes", "dl-responses"].forEach(id => $(id).classList.remove("hidden"));
  }

  async function download(path) {
    const res = await get(path);
    const blob = await res.blob();
    const cd = res.headers.get("Content-Disposition") || "";
    const m = cd.match(/filename="([^"]+)"/);
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = m ? m[1] : "export.csv";
    document.body.appendChild(a);
    a.click();
    setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 0);
  }

  $("login-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    token = $("token").value.trim();
    $("login-error").textContent = "";
    try {
      await load();
      try { sessionStorage.setItem("adminToken", token); } catch (x) { /* storage unavailable */ }
    } catch (err) {
      $("login-error").textContent = err.message;
    }
  });
  $("refresh").addEventListener("click", () => load().catch(e => alert(e.message)));
  $("dl-votes").addEventListener("click", () => download("/api/admin/export/votes.csv").catch(e => alert(e.message)));
  $("dl-responses").addEventListener("click", () => download("/api/admin/export/responses.csv").catch(e => alert(e.message)));

  if (token) load().catch(() => { /* show login */ });
})();
