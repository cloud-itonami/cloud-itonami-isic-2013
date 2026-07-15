(ns resinmfg.robotics
  "Robot-executed compounded resin/rubber test-specimen tensile-test
  verification -- the concrete, actor-level realization of
  ADR-2607142800's fleet-wide robotics-process-simulation pattern
  (established by `cloud-itonami-isic-2910`'s `automotive.robotics`,
  extended to a design-library-less weld/fastener pull test by
  `autoparts.robotics`/ADR-2607152000, to a design-library-less
  direct-approach test by `deviceassembly.robotics`/ADR-2607991500, to a
  same-domain steel-coupon tensile test by `steelworks.robotics`/
  ADR-2607999600, and to a keel-block/Y-block cast-coupon tensile test
  by `foundrymfg.robotics`/ADR-2607999800), applied here (ADR-2607999900)
  to THIS actor's own `resinmfg.store` batch record -- the FIFTH and
  LAST vertical in this session's robotics-simulation-coverage sequence
  (isic-2811/2410/2211/2431/2013).

  HONEST FRAMING OF THE GAP THIS NS CLOSES: like `foundrymfg.registry`
  (unlike `steelworks.facts`), this vertical has NO evidence-checklist
  namespace at all -- per `resinmfg.registry`'s own ns docstring
  (Decision 1 of docs/adr/0001-architecture.md), this vertical's domain
  logic is pure functions with no pre-existing capability library to
  wrap. A production batch's own mechanical/quality claims
  (`:polymer-grade`/`:off-spec-rate-percent`) are therefore
  SELF-REPORTED intake-patch fields, validated ONLY by closed-set
  membership / plausibility-bound checks (`resinmfg.registry/polymer-
  grade-valid?`/`off-spec-rate-valid?`) -- never by any physics-derived
  reading of the batch's ACTUAL mechanical properties. This ns closes
  that gap with a real, physics-derived reading, ADDITIVE alongside
  (never replacing) those existing self-reported checks -- an unrelated
  QA domain (mechanical tensile-load qualification vs. closed-set
  polymer-grade membership / off-spec-rate plausibility).

  A genuine, real, extremely standard polymer-processing QA procedure:
  the COMPOUNDED RESIN/RUBBER TEST-SPECIMEN TENSILE TEST (ASTM D638 for
  plastics / ASTM D412 for vulcanized/thermoplastic rubber -- the two
  sibling standard test methods covering exactly this actor's own
  `resinmfg.registry/valid-polymer-grades` split between thermoplastic
  resins and synthetic rubbers). A dogbone (D638) or dumbbell (D412)
  test specimen, molded or die-cut from a representative sample of the
  SAME batch's own compounded material, is gripped at both ends and
  pulled apart at a controlled rate until its peak tensile load is
  reached; that peak load is compared against the material class's own
  required minimum tensile-load spec BEFORE the batch may ship. This is
  the standard way a resin/rubber compounder qualifies a batch's actual
  mechanical properties (as opposed to its polymer-grade classification
  or off-spec/reject-rate reading), and is what a real primary-forms
  plant's own test-specimen tensile-test record would show. This
  vertical has no design-library sibling repo (like `steelworks.
  robotics`/`foundrymfg.robotics`, unlike automotive's `kami-engine-
  vehicle-designer` pairing), so the physics module is built DIRECTLY in
  this ns, taking a real git-coordinate dependency on `kotoba-lang/
  physics-2d` alone (see deps.edn) -- the same shape `autoparts.
  robotics`/`deviceassembly.robotics`/`steelworks.robotics`/`foundrymfg.
  robotics` already established for a design-library-less vertical.

  HONEST REINTERPRETATION TECHNIQUE (mirrors `autoparts.robotics`'s/
  `steelworks.robotics`'s/`foundrymfg.robotics`'s disclosed 'reaching
  end-of-tether, not literally crashing into a barrier' trick):
  `physics-2d`'s `world-step` ONLY natively resolves bodies that are
  APPROACHING/colliding -- it has no notion of a body SEPARATING under
  tension, so there is no direct way to simulate 'pull the specimen
  apart until its load peaks' with this engine's collision-only impulse
  resolver. This ns reframes the SAME physical event as an approach
  instead: a `:jaw` (the moving test-rig grip) starts right beside a
  `:fixture` (a static body anchoring the specimen's OTHER grip end) and
  moves steadily AWAY from it at a real, controlled crosshead-equivalent
  pull rate -- but a THIRD, static `:limit-boundary` body is placed
  exactly `travel-to-peak-load-m` (the specimen's own real
  plastic-extension distance to reach its peak tensile load, before
  further drawing/necking/break) beyond the jaw's start. As the jaw
  travels, it is really the SPECIMEN running out of extensibility before
  its peak load is reached -- `physics-2d` only knows how to render that
  as the jaw's leading face reaching the limit-boundary's near face, at
  which point its native inelastic (restitution 0) collision resolution
  zeroes the jaw's velocity in a SINGLE tick -- exactly the 'load rises,
  then peaks/arrests' event a real tensile test exhibits at its
  peak-load point. The peak deceleration read off that tick, times the
  batch's own recorded effective participating mass
  (`:specimen-mass-kg` -- the moving jaw + the locally-engaged
  dogbone/dumbbell specimen gauge-length material, the SAME 'effective
  participating mass' framing `autoparts.robotics`'s `:joint-mass-kg`/
  `steelworks.robotics`'s and `foundrymfg.robotics`'s `:coupon-mass-kg`
  use), is `:sim-tensile-load-n` (Newtons) -- REAL, derived from the
  actual simulated trajectory, never invented.

  Disclosed engineering priors (this ns's own, not measured facts --
  same discipline as `autoparts.robotics`'s/`steelworks.robotics`'s/
  `foundrymfg.robotics`'s pull-test constants):

  - `test-speed-mps` models a genuine, established test category --
    high-strain-rate/dynamic tensile-property qualification (the SAME
    ISO 26203-family regime `steelworks.robotics`'s/`foundrymfg.
    robotics`'s own `test-speed-mps` disclose, plus this material
    family's own high-speed tensile-impact analogs: ASTM D1822 for
    plastics, ISO 34-1/ASTM D624 tear-adjacent dynamic rubber testing),
    run at a representative low-single-digit m/s rate -- NOT the mm/min
    quasi-static crosshead speed ASTM D638's/D412's own baseline method
    uses. The SAME honest disclosure `autoparts.robotics`'s/
    `steelworks.robotics`'s/`foundrymfg.robotics`'s `test-speed-mps`
    make applies here identically: this single-tick 'boxcar' technique
    can only honestly render a meaningful force reading at a genuinely
    fast/dynamic rate (peak-decel = test-speed^2 / travel-to-peak-load
    scales with the SQUARE of speed, so a slow quasi-static rate is the
    wrong physical regime for a discrete-collision technique).
  - `travel-to-peak-load-m` is a representative plastic-extension
    distance (m) a standard dogbone/dumbbell specimen on a ~50 mm gauge
    length travels before reaching its PEAK tensile load -- disclosed at
    a low-single-digit-mm order of magnitude representative of the MORE
    RIGID engineering-thermoplastic end of this actor's own
    `resinmfg.registry/valid-polymer-grades` mix (yield-point elongation
    on the order of a few percent of gauge length for grades like PC/
    nylon/PET). Synthetic-rubber grades in the SAME set
    (`:sbr`/`:br`/`:nbr`/`:epdm`/`:cr`/`:iir`) would in reality reach
    their OWN, much-later peak load at a far larger elongation
    (elastomers routinely exceed several-hundred-percent elongation at
    break) -- but a single, disclosed, fleet-wide travel constant, not a
    per-material-family figure, mirrors the SAME simplification
    `foundrymfg.robotics/travel-to-peak-load-m`'s own docstring
    discloses for spanning a mixed alloy-grade set (there: cast-iron
    through cast-steel grades; here: rigid thermoplastic through
    elastomer grades).
  - `initial-grip-slack-m` is a small, real, disclosed test-fixture
    grip-seating/alignment slack the jaw travels BEFORE the specimen
    itself begins to bear load -- present only so the simulated
    trajectory captures a real pre-load approach phase, not just the
    single stopping tick (mirrors `autoparts.robotics`'s/`steelworks.
    robotics`'s/`foundrymfg.robotics`'s `initial-grip-slack-m`).
  - `min-tensile-load-n` is a newly-defined, clearly-disclosed
    real-world floor (the SAME allowance ADR-2607152000/ADR-2607999600/
    ADR-2607999800 gave `autoparts.robotics/min-proof-load-n`/
    `steelworks.robotics/min-tensile-load-n`/`foundrymfg.robotics/
    min-tensile-load-n`, applied here to the same kind of reading). A
    standard ASTM D638 Type I dogbone specimen's narrow (reduced)
    section (13 mm width x 3.2 mm thickness, cross-sectional area ~41.6
    mm^2) at a conservatively low tensile strength representative of the
    WEAKEST common grade in this actor's own `valid-polymer-grades` set
    (butyl rubber / IIR, ASTM D412-class minimum tensile strength on the
    order of 7 MPa) computes to roughly 291 N; this ns places its floor
    CONSERVATIVELY BELOW that computed figure (200 N) specifically so a
    legitimately weak-but-passing soft-elastomer batch is never falsely
    failed by a single fleet-wide floor spanning grades from soft
    synthetic rubber up through rigid engineering thermoplastics (PC/
    nylon/PET tensile strengths run an order of magnitude higher) -- a
    newly-defined, disclosed bound, NOT a literal per-grade transcription
    of any one named standard's exact number (the SAME conservative-
    floor-below-the-weakest-grade logic `foundrymfg.robotics/min-
    tensile-load-n`'s own docstring discloses).

  Like `autoparts.robotics`'s/`steelworks.robotics`'s/`foundrymfg.
  robotics`'s pull-test/tensile-test readings, the quantity reported
  HERE is a FORCE (Newtons), so `:specimen-mass-kg` DOES directly scale
  `:sim-tensile-load-n` (force = mass x deceleration) -- intentional,
  not an oversight: a real load-cell reading legitimately depends on the
  physical scale of the specimen/fixture under test, not an accident of
  chosen units.

  `tensile-test-out-of-tolerance?` is a pure comparator: it reads
  `:sim-tensile-load-n` off whatever map it is given (mirrors
  `steelworks.robotics/tensile-test-out-of-tolerance?`/`foundrymfg.
  robotics/tensile-test-out-of-tolerance?`). `simulation-out-of-
  tolerance?` is the governor-facing entry point -- recomputes the REAL
  simulation FRESH from the batch's own permanent `:specimen-mass-kg`
  field on EVERY call -- this actor has no separate robot-mission-run/
  store-write step wired into `resinmfg.operation` yet (`simulate-
  tensile-test-cell` below exists for API parity with sibling actors'
  robotics namespaces and future advisor wiring), so `resinmfg.governor`
  calls this always-fresh recompute directly, needing no proposal
  inspection or stored-verdict lookup at all -- the SAME shape
  `resinmfg.registry/shipment-weight-exceeded?` already established for
  this actor, extended to a physics-derived reading. A batch with no
  `:specimen-mass-kg` on file (no tensile-test specimen molded/tested
  yet) is NEVER silently treated as a violation -- the same disclosed
  'missing telemetry != violation' discipline `deviceassembly.robotics/
  connector-mating-force-out-of-tolerance?`/`steelworks.robotics/
  simulation-out-of-tolerance?`/`foundrymfg.robotics/simulation-out-of-
  tolerance?` establish.

  Pure data + pure functions -- no real robot I/O, no network.
  `physics-2d/world-step` is itself a pure, fixed-timestep integrator
  (no wall-clock/IO), so this stays exactly as offline/deterministic as
  every other sibling namespace in this fleet -- tests run without a
  network.

  Honest scope (mirrors `autoparts.robotics`/`deviceassembly.robotics`/
  `steelworks.robotics`/`foundrymfg.robotics`): this DOES model a real
  time-stepped `physics-2d` rigid-body trajectory for the tensile-test
  event. It does NOT model: the specimen's own material/stiffness
  (`physics-2d` has no force-deflection/spring model at all, and
  certainly no viscoelastic/hyperelastic rubber constitutive model --
  the specimen's own plastic 'give' is encoded purely as a travel
  DISTANCE, not a stress-strain curve), 3D geometry (2D projection only,
  the same disclosed limit every sibling states), a real load-cell/
  extensometer/DAQ connection, or a real robot controller -- still
  simulation, not control, the same 'policy, not control' boundary
  `kotoba.robotics`'s docstring already establishes. This ns explicitly
  does NOT attempt to model elongation-at-BREAK, creep, or long-term
  compression-set behavior -- only the PEAK-LOAD point of a controlled
  pull, honestly scoped the same way `foundrymfg.robotics` scopes out
  tyre burst/inflation-pressure testing as a genuinely bad fit for this
  rigid-body AABB engine."
  (:require [kotoba.robotics :as robotics]
            [physics-2d :as p2d]))

;; ---------------------------------------------------------------------------
;; Platform shims (mirrors physics-2d's own private sqrt*/abs*/signum* style
;; and `autoparts.robotics`'s/`deviceassembly.robotics`'s/`steelworks.
;; robotics`'s/`foundrymfg.robotics`'s identical shims, keeping this ns
;; portable .cljc -- a raw Math/ceil + Math/abs would be JVM-only and break
;; a ClojureScript consumer).
;; ---------------------------------------------------------------------------

(defn- abs* [x] (if (neg? x) (- x) x))

(defn- ceil* [x]
  #?(:clj  (Math/ceil (double x))
     :cljs (js/Math.ceil x)))

(def mission-actions
  "The three-step test-specimen molding/grip/pull verification mission a
  batch walks through for tensile-test qualification. All :sense/
  :actuate at :none/:low safety -- specimen-molding/grip-seating/
  tensile-pull QA sensing on a stationary test specimen, not a real
  polymerization/compounding-reactor actuation (this actor never
  actuates either -- see `resinmfg.governor`'s permanent
  `reactor-actuate-blocked-violations`/`reactor-control-blocked-
  violations`)."
  [{:step :test-specimen-molding-dimensional-check :kind :sense   :safety :none}
   {:step :grip-seating-check                       :kind :actuate :safety :low}
   {:step :tensile-pull-test                        :kind :actuate :safety :low}])

;; ---------------------- real tensile-test physics constants -----------------

(def ^:const test-speed-mps
  "Controlled jaw pull-rate (m/s) -- see ns docstring: a representative
  dynamic/high-rate tensile-property test speed (the same ISO
  26203-family dynamic tensile-qualification regime `steelworks.
  robotics`'s/`foundrymfg.robotics`'s own `test-speed-mps` disclose),
  not a literal quasi-static crosshead mm/min transcription of ASTM
  D638's/D412's own baseline method (which this single-tick 'boxcar'
  technique cannot honestly render as a meaningful force reading -- see
  docstring)."
  1.8)

(def ^:const travel-to-peak-load-m
  "The specimen's own real plastic-extension distance (m) to reach its
  PEAK tensile load, before further drawing/necking/break -- see ns
  docstring: a representative, disclosed low-single-digit-millimeter
  order of magnitude for a standard ~50 mm gauge-length dogbone/dumbbell
  specimen, conservatively scoped to the more rigid engineering-
  thermoplastic end of this vertical's own polymer-grade mix (a single
  fleet-wide constant, not a per-material-family figure -- see
  docstring for why the far-larger elongation of this actor's own
  synthetic-rubber grades is honestly disclosed but not separately
  modeled)."
  0.006)

(def ^:const initial-grip-slack-m
  "Test-fixture grip-seating/alignment slack (m) the jaw travels before
  the specimen itself begins to bear load -- present only so the
  trajectory captures a real pre-load approach phase, mirroring
  `autoparts.robotics`'s/`steelworks.robotics`'s/`foundrymfg.robotics`'s
  `initial-grip-slack-m`."
  0.0006)

(def ^:const jaw-half-w-m
  "Jaw AABB half-width along the pull axis (m) -- a small, fixed
  test-rig-grip-scale footprint, not a per-batch CAD input (this ns has
  no CAD/BREP pipeline, unlike automotive's envelope-solid bridge)."
  0.008)

(def ^:const jaw-half-h-m
  "Jaw AABB half-height (m), lateral -- half of a standard ASTM D638
  Type I dogbone specimen's narrow (reduced) section width (13 mm; half
  is 6.5 mm), the specimen geometry this ns's own `min-tensile-load-n`
  floor is computed against."
  0.0065)

(def ^:const fixture-half-w-m
  "Specimen-far-end fixture AABB half-width (m) -- static anchor, never
  actually collides with anything (the jaw moves AWAY from it), present
  purely as a real Body2D so the simulated world honestly contains both
  grip ends of the specimen being pulled apart."
  0.008)

(def ^:const fixture-half-h-m 0.0065)

(def ^:const limit-boundary-half-w-m
  "Virtual limit-boundary AABB half-width (m) -- the 'end of tether'
  wall the jaw's approach is reframed against; see ns docstring. This
  body has no physical counterpart at all -- it is a pure math device
  standing in for the specimen running out of extensibility at its peak
  load."
  0.008)

(def ^:const limit-boundary-half-h-m 0.0065)

(def ^:const settle-ticks
  "Extra ticks appended after the jaw is expected to reach the
  limit-boundary, so the trajectory also captures post-contact
  settling. `physics-2d`'s positional correction removes 80% of any
  remaining overlap per tick (`resolve-contact`'s `0.8` factor), so
  residual overlap after `settle-ticks` further ticks is `0.2^settle-
  ticks` of whatever it was at first contact -- 15 ticks converges to
  ~3e-11 (same rationale/constant as `autoparts.robotics`'s/
  `deviceassembly.robotics`'s/`steelworks.robotics`'s/`foundrymfg.
  robotics`'s `settle-ticks`, a genuine physics-2d engine property, not
  re-derived here)."
  15)

(def ^:const min-tensile-load-n
  "Real, disclosed minimum acceptable peak tensile load (N) for a
  standard ASTM D638 Type I dogbone specimen's narrow section -- see ns
  docstring. 200 N (0.2 kN) is placed conservatively BELOW the ~291 N a
  butyl-rubber/IIR (the weakest common grade in this actor's own
  `valid-polymer-grades` set) specimen of this cross-section would
  compute to, so this single fleet-wide floor never falsely fails a
  legitimately weak-but-passing soft-elastomer batch -- a newly-defined
  bound, not a literal transcription of one specific named standard's
  number for one specific grade (the same allowance ADR-2607152000/
  ADR-2607999600/ADR-2607999800 gave `autoparts.robotics/min-proof-
  load-n`/`steelworks.robotics/min-tensile-load-n`/`foundrymfg.robotics/
  min-tensile-load-n`)."
  200.0)

;; ------------------------------ real simulation ------------------------------

(defn run-tensile-test
  "Time-steps a REAL `physics-2d` world for the compounded resin/rubber
  test-specimen tensile test and returns:

    {:trajectory [{:tick :position :velocity} ...]   ; jaw body only
     :sim-peak-decel-mps2 n :sim-tensile-load-n n
     :ticks n :dt n :test-speed-mps n :travel-to-peak-load-m n}

  `specimen-mass-kg` is the batch's own recorded effective participating
  mass (moving jaw + locally-engaged dogbone/dumbbell specimen
  gauge-length material -- a bare number, the same 'effective
  participating mass' framing `autoparts.robotics`'s `:joint-mass-kg`/
  `steelworks.robotics`'s and `foundrymfg.robotics`'s `:coupon-mass-kg`
  use). opts (all optional, for tuning/testing): `:test-speed-mps`,
  `:travel-to-peak-load-m`, `:initial-grip-slack-m`, `:dt` overrides
  (each defaults to this ns's own constant of the same name).

  `:sim-peak-decel-mps2` is the PEAK magnitude of tick-to-tick velocity
  change (along the pull axis) divided by `dt` -- derived from the
  actual simulated velocity trajectory, not invented. `:sim-tensile-
  load-n` is `:sim-peak-decel-mps2 * specimen-mass-kg` (Newtons) -- see
  ns docstring for why mass legitimately scales this reading."
  [specimen-mass-kg & [{v-opt :test-speed-mps travel-opt :travel-to-peak-load-m
                         slack-opt :initial-grip-slack-m dt-opt :dt}]]
  (let [v      (double (or v-opt test-speed-mps))
        travel (double (or travel-opt travel-to-peak-load-m))
        slack  (double (or slack-opt initial-grip-slack-m))
        dt     (double (or dt-opt (/ travel v)))
        fixture-x 0.0
        jaw-x0 (+ fixture-x fixture-half-w-m jaw-half-w-m)
        limit-boundary-x (+ jaw-x0 slack travel jaw-half-w-m limit-boundary-half-w-m)
        approach-m (+ slack travel)
        ticks (long (+ settle-ticks (long (ceil* (/ approach-m (* v dt))))))
        fixture (p2d/make-body {:position [fixture-x 0.0]
                                 :velocity [0.0 0.0]
                                 :mass 0.0
                                 :restitution 0.0
                                 :friction 0.0
                                 :collider (p2d/make-aabb-collider fixture-half-w-m fixture-half-h-m)
                                 :user-data :fixture})
        jaw (p2d/make-body {:position [jaw-x0 0.0]
                             :velocity [v 0.0]
                             :mass (double specimen-mass-kg)
                             :restitution 0.0
                             :friction 0.0
                             :collider (p2d/make-aabb-collider jaw-half-w-m jaw-half-h-m)
                             :user-data :jaw})
        limit-boundary (p2d/make-body {:position [limit-boundary-x 0.0]
                                        :velocity [0.0 0.0]
                                        :mass 0.0
                                        :restitution 0.0
                                        :friction 0.0
                                        :collider (p2d/make-aabb-collider limit-boundary-half-w-m limit-boundary-half-h-m)
                                        :user-data :limit-boundary})
        w0 (p2d/world-new [0.0 0.0])
        [w1 _fixture-id] (p2d/world-add w0 fixture)
        [w2 jaw-id] (p2d/world-add w1 jaw)
        [w3 _limit-id] (p2d/world-add w2 limit-boundary)
        worlds (reductions (fn [w _] (p2d/world-step w dt)) w3 (range ticks))
        trajectory (mapv (fn [tick world]
                            (let [b (nth (:bodies world) jaw-id)]
                              {:tick tick :position (:position b) :velocity (:velocity b)}))
                          (range (count worlds)) worlds)
        vxs (mapv (comp first :velocity) trajectory)
        peak-decel-mps2 (->> (map (fn [va vb] (abs* (/ (- vb va) dt))) vxs (rest vxs))
                              (reduce max 0.0))]
    {:trajectory trajectory
     :sim-peak-decel-mps2 peak-decel-mps2
     :sim-tensile-load-n (* peak-decel-mps2 (double specimen-mass-kg))
     :ticks (count trajectory)
     :dt dt
     :test-speed-mps v
     :travel-to-peak-load-m travel}))

(defn tensile-test-telemetry-for
  "Runs the REAL `run-tensile-test` `physics-2d` simulation for
  `batch`'s own recorded `:specimen-mass-kg` and returns the actual
  simulated trajectory telemetry: `{:sim-tensile-load-n n
  :sim-peak-decel-mps2 n :ticks n :dt n :test-speed-mps n
  :travel-to-peak-load-m n}`. Pure, deterministic -- the same
  `:specimen-mass-kg` always reproduces the same telemetry."
  [batch]
  (select-keys (run-tensile-test (:specimen-mass-kg batch))
               [:sim-tensile-load-n :sim-peak-decel-mps2 :ticks :dt
                :test-speed-mps :travel-to-peak-load-m]))

(defn tensile-test-out-of-tolerance?
  "Pure comparator: does `m`'s own `:sim-tensile-load-n` (already
  present on the map -- typically merged in from `tensile-test-
  telemetry-for`) fall below `min-tensile-load-n`? Mirrors
  `autoparts.robotics/proof-load-out-of-tolerance?`'s/`steelworks.
  robotics/tensile-test-out-of-tolerance?`'s/`foundrymfg.robotics/
  tensile-test-out-of-tolerance?`'s shape exactly. Missing/non-numeric
  telemetry is never silently treated as a violation."
  [{:keys [sim-tensile-load-n]}]
  (and (number? sim-tensile-load-n)
       (< sim-tensile-load-n min-tensile-load-n)))

(defn simulation-out-of-tolerance?
  "Independent ground-truth recheck for the governor: does `batch`'s
  OWN recorded `:specimen-mass-kg`, via a REAL `run-tensile-test`
  `physics-2d` simulation recomputed FRESH right here (never a
  previously stored/self-reported value), yield a peak tensile load
  below `min-tensile-load-n`? Needs no mission run or proposal
  inspection -- like `resinmfg.registry/shipment-weight-exceeded?`, its
  only input is a permanent field already on the batch record. A batch
  with no `:specimen-mass-kg` on file (no tensile-test specimen molded/
  tested yet) never triggers this check -- see ns docstring."
  [{:keys [specimen-mass-kg] :as batch}]
  (and (number? specimen-mass-kg)
       (tensile-test-out-of-tolerance? (merge batch (tensile-test-telemetry-for batch)))))

(defn simulate-tensile-test-cell
  "Run the robot specimen-molding/grip/tensile-pull verification mission
  for `batch-id` (`batch` is the full batch record, incl.
  `:specimen-mass-kg`). Actually runs the REAL engine: `tensile-test-
  telemetry-for` -- the actual `physics-2d`-stepped jaw/fixture/
  limit-boundary collision trajectory (`:sim-tensile-load-n`/
  `:sim-peak-decel-mps2`).

  Returns {:mission .. :actions [{:action .. :proof ..} ..] :passed?
  bool :sim-tensile-load-n n :sim-peak-decel-mps2 n}. Deterministic:
  :passed? is derived from the batch's OWN recorded `:specimen-mass-kg`
  via the REAL simulated trajectory (`tensile-test-out-of-tolerance?`),
  never invented or randomized. This function exists for API parity
  with sibling actors' robotics namespaces and future `resinmfg.
  advisor`/`resinmfg.operation` wiring -- `resinmfg.governor`'s
  independent recheck (`simulation-out-of-tolerance?` above) does NOT
  depend on this mission ever having run; it always recomputes fresh
  from `:specimen-mass-kg` directly."
  [batch-id batch]
  (let [telemetry (tensile-test-telemetry-for batch)
        out-of-range? (tensile-test-out-of-tolerance? (merge batch telemetry))
        reading (if out-of-range? :out-of-tolerance :nominal)
        mission (robotics/mission (str "mission-" batch-id "-tensile-test")
                                   :robot/resin-tensile-test-cell-1
                                   :tensile-load-verification
                                   :boundaries {:station "primary-forms-lab-tensile-test-cell"}
                                   :max-steps (count mission-actions))
        actions (mapv (fn [{:keys [step kind safety]}]
                        (let [a (robotics/action (str (:mission/id mission) "-" (name step))
                                                  (:mission/id mission) kind safety
                                                  :params {:step step :batch-id batch-id})]
                          {:action a
                           :proof (robotics/telemetry-proof (:mission/id mission) step reading
                                                             :provenance :simulated)}))
                      mission-actions)]
    {:mission mission
     :actions actions
     :passed? (not out-of-range?)
     :sim-tensile-load-n (:sim-tensile-load-n telemetry)
     :sim-peak-decel-mps2 (:sim-peak-decel-mps2 telemetry)}))
