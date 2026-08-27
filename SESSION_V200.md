# SESSION_V200 — Hide inaccessible nav features

**Issue:** COSTING_ENGINEER opened Audit Log and saw restriction message.  
**Fix:** Do not show Audit Log in nav unless ADMIN or SYSTEM_ADMINISTRATOR.  
**File:** frontend/src/layout/AppLayout.tsx (`filterItem`)
