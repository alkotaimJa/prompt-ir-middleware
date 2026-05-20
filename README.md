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

## 📂 Project Status
Future work includes extending the evaluation dataset and analysing qualitative preference trends across task categories.

---

## 🔑 Keywords
Prompt Engineering · Large Language Models · Middleware · Prompt IR · Human‑in‑the‑Loop · Software Architecture · AI Infrastructure
