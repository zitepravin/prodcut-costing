# Helping Agent — Passive Icon Behavior

## Principle

The Customer Helping AI Agent is **opt-in and passive**.

It must **not** interrupt the user’s work.

| Rule | Behavior |
|------|----------|
| Visibility | A single agent icon appears on the side (bottom-right FAB) only when the tenant has module **PRODUCT_COSTING** |
| Default state | Panel is **closed**. Chat does **not** open on login, page change, or errors |
| Intervention | **None.** No auto-open, no push notifications from the agent, no unsolicited API calls |
| User action required | User **clicks the icon** when they want help |
| After open | Drawer shows a short capability statement; **no answer is generated** until the user types a question or clicks a suggestion chip |
| After close | Returns to passive icon only |

## User flow

```
User works in the app (BOM, Should-Cost, …)
        │
        │  icon visible, idle — does not speak or open
        ▼
User clicks agent icon (optional)
        │
        ▼
Right-side “Costing Help” drawer opens
        │
        │  still silent until user asks
        ▼
User sends a question or selects a prompt chip
        │
        ▼
Read-only answer: FACT / CALCULATED / RULE_RESULT / AI_INFERENCE / MISSING_DATA
+ sources when available
```

## What the icon is **not**

- Not a chatbot that greets on every page  
- Not a workflow blocker or modal  
- Not a system alert channel  
- Not available without PRODUCT_COSTING entitlement  
- Not a writer — cannot change costs, BOMs, or approvals  

## Copy (UI)

- **Tooltip (hover):** “Costing help — optional. Click only if you want information. The agent does not open by itself.”
- **aria-label:** “Optional costing help. Opens only when activated.”
- **Drawer title:** Costing Help  
- **Subtitle:** Read-only · Opens only when you ask · Product Costing data only  
- **First message (static, not an API call):** Explains capability and limits; does not query the backend until the user asks  

## Implementation anchors

- `frontend/src/layout/AppLayout.tsx` — `helpOpen` starts `false`; FAB `onClick` sets `true`
- `frontend/src/components/helpingagent/HelpingAgentPanel.tsx` — Drawer `open={open}`; `send()` only on user action
- Backend `POST /api/v1/helping-agent/chat` — invoked only after user submit

## Test expectations

- On login with PRODUCT_COSTING: icon visible, panel **not** open  
- Without click: no chat API traffic from the agent  
- Click icon → drawer open; still no chat API until question/chip  
- Close drawer → icon only again  
