(ns inventory-atom.core
  "Entry point: runs the simulation and verifies the final state."
  (:require [inventory-atom.logic :as logic]
            [inventory-atom.store :as store]
            [inventory-atom.workers :as workers])
  (:gen-class))

(def config {:buyers 5 :restockers 3 :ops-per-worker 200 :reader-interval-ms 20})

(defn summarise
  "Pure: combines every worker's result map into one total (reduce + merge-with)."
  [results]
  (reduce (fn [acc r] (merge-with + acc (select-keys r [:bought :restocked :ok :rejected])))
          {:bought 0 :restocked 0 :ok 0 :rejected 0}
          results))

(defn verify
  "Pure: checks that the final state is exactly what the successful operations predict."
  [initial-total {:keys [bought restocked]} final-inv]
  (let [expected (- (+ initial-total restocked) bought)
        actual   (logic/total-stock final-inv)]
    {:expected     expected
     :actual       actual
     :non-negative (logic/valid-inventory? final-inv)
     :consistent?  (and (= expected actual) (logic/valid-inventory? final-inv))}))

(defn -main [& _args]
  (store/reset-inventory!)
  (store/add-audit-watch!)
  (let [{:keys [buyers restockers ops-per-worker reader-interval-ms]} config
        initial  @store/inventory
        _        (store/log! "=== StockAtom: Concurrent Inventory System (Clojure atom) ===")
        _        (store/log! "Initial state:" (into (sorted-map) initial)
                             "total =" (logic/total-stock initial))
        running? (atom true)
        rdr      (workers/reader running? reader-interval-ms)
        futures  (workers/start-workers buyers restockers ops-per-worker)
        results  (mapv deref futures)        ; wait for every worker
        _        (reset! running? false)
        n-snaps  @rdr
        final    @store/inventory
        summary  (summarise results)
        report   (verify (logic/total-stock initial) summary final)]
    (store/remove-audit-watch!)
    (store/log! "\n=== Worker results ===")
    (doseq [r results] (store/log! r))
    (store/log! "\n=== Verification ===")
    (store/log! "Threads            :" (+ buyers restockers) "workers + 1 reader")
    (store/log! "Snapshots read     :" n-snaps)
    (store/log! "Initial total      :" (logic/total-stock initial))
    (store/log! "Units restocked    :" (str "+" (:restocked summary)))
    (store/log! "Units bought       :" (str "-" (:bought summary)))
    (store/log! "Rejected purchases :" (:rejected summary))
    (store/log! "Watch: transitions :" (:transitions @store/audit)
                "| sold-out events:" (:sold-out @store/audit))
    (store/log! "Expected final     :" (:expected report))
    (store/log! "Actual final       :" (:actual report) (into (sorted-map) final))
    (store/log! "All non-negative   :" (:non-negative report))
    (store/log! "RESULT             :" (if (:consistent? report) "CONSISTENT (PASS)" "INCONSISTENT (FAIL)"))
    (shutdown-agents)))