# Market Gap Delivery — V219

## Delivered (outside SAP A–E worklist)

### 1. Process carbon (P1)
- Emission factors: category **MATERIAL** or **PROCESS**
- PROCESS unit: `KG_CO2E_PER_HOUR`
- Should-cost emissions snapshot: material + process hours from routing cycle times
- Status: AVAILABLE / PARTIAL / NOT_AVAILABLE (never invent)

### 2. Benchmark analytics (P0 enhance)
- `GET /api/v1/benchmarks/analytics?bomNodeId=&category=`
- Should vs quote vs benchmark deltas; missing = DATA_NOT_AVAILABLE

### 3. Supplier portal UI (P1)
- Route `/supplier-portal`
- Login, my RFQs, quote submit via existing `/api/v1/portal`

## Not delivered (documented)

- PLM/CAD connector  
- Geometry-driven cost drivers  
- Automated routing optimization library  
