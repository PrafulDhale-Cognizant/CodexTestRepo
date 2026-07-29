# ClaimCheck

ClaimCheck extracts duplicate detection and basic eligibility rules from a nightly COBOL/AS400 workflow into a real-time Spring Boot service. Claims teams get an auditable decision in seconds while the source-of-truth legacy platform remains in place.

## Run locally

```bash
mvn spring-boot:run
```

Open <http://localhost:8080>. The pre-filled example matches `CLM-902184` in the mock legacy index and demonstrates the duplicate workflow. Change the procedure code to `99214` to see an approval, or enter an amount over $5,000 to see manual review.

## API

`POST /api/v1/claims/adjudicate`

```json
{
  "claimId": "CLM-542890",
  "memberId": "MBR-10482",
  "providerId": "PRV-4481",
  "procedureCode": "99213",
  "serviceDate": "2026-07-18",
  "amount": 185.00
}
```

Every response includes a decision status, coded reasons, processing time, timestamp, and trace ID. Exact matches also include the existing claim reference and match confidence.

## Rules in this prototype

1. An exact match on member, provider, procedure, service date, and amount is denied as a potential duplicate.
2. A unique claim over $5,000 is routed to manual review.
3. Other valid claims pass pre-adjudication.

These deliberately small, deterministic rules mirror the supplied hackathon scenario and create a clean seam where a production copybook adapter, member eligibility system, and externalized rules engine can be connected.

## Test and package

```bash
mvn test
mvn package
docker build -t claim-check .
```

Spring Boot Actuator exposes `/actuator/health` for deployment probes.

## Production rollout

The staged feature flags, promotion gates, reconciliation controls, accountable owners, and fail-closed dependency behavior are defined in [the rollout runbook](docs/rollout-runbook.md). No production flag may be advanced without satisfying that stage's entry criteria and recording the named owner's approval.
