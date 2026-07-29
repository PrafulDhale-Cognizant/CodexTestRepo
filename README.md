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

Every response includes a decision status, stable coded reasons, processing time, timestamp, and correlation ID. Exact matches also include the existing claim reference and match confidence. A caller-supplied `X-Correlation-ID` is preserved; otherwise the service creates one.

`PENDED` is returned only when a rule cannot safely make a final automated decision. In this prototype, a unique claim above $5,000 produces `AMT-101`; exact duplicates remain denied (`DUP-001`), and claims within the limit remain approved. The service retains the decision, every relevant rule outcome, correlation ID, and only the procedure code, service date, amount, and any matched claim reference as analyst evidence.

Workflow tooling can idempotently create or retrieve an analyst item after a pend:

```http
POST /internal/v1/review-work-items
Content-Type: application/json

{"claimId":"CLM-542890"}
```

The claim ID is the idempotency key: repeated calls return the same work item. Requests for claims without a persisted `PENDED` decision return `404`.

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
