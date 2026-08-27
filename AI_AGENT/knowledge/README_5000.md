# Product Costing Customer Help Agent — 5000 Q&A Pack

Baseline: PRODUCT-COSTING-SOFTWARE-MASTER-V137

Files:
- product_costing_help_5000_qa.json — master structured JSON dataset
- product_costing_help_5000_qa.jsonl — one training/evaluation example per line
- chunks/ — ten 500-example JSONL files for token/context-limited agents
- dataset_index.json — module and intent distribution

Design rules:
- Customer-facing, not a coding assistant.
- Product Costing is the primary scope.
- Existing backend/service results remain the financial source of truth.
- The agent is read-oriented and may explain/compare/summarize/recommend, but must not autonomously write business records.
- Authorization must use authentication + tenant/customer context + Central Access + RBAC.
- Never infer missing financial values; classify missing data explicitly.
- Never expose another tenant/customer's data.
- Never treat AI inference as fact.

Suggested use:
1. Use the JSONL chunks for retrieval/training jobs with context limits.
2. Use the master JSON for evaluation/inspection.
3. Filter by category or intent for focused tests.
4. Treat the source_alignment fields as implementation hints; the agent must still verify actual V137 service/API behavior at runtime.
