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

## COBOL-to-Java shadow operation

The COBOL adapter remains the only authoritative decision source. For every adjudication, shadow mode also
invokes the candidate Java rules and records an allow-listed comparison. The comparison contains only the
random correlation ID, dispositions, reason codes and their set differences, mismatch category, Java rule
version, and UTC evaluation timestamp. It never contains claim/member/provider identifiers, service dates,
amounts, duplicate details, or human-readable reason text. A Java exception is isolated from the response and
recorded as a `DEFECT`; the COBOL disposition is still returned.

Differences are operationally triaged as:

* `EXPECTED_REPRESENTATION_DIFFERENCE` — dispositions agree but reason-code representation differs.
* `RULE_DIFFERENCE` — dispositions disagree under the same data snapshot.
* `DATA_TIMING_DIFFERENCE` — dispositions disagree and a `DATA-*` or `TIMING-*` reason identifies snapshot lag.
* `DEFECT` — the Java evaluation cannot complete. Exact agreement is recorded as `NONE`.

Java decisions must **not** be enabled until the readiness gate passes on representative production traffic:
at least **99.5% exact disposition-and-reason-code agreement**, across at least **10,000 evaluations per complete
UTC-day window**, for **14 consecutive daily windows**. Traffic must cover the production mix of benefit plans,
claim types, dispositions, amounts, providers, service-date ages, and known edge cases; representativeness is
validated using aggregate distributions outside the PHI-free comparison event. Any failed or undersized window
resets the sustained-window count. Promotion also requires normal change approval and rollback readiness; the
threshold is necessary, not by itself sufficient. These controls are configured under `claim-check.shadow.parity`.
