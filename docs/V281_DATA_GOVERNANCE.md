# V281 Data Governance — Trade Data

## Rule
Do not hard-code production tariff or agreement status in Java.

## Import path
`POST /api/v1/fta/import/agreements`
`POST /api/v1/fta/import/concessions/{agreementId}`

Required fields: source, officialReference, effective dates, version.

Rejected without authoritative metadata.
