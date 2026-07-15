# Governance

`cloud-itonami-isic-2013` is an OSS open-business blueprint for plastics-and-synthetic-rubber primary-forms plant operations coordination.

## Maintainers
Maintainers may merge changes that preserve these invariants:
- a polymerization/compounding-reactor action the governor refuses is never dispatched to hardware.
- the Primary Forms Plant Operations Governor remains independent of the advisor.
- hard policy violations (equipment-control bypass, reactor actuation, record-suppression, unauthorized disclosure) cannot be overridden by human approval.
- every schedule, sign-off, record and disclose path is auditable.
- sensitive operating and personal data stays outside Git.

## Decision Records
Architecture decisions live in `docs/adr/`. Changes to the trust model, storage contract, public business model, operator certification or license should add or update an ADR.

## Operator Governance
Anyone may fork and operate independently. itonami.cloud certification is a separate trust mark and should require safety, audit and data-flow review.

Certified operators can lose certification for:
- bypassing polymerization/compounding-reactor-control or record policy checks
- mishandling sensitive data
- misrepresenting certification status
- failing to respond to security or safety incidents
