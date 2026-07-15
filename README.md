# cloud-itonami-isic-2013: Manufacture of plastics and synthetic rubber in primary forms

Open Business Blueprint for **ISIC Rev.5 2013**: manufacture of plastics and synthetic rubber in primary forms — an autonomous "actor" (LLM advisor behind an independent Governor, langgraph-clj StateGraph, append-only audit ledger) that coordinates back-office **primary-forms plant operations**: production-batch data logging (polymer-grade/weight/off-spec-rate), polymerization/compounding-reactor maintenance scheduling, safety-concern flagging, and outbound resin/synthetic-rubber shipment coordination.

This repository designs a forkable OSS business for primary-forms
plastics-and-synthetic-rubber plant operations: run by a qualified
operator so a plant keeps its own operating records instead of
renting a closed SaaS.

## Scope: upstream primary forms, not downstream molding

ISIC 2013 covers the **chemical-process plant** that polymerizes
monomers and compounds additives into plastics resin and synthetic
rubber, output as pellets, granules, powder, flake, or latex — a
**primary form**, ready to sell or ship to a downstream converter.
This is distinct from `cloud-itonami-isic-2220` (Manufacture of
plastics products), which molds/extrudes/blow-molds those primary
forms into finished products. The two verticals share a very similar
governed-actor shape (both are back-office plant-operations
coordinators with a verified/registered equipment+batch gate and a
permanent equipment-actuation block) but this plant's hazard profile
is chemical (monomer exposure, exothermic runaway-reaction risk during
polymerization), not 2220's thermal/pressure molding hazard.

## What this actor does

Proposes **plant operations coordination**, not equipment operation:
- `:log-production-batch` — polymer-grade/weight/off-spec-rate data logging (administrative, not an operational decision)
- `:schedule-maintenance` — polymerization/compounding-reactor maintenance scheduling proposal
- `:flag-safety-concern` — surface a chemical-hazard (monomer exposure, exothermic-reaction risk)/equipment-safety concern (always escalates)
- `:coordinate-shipment` — outbound resin/synthetic-rubber shipment coordination proposal

## What this actor does NOT do

**CRITICAL SCOPE BOUNDARY — this is a safety-critical domain**
(polymerization/compounding reactor equipment, monomer exposure and
exothermic runaway-reaction hazard, chemical process hazard):

- Does NOT control polymerization or compounding reactor/line equipment directly
- Does NOT make plant-safety or chemical-safety decisions (that's the plant supervisor's exclusive human authority)
- Does NOT actuate the polymerization/compounding reactor (human plant supervisor decides)
- ONLY proposes/coordinates operations back-office; all actuation requires explicit human approval
- Safety-concern flagging ALWAYS escalates — never auto-decided, no confidence threshold or phase below escalation

## Architecture

Classic governed-actor pattern (`resinmfg.operation/build`, a langgraph-clj StateGraph):
1. **`resinmfg.advisor`** (sealed intelligence node, `ResinAdvisor`): proposes decisions only, never commits
2. **`resinmfg.governor`** (independent, `Primary Forms Plant Operations Governor`): validates against domain rules, re-derived from `resinmfg.registry`'s pure functions and `resinmfg.store`'s SSoT -- never trusts the advisor's own self-report
   - HARD invariants (always `:hold`, no override):
     - Plant/batch record must be independently verified/registered (`:verified?` AND `:registered?`) before any action is taken against it (equipment before maintenance scheduling, batch before shipment coordination)
     - The request's own `:effect` must be `:propose` (never a direct-write bypass)
     - `:op` must be in the closed four-op allowlist
     - The proposal's own `:effect` must be one of the four propose-shaped effects (no direct reactor/polymerization-line-equipment control)
     - Directly actuating the polymerization/compounding reactor (`:actuate-reactor? true`) is a PERMANENT, unconditional block
     - A shipment may not push a batch's own recorded shipped weight past its own logged production weight (independently recomputed)
     - No double-scheduling the same maintenance record
     - No fabricated `:polymer-grade` value on a production-batch patch
     - No physically implausible `:off-spec-rate-percent` value on a production-batch patch
   - ESCALATE (always human sign-off, overridable by a human):
     - `:flag-safety-concern` always escalates, regardless of confidence
     - Low-confidence proposals
3. **`resinmfg.phase`** (Phase 0->3 rollout): `:schedule-maintenance`/`:flag-safety-concern`/`:coordinate-shipment` are NEVER in any phase's `:auto` set (permanent, matching the governor's own posture); only `:log-production-batch` may auto-commit at phase 3 when clean
4. **`resinmfg.store`** (append-only audit ledger + SSoT): a single `MemStore` backend behind a `Store` protocol (see ns docstring for why a second Datomic-backed backend is out of scope for this build)

## Development

```bash
# Run tests (top-level deps.edn already pins langgraph+langchain local/root)
clojure -M:test

# Run tests via the workspace :dev override alias (equivalent, kept for sibling-repo parity)
clojure -M:dev:test

# Run the demo
clojure -M:dev:run

# Lint
clojure -M:lint
```

## Status

`:implemented` — `governor.cljc`/`store.cljc`/`advisor.cljc`/`registry.cljc` + `deps.edn` complete the module set; tests green, demo runnable, langgraph-clj integration verified.

## License

AGPL-3.0-or-later
