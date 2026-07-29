# COBOL adjudication rule map

This is the extraction map used to capture the parity fixtures. Names are retained in
COBOL form so a fixture failure can be traced back to the legacy batch.

## Decision paragraphs

| Paragraph | Responsibility | Result |
| --- | --- | --- |
| `2100-VALIDATE-CLAIM` | Rejects spaces/zeroes in required claim fields before business rules run. | `REJECTED / VAL-001` |
| `3100-CHECK-ELIGIBILITY` | Reads the member row and requires status `A`; then treats both the effective and termination dates as inclusive. | `DENIED / ELG-001`, `ELG-002`, or `ELG-003` |
| `4100-CHECK-DUPLICATE` | Finds an exact match on member, provider, procedure, service date, and amount. | `DENIED / DUP-001` |
| `5100-CHECK-AMOUNT` | Sends amounts strictly greater than `5000.00` to review. | `REVIEW / AMT-101` |
| `9000-APPROVE-CLAIM` | Approves a complete, eligible, unique claim at or below the amount limit. | `APPROVED / ELG-000,DUP-000` |

Rule order is significant: validation, eligibility, duplicate detection, then the
amount threshold. The fixture's `cobolResult` is the sanitized capture of the status
and reason fields after that sequence.

## `CLAIM-RECORD` fields used

| Field | Used by |
| --- | --- |
| `CLM-CLAIM-ID` | required-field validation and response correlation |
| `CLM-MEMBER-ID` | required-field validation, member lookup, duplicate key |
| `CLM-PROVIDER-ID` | required-field validation, duplicate key |
| `CLM-PROCEDURE-CODE` | required-field validation, duplicate key |
| `CLM-SERVICE-DATE` | required-field validation, coverage check, duplicate key |
| `CLM-AMOUNT` | required-field/range validation, duplicate key, review threshold |

Eligibility attributes are not claim input: `MBR-STATUS`, `MBR-EFFECTIVE-DATE`, and
`MBR-TERMINATION-DATE` come from the member row selected by `CLM-MEMBER-ID`.
The Java service uses a deliberately small sanitized member index for these contract
cases; production integration should replace it with the member eligibility adapter.
