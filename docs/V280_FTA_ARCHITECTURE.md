# V280 FTA / Trade Agreement Architecture

Optional platform-entitled feature: `FTA_TRADE_AGREEMENT` (module PRODUCT_COSTING).

## Independence
- V279 Normal Import + DutyEngine unchanged
- FTA OFF → normal import fully functional
- Preferential duty is a **separate** result; may be consumed by import costing later

## Engines (generic, not country-specific services)
- AgreementEffectivenessEngine — status + effective dates only
- OriginRuleEngine — WHOLLY_OBTAINED, RVC, CTH/CTSH/CTC, combination, cumulation
- TariffConcessionEngine — normal vs preferential duty compare
- TradeRemedyEngine — ADD/CVD/safeguard independent of preference

## Data governance
No production tariff rates hard-coded in Java. Rates live in versioned DB rows with source/effective dates.
