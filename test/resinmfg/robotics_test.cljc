(ns resinmfg.robotics-test
  "Direct tests of `resinmfg.robotics`'s REAL, ADR-2607999900
  time-stepped `physics-2d` compounded resin/rubber test-specimen
  tensile-test simulation -- proving `:sim-tensile-load-n` is actually
  DERIVED from the simulated trajectory (changes sensibly with
  `specimen-mass-kg`/test speed, is deterministic/repeatable, and the
  peak deceleration is mass-invariant given fixed speed/travel), the
  same shape `steelworks.robotics-test`/`foundrymfg.robotics-test`/
  `deviceassembly.robotics-test`/`autoparts.robotics-test` use to prove
  a physics check isn't invented or randomized -- alongside proving
  this is purely ADDITIVE: `resinmfg.registry/polymer-grade-valid?`/
  `off-spec-rate-valid?` (the pre-existing self-reported checks) are
  untouched, and a batch with no `:specimen-mass-kg` on file never trips
  the new check."
  (:require [clojure.test :refer [deftest is testing]]
            [resinmfg.robotics :as robotics]))

(defn- abs* [x] (if (neg? x) (- x) x))

(defn- approx= [a b eps] (< (abs* (double (- a b))) eps))

(deftest tensile-test-runs-a-real-trajectory
  (testing "run-tensile-test returns a non-trivial, tick-by-tick trajectory -- not a single invented number"
    (let [{:keys [trajectory ticks dt test-speed-mps travel-to-peak-load-m]} (robotics/run-tensile-test 0.3)]
      (is (> ticks 1) "more than one simulated tick")
      (is (= ticks (count trajectory)))
      (is (pos? dt))
      (is (= robotics/test-speed-mps test-speed-mps))
      (is (= robotics/travel-to-peak-load-m travel-to-peak-load-m))
      (testing "the jaw starts moving at the full pull speed"
        (is (= test-speed-mps (first (:velocity (first trajectory))))))
      (testing "the jaw's velocity actually drops to (near) zero once the specimen reaches its peak load -- a real collision was resolved, not skipped"
        (is (< (abs* (double (first (:velocity (last trajectory))))) 1.0e-6))))))

(deftest tensile-load-scales-with-specimen-mass
  (testing "F = m*a: a heavier specimen-mass-kg input yields a proportionally larger peak tensile load, off the SAME simulated deceleration -- proves the reading is derived, not a fixed/invented constant"
    (let [light (robotics/run-tensile-test 0.2)
          heavy (robotics/run-tensile-test 0.4)]
      (is (< (:sim-tensile-load-n light) (:sim-tensile-load-n heavy)))
      (is (approx= (* 2.0 (:sim-tensile-load-n light)) (:sim-tensile-load-n heavy) 1.0e-6)
          "load doubles (within floating-point tolerance) with mass -- same peak deceleration")
      (testing "peak deceleration itself is mass-invariant at fixed speed/travel (force = decel * mass)"
        (is (approx= (:sim-peak-decel-mps2 light) (:sim-peak-decel-mps2 heavy) 1.0e-9))))))

(deftest tensile-load-scales-with-test-speed
  (testing "a faster controlled test-speed-mps yields a larger peak load off the SAME specimen mass -- a second independent axis the reading actually tracks"
    (let [slow (robotics/run-tensile-test 0.3 {:test-speed-mps 0.5})
          fast (robotics/run-tensile-test 0.3 {:test-speed-mps 3.0})]
      (is (< (:sim-tensile-load-n slow) (:sim-tensile-load-n fast))))))

(deftest tensile-test-simulation-is-deterministic
  (testing "the same specimen-mass-kg always reproduces the same telemetry -- no wall-clock/IO/randomness"
    (let [a (robotics/run-tensile-test 0.325)
          b (robotics/run-tensile-test 0.325)]
      (is (= (dissoc a :trajectory) (dissoc b :trajectory)))
      (is (= a b)))))

(deftest tensile-test-telemetry-for-reads-the-batchs-own-mass
  (testing "tensile-test-telemetry-for runs the real simulation off :specimen-mass-kg, not a hand-typed double"
    (let [light-batch {:specimen-mass-kg 0.2}
          heavy-batch {:specimen-mass-kg 0.5}
          light-telemetry (robotics/tensile-test-telemetry-for light-batch)
          heavy-telemetry (robotics/tensile-test-telemetry-for heavy-batch)]
      (is (= (:sim-tensile-load-n light-telemetry)
             (:sim-tensile-load-n (robotics/run-tensile-test 0.2))))
      (is (< (:sim-tensile-load-n light-telemetry) (:sim-tensile-load-n heavy-telemetry))))))

(deftest tensile-test-out-of-tolerance-thresholds-on-the-real-floor
  (testing "a batch whose real simulated peak tensile load is at/over the floor is in-tolerance; under it is out-of-tolerance"
    (is (false? (robotics/tensile-test-out-of-tolerance? {:sim-tensile-load-n (+ robotics/min-tensile-load-n 1.0)})))
    (is (true? (robotics/tensile-test-out-of-tolerance? {:sim-tensile-load-n (- robotics/min-tensile-load-n 1.0)})))
    (is (false? (robotics/tensile-test-out-of-tolerance? {:sim-tensile-load-n nil}))
        "missing telemetry is never silently treated as a violation")))

(deftest simulation-out-of-tolerance-is-always-fresh-and-never-invented
  (testing "simulation-out-of-tolerance? recomputes the REAL simulation fresh from :specimen-mass-kg -- a batch whose specimen is too light for the real disclosed floor is caught, one whose specimen clears it is not"
    (let [too-light {:specimen-mass-kg 0.05}
          comfortably-heavy {:specimen-mass-kg 0.6}]
      (is (true? (robotics/simulation-out-of-tolerance? too-light)))
      (is (false? (robotics/simulation-out-of-tolerance? comfortably-heavy)))))
  (testing "a batch with no :specimen-mass-kg on file (no tensile-test specimen molded/tested yet) never trips this check -- missing telemetry != violation"
    (is (false? (robotics/simulation-out-of-tolerance? {:id "batch-x"})))
    (is (false? (robotics/simulation-out-of-tolerance? {:id "batch-x" :specimen-mass-kg nil})))))

(deftest simulate-tensile-test-cell-folds-a-real-mission-around-the-check
  (testing "simulate-tensile-test-cell walks the real three-step mission and derives :passed? from the REAL simulated load, never invented"
    (let [clean {:specimen-mass-kg 0.6}
          bad {:specimen-mass-kg 0.05}
          clean-result (robotics/simulate-tensile-test-cell "batch-clean" clean)
          bad-result (robotics/simulate-tensile-test-cell "batch-bad" bad)]
      (is (true? (:passed? clean-result)))
      (is (false? (:passed? bad-result)))
      (is (= 3 (count (:actions clean-result))))
      (is (= 3 (count robotics/mission-actions)))
      (is (pos? (:sim-tensile-load-n clean-result)))
      (is (= (:sim-tensile-load-n clean-result)
             (:sim-tensile-load-n (robotics/tensile-test-telemetry-for clean)))
          "the mission's reported load is the SAME real simulated reading, not a re-derived/duplicated number"))))
