(ns inventory-atom.workers
  "Concurrent workers. Each worker runs in its own thread (a future).

   Workers never share mutable counters. Each one returns its results
   as an immutable map through its future, and -main combines them."
  (:require [inventory-atom.logic :as logic]
            [inventory-atom.store :as store]))

(def products (vec (keys store/initial-inventory)))

(defn- random-order
  "Randomness lives here, OUTSIDE swap!, so the functions given to swap! stay pure."
  []
  {:pid (rand-nth products) :qty (inc (rand-int 5))})

(defn- pause!
  "Small random delay so the threads interleave visibly."
  []
  (Thread/sleep (long (rand-int 3))))

(defn buyer
  "Starts a buyer thread that makes `n-ops` purchase attempts.
   Returns a future that yields {:bought units :ok n :rejected n}."
  [id n-ops]
  (future
    (reduce (fn [acc _]
              (let [{:keys [pid qty]} (random-order)]
                (pause!)
                (if (store/attempt-buy! pid qty)
                  (-> acc (update :bought + qty) (update :ok inc))
                  (update acc :rejected inc))))
            {:worker (str "buyer-" id) :bought 0 :ok 0 :rejected 0}
            (range n-ops))))

(defn restocker
  "Starts a restocker thread that makes `n-ops` restocks.
   Returns a future that yields {:restocked units :ok n}."
  [id n-ops]
  (future
    (reduce (fn [acc _]
              (let [{:keys [pid qty]} (random-order)]
                (pause!)
                (-> acc
                    (update :restocked + (store/do-restock! pid qty))
                    (update :ok inc))))
            {:worker (str "restocker-" id) :restocked 0 :ok 0}
            (range n-ops))))

(defn reader
  "Starts a reader thread that prints snapshots while `running?` is true.
   It dereferences the atom ONCE per pass and computes everything from that
   single snapshot, so the total and validity always match the map printed.
   Returns a future that yields the number of snapshots taken."
  [running? interval-ms]
  (future
    (loop [n 0]
      (if @running?
        (let [snapshot @store/inventory]          ; one consistent snapshot
          (store/log! (logic/format-snapshot n snapshot))
          (Thread/sleep (long interval-ms))
          (recur (inc n)))
        n))))

(defn start-workers
  "Launches all buyers and restockers (higher-order: map over ids).
   doall forces the lazy seq so every thread starts now."
  [n-buyers n-restockers n-ops]
  (doall
   (concat (map #(buyer % n-ops) (range 1 (inc n-buyers)))
           (map #(restocker % n-ops) (range 1 (inc n-restockers))))))