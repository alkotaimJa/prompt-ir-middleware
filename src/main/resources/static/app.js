(function () {
  "use strict";

  const EXAMPLES = {
    "Summarisation": "Summarise the main idea of REST APIs for a non-technical manager.",
    "Factual QA": "What is the difference between REST and SOAP?",
    "Analytical explanation": "Explain how HTTP status codes work.",
    "Instruction following": "List advantages of REST using 5 bullet points.",
    "Comparative reasoning": "Compare REST and GraphQL scalability.",
    "Coding": "Write a Java method that checks whether a string is a palindrome.",
    "Other": "How should I prepare for a software engineering job interview?"
  };
  const CHOICES = [
    ["A_BETTER", "A is better"],
    ["B_BETTER", "B is better"],
    ["BOTH_GOOD", "Both equally good"],
    ["BOTH_BAD", "Both are poor"]
  ];

  const $ = (id) => document.getElementById(id);
  const store = {
    get(k) { try { return localStorage.getItem(k); } catch (e) { return null; } },
    set(k, v) { try { localStorage.setItem(k, v); } catch (e) { /* storage unavailable */ } },
    del(k) { try { localStorage.removeItem(k); } catch (e) { /* storage unavailable */ } }
  };

  let study = null;
  let participantId = store.get("participantId");
  let category = null;
  let trial = null;

  function show(view) {
    document.querySelectorAll("main > section").forEach(s => s.classList.add("hidden"));
    $("view-" + view).classList.remove("hidden");
    window.scrollTo(0, 0);
  }

  async function api(method, url, body) {
    const res = await fetch(url, {
      method,
      headers: body ? { "Content-Type": "application/json" } : {},
      body: body ? JSON.stringify(body) : undefined
    });
    let data = null;
    try { data = await res.json(); } catch (e) { /* empty body */ }
    if (!res.ok) {
      const err = new Error((data && (data.detail || data.title)) || ("Request failed (" + res.status + ")"));
      err.status = res.status;
      throw err;
    }
    return data;
  }

  function setProgress(evaluated, max) {
    const p = $("progress");
    p.innerHTML = "<strong>" + evaluated + "</strong> of up to " + max + " questions rated";
    p.classList.remove("hidden");
  }

  // ── Consent ─────────────────────────────────────────────
  $("consent-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    $("consent-error").textContent = "";
    if (!$("consent").checked) {
      $("consent-error").textContent = "Tick the box to confirm you agree to take part.";
      return;
    }
    try {
      const fam = $("familiarity").value;
      const p = await api("POST", "/api/participants", {
        consent: true,
        background: $("background").value || null,
        llmFamiliarity: fam ? Number(fam) : null,
        accessCode: $("access").value || null
      });
      participantId = p.participantId;
      store.set("participantId", participantId);
      setProgress(0, p.maxTrials);
      openPrompt();
    } catch (err) {
      $("consent-error").textContent = err.message;
    }
  });

  // ── Prompt ──────────────────────────────────────────────
  function openPrompt() {
    $("prompt").value = "";
    $("prompt-error").textContent = "";
    show("prompt");
    $("prompt").focus();
  }

  function renderCategories() {
    const wrap = $("categories");
    wrap.innerHTML = "";
    study.taskCategories.forEach(c => {
      const b = document.createElement("button");
      b.type = "button";
      b.className = "chip";
      b.textContent = c;
      b.setAttribute("aria-pressed", String(c === category));
      b.addEventListener("click", () => { category = c; renderCategories(); });
      wrap.appendChild(b);
    });
  }

  $("example").addEventListener("click", () => {
    const c = category || "Factual QA";
    if (!category) { category = c; renderCategories(); }
    $("prompt").value = EXAMPLES[c] || EXAMPLES["Other"];
  });

  $("prompt-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const text = $("prompt").value.trim();
    $("prompt-error").textContent = "";
    if (!category) { $("prompt-error").textContent = "Choose the type of task first."; return; }
    if (!text) { $("prompt-error").textContent = "Type a question to continue."; return; }
    show("loading");
    try {
      trial = await api("POST", "/api/trials", { participantId, taskCategory: category, prompt: text });
      renderTrial();
    } catch (err) {
      if (err.status === 404) { store.del("participantId"); participantId = null; }
      show("prompt");
      $("prompt").value = text;
      $("prompt-error").textContent = err.message;
    }
  });

  // ── Compare & vote ──────────────────────────────────────
  function answerCard(resp, side) {
    const el = document.createElement("article");
    el.className = "answer " + side;
    const body = resp.ok
      ? '<div class="body">' + window.renderMarkdown(resp.text) + "</div>"
      : '<div class="body failed">' + window.escapeHtml(resp.text) + "</div>";
    el.innerHTML = '<header><span class="mark" aria-hidden="true">' + resp.label +
      "</span><span>Response " + resp.label + "</span></header>" + body;
    return el;
  }

  function renderTrial() {
    $("shown-prompt").textContent = trial.prompt;
    const wrap = $("models");
    wrap.innerHTML = "";
    trial.models.forEach(m => {
      const sec = document.createElement("section");
      sec.className = "model";
      sec.innerHTML = '<div class="model-head"><h3>' + m.modelLabel + "</h3>" +
        (m.votable ? "" : '<span class="small muted">Not rated: one answer is missing</span>') + "</div>";
      const pair = document.createElement("div");
      pair.className = "pair";
      pair.appendChild(answerCard(m.responseA, "a"));
      pair.appendChild(answerCard(m.responseB, "b"));
      sec.appendChild(pair);

      if (m.votable) {
        const v = document.createElement("div");
        v.className = "vote";
        const name = "vote-" + m.slot;
        v.innerHTML = '<fieldset><legend>Which answer from ' + m.modelLabel + " is better?</legend>" +
          '<div class="seg">' + CHOICES.map(([val, label], i) =>
            '<input type="radio" id="' + name + "-" + i + '" name="' + name + '" value="' + val + '">' +
            '<label for="' + name + "-" + i + '">' + label + "</label>").join("") + "</div></fieldset>" +
          '<label for="reason-' + m.slot + '">Why? A few words is enough</label>' +
          '<textarea class="reason" id="reason-' + m.slot + '" maxlength="5000" ' +
          'placeholder="e.g. clearer structure, more accurate, too long, missed part of the question"></textarea>';
        sec.appendChild(v);
      }
      wrap.appendChild(sec);
    });

    const best = $("best-seg");
    best.innerHTML = "";
    const opts = trial.models.map(m => [String(m.slot), m.modelLabel]).concat([["0", "None of them"]]);
    opts.forEach(([val, label], i) => {
      best.insertAdjacentHTML("beforeend",
        '<input type="radio" id="best-' + i + '" name="best" value="' + val + '">' +
        '<label for="best-' + i + '">' + label + "</label>");
    });
    best.style.gridTemplateColumns = "repeat(" + Math.min(opts.length, 4) + ", 1fr)";
    $("best-reason").value = "";
    $("vote-error").textContent = "";
    $("vote-submit").disabled = false;
    show("compare");
  }

  $("vote-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const err = $("vote-error");
    err.textContent = "";
    const minReason = study.minReasonChars || 0;
    const votes = [];
    for (const m of trial.models.filter(x => x.votable)) {
      const picked = document.querySelector('input[name="vote-' + m.slot + '"]:checked');
      const reason = $("reason-" + m.slot).value.trim();
      if (!picked) { err.textContent = "Choose an option for " + m.modelLabel + "."; return; }
      if (reason.length < minReason) {
        err.textContent = "Add a short reason for " + m.modelLabel + ".";
        $("reason-" + m.slot).focus();
        return;
      }
      votes.push({ slot: m.slot, choice: picked.value, reason });
    }
    const best = document.querySelector('input[name="best"]:checked');
    if (!best) { err.textContent = "Choose the best model overall."; return; }

    $("vote-submit").disabled = true;
    try {
      const r = await api("POST", "/api/trials/" + trial.trialId + "/evaluation", {
        participantId, votes, bestSlot: Number(best.value), bestReason: $("best-reason").value.trim() || null
      });
      setProgress(r.trialsEvaluated, r.trialsEvaluated + r.trialsRemaining);
      $("done-text").textContent = r.trialsRemaining > 0
        ? "You have rated " + r.trialsEvaluated + " question" + (r.trialsEvaluated === 1 ? "" : "s") +
          ". Each extra question makes the results more reliable."
        : "You have reached the maximum number of questions. Thank you for taking part!";
      $("again").classList.toggle("hidden", r.trialsRemaining <= 0);
      show("done");
    } catch (ex) {
      $("vote-submit").disabled = false;
      err.textContent = ex.message;
    }
  });

  $("again").addEventListener("click", openPrompt);

  // ── Boot ────────────────────────────────────────────────
  (async function init() {
    try {
      study = await api("GET", "/api/study");
      $("prompt").maxLength = study.maxPromptChars;
      if (study.accessCodeRequired) $("access-wrap").classList.remove("hidden");
      renderCategories();
      if (participantId) {
        try {
          const p = await api("GET", "/api/participants/" + participantId);
          setProgress(p.trialsEvaluated, p.maxTrials);
          if (p.trialsStarted >= p.maxTrials) {
            $("done-text").textContent = "You have reached the maximum number of questions. Thank you for taking part!";
            $("again").classList.add("hidden");
            show("done");
          } else {
            openPrompt();
          }
          return;
        } catch (e) {
          store.del("participantId");
          participantId = null;
        }
      }
      show("consent");
    } catch (e) {
      $("fatal-text").textContent = "The study server could not be reached. " + e.message;
      show("error");
    }
  })();
})();
