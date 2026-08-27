# NEXT_AGENT

## CURRENT
MASTER-V237: compile fix + Docker Dockerfile hardened.  
Offline Maven package SUCCESS. Docker image build **must be done on host**.

## NEXT
1. On machine with Docker: `export JWT_SECRET=... && docker compose up --build -d`
2. Confirm `curl http://127.0.0.1:8080/actuator/health` → UP
3. Confirm frontend http://localhost:8088
4. Customer migrate Flyway V126; test APPROVED PR → Create RFQ
5. Wave D (PO) only on explicit authorization

## DO NOT
- Duplicate RFQ / weaken module gates / invent FX / AI approve
- Claim Docker PASS without host evidence
- Rewrite A–E SAP modules
