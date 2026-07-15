(ns resinmfg.registry
  "Pure-function domain logic for the plastics-and-synthetic-rubber
  PRIMARY FORMS plant-operations coordination actor -- equipment/batch
  verification, shipment-weight recompute, polymer-grade validation,
  off-spec-rate plausibility validation, and draft maintenance-
  schedule/shipment-coordination record construction.

  Per docs/adr/0001-architecture.md Decision 1: this vertical has NO
  pre-existing `kotoba-lang/resinmfg`-style capability library to wrap
  (verified: no such repo exists). The domain logic therefore lives
  here as pure functions, re-verified INDEPENDENTLY by
  `resinmfg.governor` -- the same 'ground truth, not self-report'
  discipline every sibling actor's own registry establishes (e.g.
  `plasticsmfg.registry/shipment-weight-exceeded?` from
  `cloud-itonami-isic-2220`, the closest downstream sibling): never
  trust a proposal's own self-reported weight/status when the inputs
  needed to recompute it independently are already on record.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real plant-operations system. It builds the DRAFT record
  a plant coordinator would keep (a scheduled maintenance window, a
  coordinated shipment), not the act of actuating a polymerization/
  compounding reactor or dispatching a real freight carrier (this
  actor NEVER does either -- see README `What this actor does NOT
  do`).

  UPSTREAM vs. DOWNSTREAM scope note: ISIC 2013 (this actor) covers
  PRIMARY FORMS manufacturing -- polymerization/compounding reactors
  that produce plastics resin and synthetic rubber as pellets/
  granules/powder/flake/latex. This is a chemical-process plant
  upstream of ISIC 2220 (Manufacture of plastics products,
  `cloud-itonami-isic-2220`), which molds/extrudes/blow-molds those
  primary-form resins into finished products. The two verticals share
  a very similar governed-actor shape (both are back-office plant-
  operations coordinators with a verified/registered equipment+batch
  gate and a permanent equipment-actuation block) but are distinct
  plants with distinct hazard profiles: 2013's hazard is chemical
  (monomer exposure, exothermic runaway-reaction risk during
  polymerization) rather than 2220's thermal/pressure molding hazard.")

;; ----------------------------- constants -----------------------------

(def valid-polymer-grades
  "The closed set of polymer-grade values a production-batch record may
  declare -- thermoplastic resins AND synthetic rubbers, matching ISIC
  2013's own 'plastics and synthetic rubber in primary forms' scope
  (unlike a downstream molder, this plant's own output includes raw
  synthetic-rubber grades, not only thermoplastic resin grades).
  Anything else is a fabricated/unrecognized polymer grade -- the
  governor HARD-holds rather than let an invented grade pass through."
  #{;; thermoplastic resins (primary-form pellet/granule/powder output)
    :pp :pe-hdpe :pe-ldpe :pvc :ps :abs :pet :pc :nylon
    ;; synthetic rubbers (primary-form crumb/bale/latex output)
    :sbr :br :nbr :epdm :cr :iir})

(def valid-output-forms
  "The closed set of PRIMARY FORM shapes this plant's own output may
  take -- pellet/granule/powder/flake for solid resin/rubber crumb,
  latex for the liquid-dispersion synthetic-rubber output. A primary-
  forms plant never ships a molded/extruded finished shape (that is
  ISIC 2220's own downstream scope, not this actor's)."
  #{:pellet :granule :powder :flake :latex})

(def off-spec-rate-min-percent
  "Physical floor for a batch's own off-spec/reject-rate reading (zero
  off-spec output is the best possible outcome, never negative)."
  0.0)

(def off-spec-rate-max-percent
  "Physical ceiling for a batch's own off-spec/reject-rate reading -- a
  batch cannot reject more than 100% of its own output. A reading
  above this is implausible sensor/QC data, not a real batch."
  100.0)

;; ----------------------------- equipment checks -----------------------------

(defn equipment-verified?
  "Ground-truth check: has `equipment`'s own record been marked
  verified (i.e. it has actually been inspected/commissioned and
  registered in the SSoT, not merely referenced from an unverified
  maintenance request)? A pure predicate over the equipment's own
  permanent field -- no proposal inspection needed."
  [equipment]
  (true? (:verified? equipment)))

(defn equipment-registered?
  "Ground-truth check: does `equipment`'s own record carry a
  `:registered?` true flag (i.e. it is on file in the plant's
  equipment registry)? Scheduling maintenance against equipment that
  is not on file and registered is the exact scope violation this
  actor's HARD invariant ('plant/batch record must be independently
  verified/registered before any action') exists to block."
  [equipment]
  (true? (:registered? equipment)))

(defn equipment-ready?
  "Combined ground-truth gate: the equipment must be both `verified?`
  AND `registered?` before ANY maintenance may be scheduled against
  it. Two independent facts on the equipment's own permanent record,
  neither inferred from the advisor's own rationale."
  [equipment]
  (and (equipment-verified? equipment) (equipment-registered? equipment)))

;; ----------------------------- batch checks -----------------------------

(defn batch-verified?
  "Ground-truth check: has `batch`'s own record been marked verified
  (i.e. its polymer-grade/weight/off-spec-rate claims have actually
  been QC-inspected, not merely logged from an unverified intake
  patch)?"
  [batch]
  (true? (:verified? batch)))

(defn batch-registered?
  "Ground-truth check: is `batch`'s own record on file in the plant's
  production ledger? Coordinating a shipment against a batch that is
  not on file and registered is the exact scope violation this
  actor's HARD invariant ('plant/batch record must be independently
  verified/registered before any action') exists to block."
  [batch]
  (true? (:registered? batch)))

(defn batch-ready?
  "Combined ground-truth gate: the batch must be both `verified?` AND
  `registered?` before ANY shipment may be coordinated against it."
  [batch]
  (and (batch-verified? batch) (batch-registered? batch)))

(defn shipment-weight-exceeded?
  "Ground-truth check for a `:coordinate-shipment` proposal:
  would `shipped-to-date-kg` + `new-weight-kg` exceed `batch`'s own
  recorded `:weight-kg` (the batch's own logged production weight)?
  Needs no proposal inspection or stored-verdict lookup -- its inputs
  are permanent fields already on the batch's own record, the same
  shape every sibling actor's own cost/total-matching check uses."
  [batch new-weight-kg]
  (let [capacity (:weight-kg batch)
        so-far (:shipped-weight-kg batch 0.0)]
    (and (number? capacity)
         (number? new-weight-kg)
         (> (+ (double so-far) (double new-weight-kg)) (double capacity)))))

(defn polymer-grade-valid?
  "Is `polymer-grade` one of the closed, known polymer-grade values
  (thermoplastic resin or synthetic rubber)? nil/blank is treated as
  invalid (a production-batch patch must declare a real polymer grade,
  not omit it silently)."
  [polymer-grade]
  (contains? valid-polymer-grades polymer-grade))

(defn off-spec-rate-valid?
  "Is `percent` a physically plausible batch off-spec/reject-rate
  reading? Rejects nil, non-numbers, negative values, and values
  beyond `off-spec-rate-max-percent` -- a fabricated or sensor-error
  reading, never let through as a real batch fact."
  [percent]
  (and (number? percent)
       (>= (double percent) off-spec-rate-min-percent)
       (<= (double percent) off-spec-rate-max-percent)))

;; ----------------------------- draft record construction -----------------------------

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the human plant supervisor's/shipping approver's act, not this
  actor's."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn register-maintenance
  "Validate + construct the MAINTENANCE-SCHEDULE DRAFT -- a proposed
  polymerization/compounding-reactor maintenance window against a
  verified, registered piece of equipment. Pure function -- does not
  actuate the reactor/compounding line or execute any maintenance; it
  builds the RECORD a plant coordinator would keep. `resinmfg.governor`
  independently re-verifies the equipment's own verified/registered
  ground truth, and permanently blocks any attempt to directly actuate
  the reactor/polymerization line (see README `Actuation`), before
  this is ever allowed to commit."
  [maintenance-id equipment-id sequence]
  (when-not (and maintenance-id (not= maintenance-id ""))
    (throw (ex-info "maintenance: maintenance_id required" {})))
  (when-not (and equipment-id (not= equipment-id ""))
    (throw (ex-info "maintenance: equipment_id required" {})))
  (when (< sequence 0)
    (throw (ex-info "maintenance: sequence must be >= 0" {})))
  (let [maintenance-number (str "MNT-" (zero-pad sequence 6))
        record {"record_id" maintenance-number
                "kind" "maintenance-schedule-draft"
                "maintenance_id" maintenance-id
                "equipment_id" equipment-id
                "immutable" true}]
    {"record" record "maintenance_number" maintenance-number
     "certificate" (unsigned-certificate "MaintenanceSchedule" maintenance-number maintenance-number)}))

(defn register-shipment
  "Validate + construct the SHIPMENT-COORDINATION DRAFT -- a proposed
  outbound resin/synthetic-rubber primary-form shipment against a
  verified, registered production batch. Pure function -- does not
  dispatch any real freight carrier; it builds the RECORD a plant
  coordinator would keep. `resinmfg.governor` independently
  re-verifies the shipment's own claimed weight against
  `shipment-weight-exceeded?`, before this is ever allowed to commit."
  [shipment-id sequence]
  (when-not (and shipment-id (not= shipment-id ""))
    (throw (ex-info "shipment: shipment_id required" {})))
  (when (< sequence 0)
    (throw (ex-info "shipment: sequence must be >= 0" {})))
  (let [shipment-number (str "SHP-" (zero-pad sequence 6))
        record {"record_id" shipment-number
                "kind" "shipment-coordination-draft"
                "shipment_id" shipment-id
                "immutable" true}]
    {"record" record "shipment_number" shipment-number
     "certificate" (unsigned-certificate "ShipmentCoordination" shipment-number shipment-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
