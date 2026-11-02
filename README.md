<a id="top"></a>

# 🤖 AI Bug Triage API

🇬🇧 **English** · [🇧🇷 Português](README.pt-BR.md)

A small **Spring Boot** API that reads a bug report and returns its **severity**, **category**, **suggested team**, a one-line **summary** and **labels**. An LLM does the classification; the backend treats its answer as **untrusted input**, validates it, and falls back to a deterministic rule-based classifier when anything goes wrong.

> 🧪 Learning/portfolio project. No company code, no real bug reports. Runs **without any API key** by default.

## 📑 Table of contents

- [✨ What it does](#what)
- [🚀 Quick start](#quick-start)
- [🔌 API](#api)
- [🧠 Design decisions](#design)
- [🏗️ Architecture](#architecture)
- [🧪 Tests](#tests)
- [🔧 Configuration](#configuration)
- [🗺️ Roadmap](#roadmap)
- [⚠️ Honest notes](#notes)

<a id="what"></a>

## What it does

```
POST /api/v1/triage          {"title": "...", "description": "...", "environment": "..."}
        │
        ▼
 validate input (Bean Validation) ──► TriageClassifier
                                         ├─ LLM (Anthropic) ──► validate the answer ──► result
                                         └─ rules (fallback / default) ───────────────► result
```

<sub>[⬆ back to top](#top)</sub>

<a id="quick-start"></a>

## Quick start

Requirements: **Java 21** and **Maven 3.9+**.

```bash
mvn test                 # run the tests
mvn spring-boot:run      # starts on http://localhost:8080 (rule-based mode, no key needed)
```

```bash
curl -s -X POST http://localhost:8080/api/v1/triage \
  -H "Content-Type: application/json" \
  -d '{"title":"Users cannot login after deploy","description":"Everyone gets rejected","environment":"production"}'
```

```json
{
  "severity": "CRITICAL",
  "category": "FUNCTIONAL",
  "suggestedTeam": "backend",
  "summary": "Users cannot login after deploy",
  "labels": ["functional", "severity-critical"],
  "source": "rules"
}
```

To use the LLM instead:

```bash
export ANTHROPIC_API_KEY="your-key"     # never commit it
export TRIAGE_PROVIDER=anthropic
mvn spring-boot:run
```

`"source"` tells you who produced the answer: `"llm"` or `"rules"`.

<sub>[⬆ back to top](#top)</sub>

<a id="api"></a>

## API

### `POST /api/v1/triage`

| Field | Rules |
|---|---|
| `title` | required, up to 200 characters |
| `description` | required, up to 5,000 characters |
| `environment` | optional, up to 200 characters |

| Status | When |
|---|---|
| `200` | classification returned (even if the LLM failed: the fallback answers) |
| `400` | invalid input, returned as an RFC 7807 problem detail |

Values: **severity** `CRITICAL · HIGH · MEDIUM · LOW` · **category** `SECURITY · DATA · PERFORMANCE · INTEGRATION · UI_UX · FUNCTIONAL · OTHER`.

<sub>[⬆ back to top](#top)</sub>

<a id="design"></a>

## Design decisions

| Decision | Why |
|---|---|
| **LLM output is untrusted** | The answer is parsed strictly: severity and category must be valid enum values, the summary is required and truncated, labels are capped. Anything else triggers the fallback. |
| **Deterministic fallback** | If the model is down, slow, or returns nonsense, the API still answers. Rule-based mode is also the default, so the project runs and is testable with no key. |
| **The team is derived, not generated** | `suggestedTeam` comes from the category in code (`Category.team()`), so the model can't invent teams. |
| **Prompt-injection hardening** | The report goes inside `<bug_report>` tags, the system prompt says to treat it as data, and the tags are stripped from the input so a report can't close the block early. This reduces the risk; it doesn't remove it, which is why the output is validated too. |
| **Port and adapter for the LLM** | `LlmClient` is a one-method interface. Tests use lambdas; production uses `AnthropicLlmClient` (JDK `HttpClient`, no extra SDK). Swapping providers means one new class. |
| **Secrets and logs** | The key comes from the environment. Error logs contain the exception type only, never the report text or the key. |
| **Input limits** | Size limits on every field cap cost and abuse. |

<sub>[⬆ back to top](#top)</sub>

<a id="architecture"></a>

## Architecture

```
src/main/java/dev/ketleen/triage/
├── api/          TriageController, TriageConfig (picks the provider)
├── classifier/   TriageClassifier · RuleBasedClassifier · LlmTriageClassifier
├── llm/          LlmClient · AnthropicLlmClient · LlmException
└── model/        TriageRequest · TriageResult · Severity · Category
```

<sub>[⬆ back to top](#top)</sub>

<a id="tests"></a>

## Tests

| Test | What it checks |
|---|---|
| `RuleBasedClassifierTest` | severity/category/team rules, security never below HIGH, summary truncation |
| `LlmTriageClassifierTest` | valid answer, JSON inside markdown fences, non-JSON, invalid enum, missing field, LLM failure, injected closing tag, summary truncation (all with a fake `LlmClient`, no network) |
| `TriageControllerTest` | HTTP contract and validation (`400`) with the classifier mocked |
| `TriageApiIntegrationTest` | full application context, end to end, without any key |

<sub>[⬆ back to top](#top)</sub>

<a id="configuration"></a>

## Configuration

| Variable | Default | Meaning |
|---|---|---|
| `TRIAGE_PROVIDER` | `rules` | `rules` or `anthropic` |
| `ANTHROPIC_API_KEY` | _(empty)_ | required when the provider is `anthropic` |
| `ANTHROPIC_MODEL` | `claude-sonnet-5-5` | model id sent to the API |
| `ANTHROPIC_BASE_URL` | `https://api.anthropic.com` | useful for proxies and tests |

Docker: `docker build -t ai-bug-triage-api . && docker run -p 8080:8080 ai-bug-triage-api`

<sub>[⬆ back to top](#top)</sub>

<a id="roadmap"></a>

## Roadmap

- Persist triage results (PostgreSQL) and add a `GET` to list them
- Detect likely duplicate reports
- Per-client rate limiting
- Metrics: LLM latency, fallback rate

<a id="notes"></a>

## Honest notes

- Rule-based mode is a keyword heuristic: good as a safety net, not as a substitute for the LLM.
- Severity from an LLM is a suggestion for a human to confirm, not a decision.
- Don't send confidential bug reports to an external API without checking your company's policy.

---

Made by [Ketleen](https://github.com/KetleenAnne) · ☕ + 🐱
