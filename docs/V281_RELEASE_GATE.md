# V281 Release Gate

Release allowed when:
1. Backend full suite green
2. Flyway IT green on MySQL 8 (release-gate.yml)
3. FTA + Import golden green
4. FTA requires IMPORT_COSTING enforced
5. No fabricated tariff rates in Java

Blocked without: live MySQL migration proof in target environment.
