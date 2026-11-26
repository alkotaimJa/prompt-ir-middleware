# Formalising Prompt Engineering as Middleware
### Provider‑Agnostic Prompt Transformation for Multi‑LLM Systems

## 📌 Overview
Large Language Models (LLMs) are increasingly integrated into modern software systems. However, their behaviour is **highly sensitive to prompt formulation**: small changes in wording can lead to significantly different outputs. This creates challenges for **reliability, reproducibility, and evaluation** in LLM‑based software systems.

The project proposes a **middleware‑level solution** that formalises prompt handling and supports multi‑provider execution.

---

## 🎯 Problem Addressed
Most existing LLM applications:
- Embed prompts directly inside application code
- Are tightly coupled to a single LLM provider
- Lack structured and reusable prompt representations
- Rely on automated evaluation that does not reflect real user judgement

These limitations reduce the maintainability and reliability of LLM‑based systems.

---

## 💡 Proposed Solution
This project introduces a **prompt transformation middleware** with the following core concepts:

### 🔹 Prompt Intermediate Representation (Prompt IR)
A structured, provider‑agnostic representation of prompts that explicitly separates:
- task instructions  
- roles / personas  
- user input  
- context  
- output constraints  


### 🔹 Provider‑Specific Prompt Compilation
The same Prompt IR is compiled into semantically equivalent prompts for different LLM APIs (e.g. OpenAI, Anthropic, Google), enabling consistent execution across providers.

### 🔹 Multi‑Provider Execution
Prompts can be executed concurrently across multiple LLMs, enabling controlled comparison and robustness analysis.

---

## 🧠 Evaluation Approach
The system is evaluated using a **human‑in‑the‑loop, preference‑based methodology**:
- Raw prompt vs transformed prompt
- Side‑by‑side comparison
- Human judgement as the ground truth


---

## 🏗️ Architecture Highlights
- Layered Spring Boot middleware architecture
- Controller, service, provider‑adapter, and configuration layers
- Asynchronous multi‑provider execution
- Modular prompt transformation pipeline


---

## 🔬 Research Foundations
This work is grounded in peer‑reviewed research from:
- **ACM Transactions on Software Engineering and Methodology (TOSEM)** – Promptware Engineering
- **ACM Transactions on Intelligent Systems and Technology (TIST)** – Evaluation of Large Language Models
- **IEEE / EMNLP / ACL** studies on prompt sensitivity and human‑centred evaluation

---

## 🔧 Prompt Transformation Pipeline (v2)

| # | Layer (id) | What it adds to the Prompt IR |
|---|---|---|
| T1 | `task-classification` | Task type from the participant's category, else inferred (factual, analytical, instruction following, comparative, coding, general) |
| T2 | `constraint-extraction` | Audience (beginner, non-technical, child, expert), exact counts, word limits, programming/response language |
| T3 | `instruction-scaffolding` | Task-specific instruction and success criteria (no generic wrapper) |
| T4 | `role-detection` | Conservative domain persona (coding → software engineer, unambiguous domain terms only); none otherwise |
| T5 | `role-conditioning` | Explicit role → detected role → (empty) default |
| T6 | `output-format` | Requested → named in the text → task default (table for comparisons, fenced code for coding) |
| T7 | `length-control` | Proportional-length guidance unless the user gave an explicit length/count (guards against length bias) |
| T8 | `context-delimitation` | User-selected context in its own delimited section |
| T9 | `response-guidelines` | State assumptions / flag uncertainty; plan before writing (comparative, analytical); self-check (instruction following) |
| S1 | `ir-normalisation` | Infrastructure: neutral defaults |
| S2 | `provider-compilation` | Infrastructure: identical wording for all providers; XML tags for Claude, Markdown sections for GPT/Gemini |
| S3 | `token-budgeting` | Infrastructure: input budget guard (trims context only, never the user's request) |

Content layers can be switched off with `PIPELINE_DISABLED_LAYERS=role-detection,length-control` (ablation). The active configuration is stored with every trial as its pipeline version (e.g. `2.0` or `2.0-without:length-control`). With all content layers disabled the compiled prompt equals the raw prompt.

## 🧪 Evaluation Platform (raw prompt vs transformed prompt)

The repository now contains a complete, deployable experiment platform:

| Part | What it does |
|---|---|
| Participant UI (`/`) | Consent → ask a question → optionally pick a **clarifying context** (given to both conditions) → see **three blinded models**, each with **Response A / Response B** (one from the raw prompt, one from the transformed prompt, random order) → vote + write a short reason per model → pick the best model overall |
| Experiment API (`/api/...`) | Runs the pipeline, executes 2 × 3 LLM calls concurrently, stores everything, resolves blinded votes |
| Researcher dashboard (`/admin.html`) | Win rate of transformed prompts per provider and task category, Wilson 95% CI, exact sign test, best-model counts, latency/failures, latest reasons, CSV export |
| Database | PostgreSQL in production, H2 locally |

### What is stored

| Table | Contents |
|---|---|
| `participants` | anonymous id, optional background and LLM familiarity, consent |
| `trials` | raw prompt, Prompt IR (JSON), all compiled/transformed prompts (JSON), pipeline version, task category, context options offered + generator + selected context, shuffled model order, usable flag, best model + reason |
| `llm_responses` | provider, model, condition (RAW / TRANSFORMED), exact system + user prompt sent, response text, latency, success/error, blinded label A/B |
| `votes` | provider, what was clicked (A/B/both good/both poor), un-blinded preference (RAW / TRANSFORMED / TIE / NEITHER), participant's reason |

### API

```
GET  /api/study                       study settings
POST /api/participants                {consent, background?, llmFamiliarity?, accessCode?}
GET  /api/participants/{id}           progress
POST /api/trials/context-options      {participantId, taskCategory, prompt}  → {contextToken, options}
POST /api/trials                      {participantId, taskCategory, prompt, contextToken?, selectedContextIndex?}  → blinded responses
POST /api/trials/{id}/evaluation      {participantId, votes:[{slot, choice, reason}], bestSlot, bestReason}
GET  /api/admin/stats                 header X-Admin-Token
GET  /api/admin/export/votes.csv      header X-Admin-Token
GET  /api/admin/export/responses.csv  header X-Admin-Token
```

The original middleware endpoints (`POST /chat`, `POST /api/chat/multi`, `POST /api/chat/gemini`) are unchanged.

### Fault tolerance

Every provider call runs independently with a timeout and at most two attempts. If a provider returns an error (e.g. HTTP 500) or times out, its pair is shown as unavailable and excluded from voting while the other providers' answers are returned normally. If every provider fails, the participant is told so and the attempt does not count towards their limit. Context-option generation falls back through the configured providers and finally to a fixed list. Simulate failures locally with `EXPERIMENT_MOCK_LLM=true EXPERIMENT_MOCK_FAIL_PROVIDERS=claude`.

### Run locally

```bash
# No API keys needed: mock answers, local H2 database in ./data
EXPERIMENT_MOCK_LLM=true ADMIN_TOKEN=dev ./mvnw spring-boot:run
# open http://localhost:8080  and  http://localhost:8080/admin.html (token: dev)

# Real models
OPENAI_KEY=... ANTHROPIC_KEY=... GEMINI_KEY=... ADMIN_TOKEN=choose-a-secret ./mvnw spring-boot:run
```

### Deploy (Render, free tier)

1. Push this branch to GitHub and merge it into `main`.
2. In Render: **New → Blueprint**, pick this repository. `render.yaml` creates the web service and a PostgreSQL database.
3. Enter `OPENAI_KEY`, `ANTHROPIC_KEY`, `GEMINI_KEY` (and optionally `EXPERIMENT_ACCESS_CODE`).
4. After the deploy, open the service URL and share it with participants. The generated `ADMIN_TOKEN` is under the service's **Environment** tab.

Notes: free web services sleep after inactivity (the first visit then takes about a minute), and Render's free PostgreSQL expires after a limited period, so download the CSV regularly or point `DATABASE_URL` at a persistent database (e.g. Neon). Any host that runs a Docker image works the same way: set `DATABASE_URL` and the keys.

### Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `OPENAI_KEY`, `ANTHROPIC_KEY`, `GEMINI_KEY` | – | provider API keys |
| `OPENAI_MODEL`, `ANTHROPIC_MODEL`, `GEMINI_MODEL` | gpt-4o, claude-opus-4-5, gemini-3.8-flash | model ids (stored with every response) |
| `DATABASE_URL` | local H2 | `postgres://user:pass@host:5432/db` |
| `ADMIN_TOKEN` | – (admin API disabled) | protects results and exports |
| `EXPERIMENT_ACCESS_CODE` | – (open) | code participants must enter |
| `EXPERIMENT_MAX_TRIALS` | 25 | questions per participant |
| `EXPERIMENT_MAX_TRIALS_PER_IP_HOUR` | 40 | abuse/cost protection |
| `EXPERIMENT_MIN_REASON_CHARS` | 3 | minimum length of a vote reason |
| `EXPERIMENT_MOCK_LLM` | false | deterministic mock answers |
| `EXPERIMENT_MOCK_FAIL_PROVIDERS` | – | mock mode: providers that fail with a simulated HTTP 500 |
| `EXPERIMENT_CONTEXT_STEP` | true | offer clarifying context options |
| `EXPERIMENT_CONTEXT_PROVIDERS` | gemini,openai,claude | order of providers used to generate context options |
| `PIPELINE_DISABLED_LAYERS` | – | content layers to switch off (ablation) |
| `LLM_MAX_ATTEMPTS` | 2 | attempts per provider call before it is marked failed |

## 📂 Project Status
Implementation and evaluation platform complete; data collection (raw vs transformed preference study) is the current phase.

---

## 🔑 Keywords
Prompt Engineering · Large Language Models · Middleware · Prompt IR · Human‑in‑the‑Loop · Software Architecture · AI Infrastructure
