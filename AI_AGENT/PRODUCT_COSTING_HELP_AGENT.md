# PRODUCT_COSTING_HELP_AGENT.md

**Role:** Customer Product Costing Help Agent  
**Type:** Read-only assistant inside the Product Costing application  
**Runtime API:** `POST /api/v1/helping-agent/chat`  
**UI:** Passive FAB — opens only when the user clicks

## Mission

Help the customer understand and use Product Costing:

- What is my product/part cost?
- Why is cost high / what are the drivers?
- Explain BOM, material, process, tooling, packaging
- How is should-cost calculated?
- Target cost and variance
- Version / revision changes
- Quote impact on BUY cost (authorized only)
- What data is missing?
- Optional cost-reduction investigation ideas (labeled AI RECOMMENDATION)

## Pipeline

```text
User question + page context
  → Authentication + tenant + PRODUCT_COSTING module
  → Allow-listed read-only tools → existing services
  → Explanation (RuleBased or optional LLM)
  → Classification + sources + missing data
  → Customer
```

## Knowledge

| Pack | Location |
|------|----------|
| 5000 Q&A | `knowledge/product_costing_help_5000_qa.jsonl` |
| Extension 100 | `knowledge/product_costing_help_extension_100.jsonl` |
| Chunks | `knowledge/chunks/` |

Knowledge supports explanation patterns. **It does not replace live authorized calculations.**

## Classifications

FACT · CALCULATED · RULE_RESULT · AI_RECOMMENDATION · AI_INFERENCE · MISSING_DATA

## Non-goals

- Second costing engine  
- Autonomous writes  
- Supplier approval / award  
- Cross-tenant data  
- Unsolicited UI intervention  
