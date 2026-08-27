# PRODUCT COSTING & SUPPLIER INTELLIGENCE PLATFORM

## Master AI Development Specification

**Document Type:** Master AI Development Instruction\
**System:** Enterprise Product Costing + Supplier Selection + Supplier Qualification + Supplier Approval\
**Industry:** Industry-neutral (automotive is one profile; manufacturing, process, project, service, logistics, and others via configuration)\
**Development Standard:** Production-Ready Enterprise Software\
**Primary Role:** Senior AI Product Architect + Software Architect + Manufacturing Costing Analyst + Supplier Evaluation Specialist + Full-Stack Development Agent

---

# 1. AI AGENT ROLE

The AI agent is responsible for:

- Product architecture

- Software development

- Product costing

- Should-cost analysis

- Supplier discovery

- Supplier qualification

- Supplier evaluation

- Supplier comparison

- Supplier risk analysis

- Supplier recommendation

- API development

- Database development

- Frontend development

- Backend development

- Testing

- QA support

- Documentation

- Technical analysis

- Data validation

- Continuous project handover

The AI must behave like a **senior enterprise engineering team**, not like a code-generation assistant.

---



---

# SKILL — UNIVERSAL COSTING (V278+)

**Skill name:** `universal-costing`  
**Location:** `AI_AGENT/skills/universal-costing/SKILL.md` and `~/.grok/skills/universal-costing/`  
**Source framework:** `V278_Industry_Neutral_Universal_Costing_Framework_Report.docx`

When working on costing architecture, methods, drivers, ABC, cost objects, industry profiles, or multi-industry rollout:

1. Load the **universal-costing** skill.
2. Treat industry differences as **configuration profiles**, not code branches.
3. Prefer extending `com.automotive.costing.universal` and existing engines over new industry silos.
4. Preserve tenant isolation, feature entitlement, CONFIG_* RBAC, and freeze/snapshot rules.
5. Classify work as ALREADY EXISTS / ENHANCE / NEW / FUTURE before coding.

Core model: **Cost Object → Method → Driver → Source → Component → Allocation → Version → Calculation → Freeze → Result → Variance**


# 2. PRIMARY BUSINESS OBJECTIVE

The platform must answer two fundamental business questions:

## Question A — Product Cost

> What should this product or part cost?

## Question B — Supplier Selection

> Which supplier is the most suitable supplier for this part?

The system must never assume:

> Lowest Price = Best Supplier

Supplier selection must consider:

- Cost

- Should Cost

- TCO

- Quality

- Delivery

- Capacity

- Technical capability

- Warranty

- Guarantee

- After-sales service

- Supplier history

- Compliance

- Risk

- Business continuity

- Supply-chain reliability

---

# 3. SOURCE-OF-TRUTH HIERARCHY

Before modifying or creating anything, inspect the available project documentation.

Priority order:

 1. `SRS`

 2. `TDD`

 3. `TEST_PLAN`

 4. `QA_TEST_CASE_TRACKER`

 5. `PROJECT_STATUS.md`

 6. `CURRENT_CONTEXT.md`

 7. `API_SPEC.md`

 8. `DATABASE_SCHEMA.md`

 9. `CHANGELOG.md`

10. `NEXT_AGENT.md`

11. Existing source code

## Conflict Rule

If two requirements conflict:

**Higher-priority documentation wins.**

Never silently resolve a business-rule conflict.

## Missing Requirement Rule

If a required business rule is missing:

1. Identify the gap.

2. Explain why the gap matters.

3. Propose an option.

4. Clearly mark it as:

`PROPOSED REQUIREMENT`

5. Do not implement the proposed rule as confirmed business logic without authorization.

---

# 4. DEVELOPMENT LIFECYCLE

Every development task follows this sequence:

```text
READ REQUIREMENTS
      ↓
UNDERSTAND CURRENT SYSTEM
      ↓
IDENTIFY IMPACT
      ↓
DESIGN SOLUTION
      ↓
DATABASE DESIGN
      ↓
API DESIGN
      ↓
BACKEND IMPLEMENTATION
      ↓
FRONTEND IMPLEMENTATION
      ↓
BUSINESS VALIDATION
      ↓
UNIT TESTING
      ↓
NEGATIVE TESTING
      ↓
INTEGRATION VALIDATION
      ↓
DOCUMENTATION
      ↓
PROJECT STATUS UPDATE
      ↓
HANDOVER
```

Never skip architectural impact analysis.

---

# 5. TECHNOLOGY STACK

## Frontend

- React

- TypeScript

- Material UI

## Backend

- Java 21

- Spring Boot

- Spring Security

- Spring Data JPA

- Maven

## Database

- MySQL

## Authentication

- JWT

## API Documentation

- OpenAPI

- Swagger

## Testing

- JUnit

- Mockito

## Financial Calculation

- `BigDecimal`

Never use:

- `float`

- `double`

for monetary calculations.

---

# 6. ARCHITECTURE PRINCIPLES

Follow:

- Clean Architecture

- SOLID

- Separation of concerns

- REST principles

- Domain-driven business logic

- Modular design

- Secure-by-default design

- API-first compatibility

- Testable components

- Maintainable code

## Layer Responsibilities

### Controller

Responsible only for:

- HTTP request

- Request validation

- Authentication context

- Calling service

- HTTP response

Controllers must not contain business calculations.

### Service

Responsible for:

- Business logic

- Calculations

- Validation requiring business rules

- Workflow

- Scoring

- Recommendation preparation

- Transaction orchestration

### Repository

Responsible only for:

- Database access

- Query operations

- Persistence

### DTO

Responsible for:

- API request contracts

- API response contracts

Entities must never be exposed directly through APIs.

### Entity

Responsible for:

- Persistence mapping

- Database relationships

- Persistence state

---

# 7. PRODUCT COSTING DOMAIN

The costing engine must support, where defined by the approved requirements:

## Product Structure

- Product

- Part

- Part revision

- BOM

- BOM revision

- BOM quantity

- Unit of measure

- Annual quantity

- Production volume

## Material

- Raw material

- Material grade

- Material specification

- Material price

- Material consumption

- Yield

- Scrap

- Scrap recovery

## Manufacturing Process

- Process

- Operation

- Machine

- Machine rate

- Cycle time

- Setup time

- Changeover

- Labor

- Direct labor

- Indirect labor

- Energy

- Utilities

- Manufacturing overhead

## Tooling

- Tool

- Die

- Fixture

- Gauge

- Tooling cost

- Tool life

- Tooling amortization

## Additional Cost

- Inspection

- Quality cost

- Rejection

- Rework

- Packaging

- Transportation

- Logistics

- Warranty

- Other approved costs

## Commercial

- Supplier quotation

- Currency

- Exchange rate

- MOQ

- Lead time

- Volume

- Margin

- Profit

- Target cost

---

# 8. COSTING MODEL

The platform should maintain traceability between:

```text
Quoted Cost
     ↓
Calculated Cost
     ↓
Should Cost
     ↓
Target Cost
     ↓
TCO
```

Every calculation must be traceable to its source data.

The system must allow the user to identify:

- Cost component

- Input value

- Unit

- Quantity

- Rate

- Calculation formula

- Result

- Source

- Revision

- Timestamp

- Responsible user

---

# 9. SHOULD-COST ENGINE

Should-cost analysis must use a bottom-up approach where sufficient data exists.

Conceptually:

```text
Material
+ Process
+ Labor
+ Machine
+ Energy
+ Tooling
+ Overhead
+ Packaging
+ Logistics
+ Other Approved Costs
=
Should Cost
```

The system must compare:

```text
Supplier Quote
        VS
Should Cost
```

Example:

```text
Supplier Quote = ₹120
Should Cost    = ₹100
Variance       = ₹20
```

The system must explain the variance using available data.

Example:

> Quote is approximately ₹20 above the current should-cost estimate. The identified variance is primarily associated with material and process assumptions.

Never invent a reason when the underlying data is unavailable.

---

# 10. TOTAL COST OF OWNERSHIP

Supplier evaluation must not rely solely on purchase price.

Where applicable:

```text
Purchase Price
+ Freight
+ Logistics
+ Packaging
+ Inventory Cost
+ Quality/Rejection Cost
+ Rework Cost
+ Warranty Cost
+ Service Cost
+ Tooling Cost
+ Other Approved Lifecycle Costs
=
TCO
```

If data is unavailable:

`DATA NOT AVAILABLE`

must be displayed.

Do not silently convert missing information into zero.

---

# 11. SUPPLIER LIFECYCLE

Supplier lifecycle should support the approved project workflow.

Potential lifecycle:

```text
DISCOVERED
     ↓
IMPORTED
     ↓
UNDER_VERIFICATION
     ↓
QUALIFIED
     ↓
EVALUATED
     ↓
RECOMMENDED
     ↓
APPROVAL_PENDING
     ↓
APPROVED
```

Negative states may include:

```text
REJECTED
BLOCKED
SUSPENDED
```

Exact states must follow the approved requirements.

Supplier discovery must never automatically create an approved supplier.

---

# 12. SUPPLIER EVALUATION ENGINE

Evaluation must support configurable criteria.

Potential categories:

 1. Cost

 2. Quality

 3. Delivery

 4. Technical Capability

 5. Capacity

 6. Warranty

 7. Guarantee

 8. After-Sales Service

 9. Supplier History

10. Policy / Compliance

11. Risk

12. Business Continuity

13. Supply Chain

14. Certifications

Weights must be configurable where permitted by the approved architecture.

Never hard-code an arbitrary universal weighting model.

---

# 13. SUPPLIER QUALITY ENGINE

Where reliable data exists, evaluate:

- PPM

- Rejection rate

- NCR

- Customer complaints

- CAPA

- 8D

- Audit score

- Process capability

- First-pass yield

- Defect history

- Quality certification

- Certification validity

- Certification expiry

- Quality trend

Historical performance must be distinguished from current performance.

---

# 14. DELIVERY EVALUATION

Evaluate:

- OTD

- OTIF

- Lead time

- Delivery reliability

- Late delivery count

- Average delay

- Maximum delay

- Emergency response

- Capacity response

- Schedule flexibility

Historical delivery data should take priority over unsupported supplier claims.

---

# 15. WARRANTY EVALUATION

Evaluate, where applicable:

- Warranty duration

- Warranty coverage

- Warranty exclusions

- Claim process

- Claim response time

- Replacement time

- Warranty claim rate

- Historical warranty cost

- Claim acceptance/rejection

- Resolution time

Never assume warranty terms.

---

# 16. GUARANTEE EVALUATION

Support:

- Quality guarantee

- Delivery guarantee

- Capacity guarantee

- Performance guarantee

- Tooling guarantee

- Price guarantee

Record:

- Guarantee period

- Conditions

- Failure consequences

- Penalties

- Historical failures

---

# 17. AFTER-SALES SERVICE

Treat service as a separate evaluation category.

Evaluate:

- Service availability

- Response time

- Technical support

- Emergency support

- Field support

- Replacement support

- Spare-parts availability

- Escalation process

- Service SLA

- Historical performance

- Complaint closure time

- Local support

Warranty and after-sales service must not automatically be treated as the same category.

---

# 18. POLICY & COMPLIANCE

Potential evaluation areas:

- Quality policy

- Environmental policy

- Safety policy

- Business continuity

- Ethical sourcing

- Anti-bribery

- Information security

- Responsible sourcing

- Conflict of interest

- Subcontractor controls

A supplier declaration must not automatically be considered verified evidence when verification is required.

---

# 19. SUPPLIER RISK ENGINE

Risk must be independently visible.

Evaluate:

### Financial

- Financial stability

- Financial dependency

### Quality

- Quality failures

- Certification risk

### Delivery

- Delivery reliability

- Lead-time risk

### Capacity

- Available capacity

- Capacity dependency

### Geographic

- Location risk

- Regional disruption

### Supply Chain

- Raw-material dependency

- Subcontractor dependency

- Single-source risk

### Business Continuity

- Disaster recovery

- Plant shutdown

- Business continuity plan

Risk must not disappear inside the overall score.

---

# 20. QUESTIONNAIRE ENGINE

Supplier questionnaires must be configurable.

Never hard-code supplier questions into business logic.

Each question may contain:

- Question ID

- Category

- Question text

- Answer type

- Required

- Evidence required

- Scoring rule

- Weight

- Mandatory gate

- Supplier applicability

- Version

- Effective date

- Expiry

- Active/inactive

Supported answer types may include:

- Yes/No

- Number

- Percentage

- Currency

- Date

- Text

- Single selection

- Multiple selection

- Document

- Rating

---

# 21. QUESTION VERSIONING

Question changes must not alter historical evaluations.

When a question changes:

```text
Question Version 1
        ↓
Question Version 2
        ↓
Question Version 3
```

Historical evaluation records must remain linked to the version that was active when the evaluation occurred.

---

# 22. EVIDENCE MANAGEMENT

Supplier answers and evidence are separate concepts.

Evidence status:

```text
PENDING
VERIFIED
FAILED
EXPIRED
NOT_REQUIRED
WAIVED
```

Where required, evidence should maintain:

- Document/reference

- Upload date

- Expiry date

- Verification status

- Verified by

- Verification date

Unverified information must never be represented as verified information.

---

# 23. SUPPLIER SCORING ENGINE

The scoring engine must support:

- Category score

- Question score

- Weight

- Normalization

- Mandatory gates

- Eligibility

- Overall score

- Score explanation

Example weighting:

```text
Quality       30%
Cost          20%
Delivery      15%
Service       10%
Warranty      10%
Capability     5%
History        5%
Risk           5%
```

This is an example only.

Do not implement these values unless they are approved by project requirements.

---

# 24. MANDATORY ELIGIBILITY GATES

Numerical score does not override mandatory requirements.

Potential gates:

- Required certification

- Technical capability

- Required capacity

- Critical evidence

- Supplier blocked status

- Quality threshold

- Compliance condition

Evaluation flow:

```text
Supplier Data
      ↓
Mandatory Gates
      ↓
Eligibility
      ↓
Scoring
      ↓
Risk Assessment
      ↓
Recommendation
```

A supplier with a high numerical score can still be:

`NOT ELIGIBLE`

---

# 25. SUPPLIER COMPARISON

The system must provide side-by-side comparison.

Compare:

- Quote

- Should Cost

- TCO

- Quality

- Delivery

- Service

- Warranty

- Guarantee

- Capacity

- Technical capability

- History

- Risk

- Compliance

- Overall score

- Eligibility

The UI must make trade-offs visible rather than displaying only one ranking number.

---

# 26. AI RECOMMENDATION ENGINE

The AI may:

- Analyze supplier data

- Explain scores

- Identify risks

- Identify cost variance

- Highlight missing evidence

- Compare suppliers

- Generate recommendation drafts

- Explain ranking

- Identify negotiation opportunities

- Identify data-quality problems

- Suggest additional questions

The AI must never:

- Approve suppliers automatically

- Award business automatically

- Override mandatory gates

- Invent supplier data

- Invent certification status

- Invent warranty terms

- Invent quality history

- Invent cost data

- Hide negative evidence

- Change approved scoring rules without authorization

---

# 27. AI DECISION BOUNDARY

The AI role is:

```text
DATA
 ↓
ANALYSIS
 ↓
CALCULATION
 ↓
RISK IDENTIFICATION
 ↓
RECOMMENDATION
 ↓
EXPLANATION
 ↓
HUMAN DECISION
```

Never:

```text
AI
 ↓
AUTOMATIC APPROVAL
```

Human authorization remains mandatory.

---

# 28. AI EXPLAINABILITY

Every recommendation must explain:

### DATA USED

What information was considered?

### SCORE

What scores were calculated?

### WEIGHT

Which approved weights were applied?

### ELIGIBILITY

Did the supplier pass mandatory gates?

### STRENGTHS

What are the supplier's major advantages?

### WEAKNESSES

What are the major disadvantages?

### RISK

What risks were identified?

### MISSING DATA

What information is unavailable?

### COST VARIANCE

How does quote compare with should-cost?

### TCO IMPACT

How does lifecycle cost affect the decision?

### EVIDENCE

Which evidence supports the conclusion?

---

# 29. AI INFORMATION CLASSIFICATION

AI output must distinguish:

```text
FACT
CALCULATED RESULT
AI INFERENCE
MISSING DATA
```

Example:

**FACT**

Supplier OTD is 96%.

**CALCULATED RESULT**

Supplier TCO is ₹X per unit.

**AI INFERENCE**

Supplier B appears to have lower delivery risk based on available historical data.

**MISSING DATA**

Warranty claim history is unavailable.

Never present inference as fact.

---

# 30. AI CONFIDENCE

Where technically appropriate, provide:

### HIGH CONFIDENCE

- Verified historical data

- Verified documents

- Complete costing data

### MEDIUM CONFIDENCE

- Partially complete information

- Some unverified data

### LOW CONFIDENCE

- Significant missing data

- Unverified supplier claims

- Insufficient historical information

Low confidence must produce a visible warning.

---

# 31. HUMAN APPROVAL

Human approval is mandatory for:

- Supplier approval

- Significant supplier rejection

- Mandatory-gate exceptions

- Final recommendation

- Major cost-model changes

- Scoring-weight changes

- Approval overrides

- Critical risk acceptance

The AI prepares evidence.

The authorized human makes the final decision.

---

# 32. MULTI-TENANT SECURITY

The platform must support tenant isolation.

Tenant A must never access:

- Tenant B suppliers

- Tenant B products

- Tenant B costing

- Tenant B evaluations

- Tenant B documents

- Tenant B quotations

Security must be enforced at the backend.

Frontend visibility is not a security mechanism.

---

# 33. CENTRAL ACCESS MANAGEMENT

Access management must be modular.

Example:

### Customer A

```text
Product Costing = YES
Vendor Selection = YES
Purchase        = NO
```

### Customer B

```text
Product Costing = YES
Vendor Selection = NO
Purchase        = YES
```

Future modules may include:

- Purchase

- Purchase Orders

- Order Tracking

- Supplier Performance

- Inventory

- Other approved modules

The access-control architecture must support additional modules without requiring a complete redesign.

---

# 34. API GOVERNANCE

Whenever an API changes, update:

- API specification

- OpenAPI/Swagger

- DTO

- Controller

- Service

- Validation

- Frontend integration

- Tests

- Documentation

API changes must remain backward compatible unless an approved breaking change exists.

---

# 35. DATABASE GOVERNANCE

Whenever database schema changes:

Update:

- Database schema documentation

- Entity

- Migration

- Repository

- Related service

- API

- Tests

Never make undocumented breaking schema changes.

---

# 36. VALIDATION & ERROR HANDLING

Every API must implement appropriate:

- Input validation

- Business validation

- Authorization validation

- Tenant validation

- Global exception handling

- Structured error responses

- Logging

Errors must not expose sensitive internal information.

---

# 37. TESTING STANDARD

Every feature must contain:

## Unit Tests

Test individual services and business logic.

## Validation Tests

Test valid and invalid inputs.

## Negative Tests

Test:

- Missing data

- Invalid values

- Unauthorized access

- Wrong tenant

- Invalid state

- Failed mandatory gate

- Missing evidence

- Expired evidence

- Invalid costing input

## Calculation Tests

Verify:

- Material cost

- Process cost

- Labor

- Machine cost

- Tooling

- Overhead

- Should cost

- TCO

- Variance

- Scoring

Financial calculations must have precise expected results.

---

# 38. DOCUMENTATION STANDARD

Every feature must document:

 1. Business purpose

 2. Functional behavior

 3. Technical design

 4. Database impact

 5. API endpoints

 6. Validation rules

 7. Security rules

 8. Error handling

 9. Dependencies

10. Testing

11. Known limitations

12. Future considerations

---

# 39. CODE QUALITY RULES

Use:

- Meaningful class names

- Meaningful method names

- Meaningful variable names

- Appropriate JavaDoc

- Appropriate logging

- Small maintainable methods

- Reusable services

- Strong typing

- DTO-based APIs

Do not generate:

- TODO

- FIXME

- Fake implementation

- Placeholder implementation

- Dummy business logic

- Hard-coded business data

- Fake supplier information

- Fake scores

- Fake certification information

---

# 40. FINANCIAL PRECISION

All financial calculations must use:

```java
BigDecimal
```

Never:

```java
float
double
```

Money calculations must define:

- Precision

- Scale

- Rounding policy

- Currency

- Exchange rate handling

Rounding must occur only according to the approved business rule.

---

# 41. AUDITABILITY

Important business operations should maintain traceability.

Where required, capture:

- User

- Tenant

- Timestamp

- Action

- Previous value

- New value

- Reason

- Approval

- Rejection

- Override

- Evidence

- Calculation version

Costing and supplier recommendations must be explainable after the fact.

---

# 42. VERSION CONTROL

Version important business configurations:

- Cost models

- BOM

- Product revision

- Supplier evaluation

- Questionnaires

- Question versions

- Scoring models

- Weight configurations

- Supplier evidence

- Recommendation results

Historical results must remain reproducible.

---

# 43. CHANGE MANAGEMENT

Every completed feature must record:

```text
Feature
Files Modified
Business Reason
Technical Reason
Database Impact
API Impact
Frontend Impact
Testing
Risk
Dependencies
Known Issues
Next Task
```

---

# 44. MULTI-AGENT DEVELOPMENT

Assume multiple AI agents may work simultaneously.

Rules:

1. Never overwrite completed work.

2. Inspect existing implementation before editing.

3. Do not rewrite unrelated modules.

4. Do not silently modify another module's business rules.

5. Maintain API compatibility.

6. Maintain database compatibility.

7. Document cross-module changes.

8. Update project status after every meaningful change.

---

# 45. PROJECT CONTINUITY FILES

The following files are mandatory:

```text
PROJECT_STATUS.md
CURRENT_CONTEXT.md
NEXT_AGENT.md
CHANGELOG.md
API_SPEC.md
DATABASE_SCHEMA.md
```

---

# 46. PROJECT_STATUS.md

Must contain:

- Current project phase

- Completed modules

- In-progress modules

- Pending modules

- Current implementation status

- Known issues

- Test status

- Deployment status

- Immediate next task

---

# 47. CURRENT_CONTEXT.md

Must contain enough technical context for another AI agent to continue immediately.

Include:

- Current architecture

- Current module

- Recent changes

- Important decisions

- Active assumptions

- Dependencies

- Current blockers

- Files being modified

- Next implementation step

---

# 48. NEXT_AGENT.md

Must provide direct handover instructions.

Include:

```text
CURRENT STATE
COMPLETED
IN PROGRESS
DO NOT CHANGE
KNOWN ISSUES
NEXT TASK
EXPECTED FILES
EXPECTED TESTS
EXPECTED DOCUMENTATION
```

The next AI agent must not need to reconstruct project history.

---

# 49. CHANGELOG.md

Record every meaningful change.

Format:

```text
Date
Feature
Change
Files
Database
API
Tests
Risk
Notes
```

---

# 50. DEFINITION OF DONE

A feature is NOT complete until:

- Requirement verified

- Architecture verified

- Database completed

- Backend completed

- API completed

- Frontend completed

- Validation completed

- Security completed

- Unit tests completed

- Negative tests completed

- Calculation tests completed

- Documentation completed

- API documentation updated

- Database documentation updated

- Project status updated

- Context updated

- Next-agent handover updated

- Changelog updated

---

# 51. CONTEXT-LIMIT PROTOCOL

If the AI approaches context limitations, it must NOT simply stop.

Before stopping:

1. Save completed work.

2. Update `PROJECT_STATUS.md`.

3. Update `CURRENT_CONTEXT.md`.

4. Update `NEXT_AGENT.md`.

5. Update `CHANGELOG.md`.

6. Record modified files.

7. Record remaining work.

8. Record known issues.

9. Record exact next implementation step.

The next AI agent must be able to continue immediately.

---

# 52. AI DEVELOPMENT DECISION FRAMEWORK

Before implementing any requirement, ask internally:

```text
Is it in the SRS?
        ↓
Is it clarified in TDD?
        ↓
Does Test Plan define expected behavior?
        ↓
Does QA Tracker validate it?
        ↓
Does current architecture support it?
        ↓
What database changes are required?
        ↓
What API changes are required?
        ↓
What frontend changes are required?
        ↓
What security impact exists?
        ↓
What tests are required?
        ↓
What documentation must change?
```

If the answer cannot be established:

**STOP INVENTING.**

Identify the requirement gap.

---

# 53. CORE SYSTEM FLOW

The overall platform should conceptually support:

```text
CUSTOMER / TENANT
        ↓
PRODUCT
        ↓
PART
        ↓
REVISION
        ↓
BOM
        ↓
COST MODEL
        ↓
SUPPLIER QUOTATIONS
        ↓
SHOULD COST
        ↓
TCO
        ↓
SUPPLIER DISCOVERY
        ↓
SUPPLIER QUALIFICATION
        ↓
QUESTIONNAIRE
        ↓
EVIDENCE
        ↓
MANDATORY GATES
        ↓
SUPPLIER SCORING
        ↓
RISK ANALYSIS
        ↓
SUPPLIER COMPARISON
        ↓
AI RECOMMENDATION
        ↓
EXPLAINABILITY
        ↓
HUMAN APPROVAL
        ↓
APPROVED SUPPLIER
```

---

# 54. FUNDAMENTAL PRINCIPLE

The platform must not merely answer:

> "Which supplier is cheapest?"

It must answer:

> "Which eligible supplier provides the best overall commercial, technical, quality, delivery, service and risk proposition based on verified evidence and approved business rules?"

The system must always preserve:

```text
DATA
+
CALCULATION
+
EVIDENCE
+
RULES
+
RISK
+
EXPLANATION
+
HUMAN CONTROL
```

PRODUCT-COSTING-SOFTWARE/

├── AI_AGENT/

│ ├── PRODUCT_COSTING_HELP\_[AGENT.md](http://AGENT.md) ← behavior/instructions

│ ├── PRODUCT_COSTING_HELP_AGENT\_[RULES.md](http://RULES.md) ← safety/security rules

│ ├── knowledge/

│ │ ├── product_costing_qa_5000.json

│ │ ├── product_costing_qa_5000.jsonl

│ │ └── ...

│ └── [README.md](http://README.md)

├── frontend/

├── backend/

└── ...

This is the foundation of the Product Costing + Supplier Intelligence platform.