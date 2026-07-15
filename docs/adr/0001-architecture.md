# ADR-0001: ResinAdvisor ⊣ Primary Forms Plant Operations Governor architecture

## Status

Accepted. `cloud-itonami-isic-2013` promoted from `:spec` to
`:implemented` in the `kotoba-lang/industry` registry, following the
verified fresh-scaffold protocol established by prior actors in this
fleet.

## Context

`cloud-itonami-isic-2013` publishes an OSS blueprint for plastics-and-
synthetic-rubber PRIMARY FORMS **plant operations coordination**
(production-batch polymer-grade/weight/off-spec-rate data logging,
polymerization/compounding-reactor maintenance scheduling, safety-
concern flagging, and outbound resin/synthetic-rubber shipment
coordination). Like every actor in this fleet, the blueprint alone is
not an implementation: this ADR records the governed-actor
architecture that promotes it to real, tested code, following the
same langgraph StateGraph + independent Governor + Phase 0->3 rollout
pattern established across the cloud-itonami fleet.

The closest domain analog is `cloud-itonami-isic-2220` (Manufacture of
plastics products): both are back-office coordination actors for a
fixed processing PLANT with heavy manufacturing equipment and a real
physical safety dimension, and both share the same four-op shape
(`:log-production-batch`/`:schedule-maintenance`/`:flag-safety-
concern`/`:coordinate-shipment`) and the same two-entity verified/
registered gate structure (equipment for maintenance scheduling, batch
for shipment coordination). The two verticals are, however, distinct
plants at distinct points in the value chain: 2013 is the UPSTREAM
chemical-process plant (polymerization/compounding reactors producing
plastics resin and synthetic rubber as pellets/granules/powder/flake/
latex -- a PRIMARY FORM), while 2220 is the DOWNSTREAM plant that
molds/extrudes/blow-molds those primary forms into finished products.
This build mirrors 2220's architecture closely (itself informed by
`cloud-itonami-isic-1610`'s Sawmilling-and-planing-of-wood actor) but
adapts the hazard profile and equipment/product vocabulary to the
upstream chemical-process plant: 2013's central physical hazard is
chemical (monomer exposure, exothermic runaway-reaction risk during
polymerization) rather than 2220's thermal/pressure molding hazard;
2013's permanent equipment-actuation block guards a polymerization/
compounding REACTOR (`:actuate-reactor?`) rather than a molding/
extrusion LINE (`:actuate-line?`); and 2013's production-batch record
declares a `:polymer-grade` (spanning both thermoplastic resins and
synthetic rubbers, per ISIC 2013's own combined scope) and an
`:off-spec-rate-percent`, rather than 2220's `:resin-type`/`:reject-
rate-percent` (2220's batch is always a finished thermoplastic
product, never a synthetic rubber).

This vertical has NO pre-existing `kotoba-lang/resinmfg`-style
capability library to wrap (verified: no such repo exists). This build
therefore uses self-contained domain logic — pure functions in
`resinmfg.registry` (equipment/batch verification, shipment-weight
recompute, polymer-grade validation, off-spec-rate plausibility
validation) are re-verified independently by the governor, the same
"ground truth, not self-report" discipline established across prior
actors (most directly `cloud-itonami-isic-2220`'s `plasticsmfg.registry`).

This blueprint's own `:itonami.blueprint/governor` keyword,
`:primary-forms-plant-operations-governor`, is grep-verified UNIQUE
fleet-wide (`gh search code "primary-forms-plant-operations-governor"
--owner cloud-itonami`, zero hits before this repo was created).

## Decision

### Decision 1: Self-contained domain logic (no external primary-forms-manufacturing capability library to wrap)

Unlike actors that delegate to pre-existing domain libraries, this
primary-forms plastics-and-synthetic-rubber vertical has NO
pre-existing capability library to wrap. The equipment/batch-
verification / shipment-weight / polymer-grade / off-spec-rate
validation functions live as pure functions in `resinmfg.registry` and
are re-verified independently by `resinmfg.governor` — the same
"ground truth, not self-report" discipline established across prior
actors (most directly `cloud-itonami-isic-2220`'s `plasticsmfg.registry`).

### Decision 2: Coordination, not control — scope boundary at the back-office

This actor is **strictly back-office coordination** of primary-forms
plastics-and-synthetic-rubber plant operations. It does NOT:
- Control polymerization or compounding reactor/line equipment directly
- Make plant-safety or chemical-safety decisions (exclusive to the human plant supervisor)
- Actuate the polymerization/compounding reactor

All proposals are `:effect :propose` only. The advisor proposes; the
governor validates; escalation paths funnel to human plant-supervisor
approval. This is not a replacement for the supervisor's authority —
it is a proposal-screening and documentation layer.

**CRITICAL SAFETY BOUNDARY**: primary-forms plastics-and-synthetic-
rubber manufacturing is a safety-critical domain (monomer exposure,
exothermic runaway-reaction risk during polymerization, chemical
process hazard, heavy material handling). Safety-concern flagging
NEVER auto-commits. All safety concerns escalate immediately to human
review.

### Decision 3: Safety-concern escalation — always human sign-off

`:flag-safety-concern` (chemical-hazard concern, monomer-exposure/
exothermic-reaction-risk, equipment-safety concern, crew fatigue)
ALWAYS escalates, never auto-commits. This is not a "low-stakes
proposal" — it is a circuit-breaker that must reach human authority.

### Decision 4: Two independent verified/registered gates (equipment AND batch), not one

Like `cloud-itonami-isic-2220`, this vertical has TWO entity kinds
each gating a different op: `:schedule-maintenance` independently
verifies the referenced **equipment** unit's own `:verified?`/
`:registered?` fields; `:coordinate-shipment` independently verifies
the referenced **batch**'s own `:verified?`/`:registered?` fields.
Both are the same "plant/batch record must be independently
verified/registered before any action" HARD invariant applied to the
two distinct record kinds this domain actually has.
`:coordinate-shipment` additionally independently recomputes whether a
batch's own recorded shipped-to-date weight plus the proposal's own
claimed weight would exceed the batch's own recorded production
weight — never taken on the advisor's self-report.

### Decision 5: HARD invariants (no override)

Four HARD governor invariants (elaborated into ten concrete checks in
`resinmfg.governor`, mirroring `cloud-itonami-isic-2220`'s own
elaboration of its HARD invariants into concrete checks) block
proposals and cannot be overridden by human approval:
1. Plant/batch record (equipment for maintenance, batch for shipment) must be independently verified/registered before any action is taken against it, and a shipment's weight must independently recompute within the batch's own logged production weight
2. Proposals must be `:effect :propose` only (never direct equipment control)
3. Direct polymerization/compounding-reactor-equipment control or reactor actuation is permanently blocked
4. The op allowlist is closed — `:log-production-batch`/`:schedule-maintenance`/`:flag-safety-concern`/`:coordinate-shipment` only

## Consequences

(+) Primary-forms plastics-and-synthetic-rubber plant operations
back-office now has a documented, governed, auditable coordination
layer that funnels all decisions through independent validation
before human approval.

(+) The "coordination, not control" boundary is explicit in code: all
`:effect :propose`, all real-world actuation requires human plant-
supervisor sign-off.

(+) Scope is bounded and verifiable: four HARD invariants (elaborated
into ten concrete governor checks) protect against scope creep into
unauthorized equipment operation or reactor actuation. Safety concerns
are a circuit-breaker, not a threshold.

(+) Safety-critical discipline is explicit: safety-concern flagging
cannot be rate-limited, suppressed, or auto-decided by phase gate.
Human review is mandatory.

(-) Still a simulation/proposal layer, not a real plant-operations
control system. Equipment actuation and reactor operation remain
human-controlled via external channels.

(-) No integration with real plant-management databases (equipment
telemetry, batch tracking, freight dispatch) — this is a standalone
coordinator blueprint.

## Verification

- `cloud-itonami-isic-2013`: `clojure -M:test` green (all tests pass;
  see the superproject ADR and `kotoba-lang/industry` registry entry
  for the exact `Ran N tests containing M assertions, 0 failures, 0
  errors` output, verified from an independent fresh clone), `clojure
  -M:lint` clean, `clojure -M:dev:run` demo narrative exercises
  proposal submission, escalation, and every HARD-hold scenario
  directly (not-propose-effect, unknown-op, equipment-not-verified,
  batch-not-verified, shipment-weight-exceeded, reactor-actuate-
  blocked, already-scheduled, invalid-polymer-grade, invalid-off-spec-
  rate).
- All source is `.cljc` (portable ClojureScript / JVM / nbb) — no
  JVM-only interop; the actor graph is invoked exclusively via
  `langgraph.graph/run*` (not `.invoke`, which is not cljs-portable).
- Audit ledger is append-only, all decisions are traced; every settled
  request (commit or hold) leaves exactly one ledger fact.
- `deps.edn` pins `io.github.kotoba-lang/langgraph` and
  `io.github.kotoba-lang/langchain` via `:local/root` directly in the
  top-level `:deps` (not only under a `:dev` alias), so a bare
  `clojure -M:test` resolves offline inside the monorepo checkout.
