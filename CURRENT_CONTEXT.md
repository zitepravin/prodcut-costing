# CURRENT_CONTEXT — V292 demo data

**Module:** Multi-tenant demo seed (V167 + V168)  
**Recent changes:** Schema-aligned V167 fixes; V168 volume for materials/programs/BOM roots/should-cost/suppliers  
**Decisions:** Products modeled as `bom_node` roots; materials use `record_id`/`code`/`customer_id`; tooling uses `tooling_record`  
**Active assumptions:** DEFAULT customer exists from earlier migrations; password hash shared for all demo users  
**Blockers:** No live DB in packaging environment  
**Files modified:**  
- `backend/src/main/resources/db/migration/V167__complete_demo_company_data.sql`  
- `backend/src/main/resources/db/migration/V168__demo_volume_extension.sql`  
- `CHANGELOG.md`, `PROJECT_STATUS.md`, `CURRENT_CONTEXT.md`, `NEXT_AGENT.md`  
**Next step:** Flyway migrate + count validation queries by tenant
