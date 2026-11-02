<a id="topo"></a>

# 🤖 AI Bug Triage API

[🇬🇧 English](README.md) · 🇧🇷 **Português**

Uma API **Spring Boot** pequena que lê um relato de bug e devolve a **severidade**, a **categoria**, o **time sugerido**, um **resumo** de uma linha e **labels**. Um LLM faz a classificação; o backend trata a resposta dele como **entrada não confiável**, valida e, se algo der errado, usa um classificador determinístico baseado em regras.

> 🧪 Projeto de estudo/portfólio. Nenhum código de empresa, nenhum relato real. Roda **sem chave de API** por padrão.

## 📑 Sumário

- [✨ O que faz](#o-que-faz)
- [🚀 Início rápido](#inicio-rapido)
- [🔌 API](#api)
- [🧠 Decisões de design](#design)
- [🏗️ Arquitetura](#arquitetura)
- [🧪 Testes](#testes)
- [🔧 Configuração](#configuracao)
- [🗺️ Próximos passos](#roadmap)
- [⚠️ Notas honestas](#notas)

<a id="o-que-faz"></a>

## O que faz

```
POST /api/v1/triage          {"title": "...", "description": "...", "environment": "..."}
        │
        ▼
 valida a entrada (Bean Validation) ──► TriageClassifier
                                          ├─ LLM (Anthropic) ──► valida a resposta ──► resultado
                                          └─ regras (fallback / padrão) ─────────────► resultado
```

<sub>[⬆ voltar ao topo](#topo)</sub>

<a id="inicio-rapido"></a>

## Início rápido

Requisitos: **Java 21** e **Maven 3.9+**.

```bash
mvn test                 # roda os testes
mvn spring-boot:run      # sobe em http://localhost:8080 (modo regras, sem chave)
```

```bash
curl -s -X POST http://localhost:8080/api/v1/triage \
  -H "Content-Type: application/json" \
  -d '{"title":"Usuarios nao conseguem login apos deploy","description":"Todos sao rejeitados","environment":"production"}'
```

Para usar o LLM:

```bash
export ANTHROPIC_API_KEY="sua-chave"    # nunca faça commit dela
export TRIAGE_PROVIDER=anthropic
mvn spring-boot:run
```

O campo `"source"` mostra quem respondeu: `"llm"` ou `"rules"`.

<sub>[⬆ voltar ao topo](#topo)</sub>

<a id="api"></a>

## API

### `POST /api/v1/triage`

| Campo | Regras |
|---|---|
| `title` | obrigatório, até 200 caracteres |
| `description` | obrigatório, até 5.000 caracteres |
| `environment` | opcional, até 200 caracteres |

| Status | Quando |
|---|---|
| `200` | classificação devolvida (mesmo se o LLM falhou: o fallback responde) |
| `400` | entrada inválida, no formato RFC 7807 |

Valores: **severity** `CRITICAL · HIGH · MEDIUM · LOW` · **category** `SECURITY · DATA · PERFORMANCE · INTEGRATION · UI_UX · FUNCTIONAL · OTHER`.

<sub>[⬆ voltar ao topo](#topo)</sub>

<a id="design"></a>

## Decisões de design

| Decisão | Por quê |
|---|---|
| **A saída do LLM não é confiável** | A resposta é lida de forma estrita: severidade e categoria precisam ser valores válidos do enum, o resumo é obrigatório e truncado, as labels têm limite. Qualquer outra coisa aciona o fallback. |
| **Fallback determinístico** | Se o modelo cair, demorar ou responder bobagem, a API continua respondendo. O modo por regras também é o padrão, então o projeto roda e é testável sem chave. |
| **O time é derivado, não gerado** | `suggestedTeam` vem da categoria no código (`Category.team()`), então o modelo não inventa times. |
| **Proteção contra prompt injection** | O relato vai dentro de tags `<bug_report>`, o prompt de sistema diz para tratá-lo como dado, e as tags são removidas da entrada para o relato não fechar o bloco antes da hora. Isso reduz o risco, não elimina; por isso a saída também é validada. |
| **Porta e adaptador para o LLM** | `LlmClient` é uma interface de um método. Os testes usam lambdas; em produção, `AnthropicLlmClient` (`HttpClient` do JDK, sem SDK extra). Trocar de provedor é uma classe nova. |
| **Segredos e logs** | A chave vem do ambiente. Os logs de erro trazem só o tipo da exceção, nunca o texto do relato nem a chave. |
| **Limites de entrada** | Limites de tamanho em todos os campos controlam custo e abuso. |

<sub>[⬆ voltar ao topo](#topo)</sub>

<a id="arquitetura"></a>

## Arquitetura

```
src/main/java/dev/ketleen/triage/
├── api/          TriageController, TriageConfig (escolhe o provedor)
├── classifier/   TriageClassifier · RuleBasedClassifier · LlmTriageClassifier
├── llm/          LlmClient · AnthropicLlmClient · LlmException
└── model/        TriageRequest · TriageResult · Severity · Category
```

<sub>[⬆ voltar ao topo](#topo)</sub>

<a id="testes"></a>

## Testes

| Teste | O que verifica |
|---|---|
| `RuleBasedClassifierTest` | regras de severidade/categoria/time, segurança nunca abaixo de HIGH, truncamento do resumo |
| `LlmTriageClassifierTest` | resposta válida, JSON dentro de cercas markdown, texto sem JSON, enum inválido, campo ausente, falha do LLM, tag de fechamento injetada, truncamento (tudo com um `LlmClient` falso, sem rede) |
| `TriageControllerTest` | contrato HTTP e validação (`400`) com o classificador simulado |
| `TriageApiIntegrationTest` | contexto completo da aplicação, de ponta a ponta, sem chave |

<sub>[⬆ voltar ao topo](#topo)</sub>

<a id="configuracao"></a>

## Configuração

| Variável | Padrão | Significado |
|---|---|---|
| `TRIAGE_PROVIDER` | `rules` | `rules` ou `anthropic` |
| `ANTHROPIC_API_KEY` | _(vazio)_ | obrigatória quando o provedor é `anthropic` |
| `ANTHROPIC_MODEL` | `claude-sonnet-5-5` | id do modelo enviado à API |
| `ANTHROPIC_BASE_URL` | `https://api.anthropic.com` | útil para proxies e testes |

Docker: `docker build -t ai-bug-triage-api . && docker run -p 8080:8080 ai-bug-triage-api`

<sub>[⬆ voltar ao topo](#topo)</sub>

<a id="roadmap"></a>

## Próximos passos

- Persistir as triagens (PostgreSQL) e criar um `GET` para listar
- Detectar relatos prováveis duplicados
- Limite de requisições por cliente
- Métricas: latência do LLM e taxa de fallback

<a id="notas"></a>

## Notas honestas

- O modo por regras é uma heurística de palavras-chave: serve de rede de segurança, não substitui o LLM.
- A severidade vinda de um LLM é uma sugestão para um humano confirmar, não uma decisão.
- Não envie relatos confidenciais a uma API externa sem checar a política da sua empresa.

---

Feito por [Ketleen](https://github.com/KetleenAnne) · ☕ + 🐱
