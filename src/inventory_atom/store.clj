(ns inventory-atom.store
  "The IDENTITY of the inventory.
  Identity vs state:
  inventory (the atom) is the identity: one stable reference that lives
  for the whole program.
  The map it holds is the state: an immutable snapshot. Nobody edits it.
  Changing the inventory means moving the atom to a NEW snapshot with swap!.
  swap! uses compare-and-set: if another thread changed the atom first,
  it retries our (pure) function on the latest value. No locks are needed.
  Reading with @inventory gives one complete, consistent snapshot."
  (:require [inventory-atom.logic :as logic]))

(def initial-inventory
  {:apple 50 :banana 30 :cherry 20 :durian 10})

(def inventory
  "The single atom. The validator rejects any state with negative stock,
  so an invalid snapshot can never become the current value."
  (atom initial-inventory :validator logic/valid-inventory?))

(defn log!
  "Thread-safe println so lines from different threads do not interleave."
  [& args]
  (locking *out*
    (apply println args)))

(defn reset-inventory!
  "Puts the atom back to the initial state (used by -main and tests)."
  []
  (reset! inventory initial-inventory))

(defn attempt-buy!
  "Tries to buy qty of pid. Returns true if the purchase succeeded.
  swap-vals! returns [old new], so we detect success by comparing them,
  with no side effects inside the swap."
  [pid qty]
  (let [[old new] (swap-vals! inventory logic/buy pid qty)]
    (not= (get old pid) (get new pid))))

(defn do-restock!
  "Adds qty of pid. Restocking always succeeds. Returns qty."
  [pid qty]
  (swap! inventory logic/restock pid qty)
  qty)

(def audit
  "Statistics collected by the watch: how many state transitions happened
  and how many times a product sold out."
  (atom {:transitions 0 :sold-out 0}))

(defn add-audit-watch!
  "A watch receives BOTH the old and the new state on every change,
  which shows that each update is a transition between two immutable values.
  Watches run after the change is committed, so side effects are allowed here."
  []
  (reset! audit {:transitions 0 :sold-out 0})
  (add-watch inventory :audit
    (fn [_key _ref old new]
      ;; a rejected purchase "swaps" to an equal value, so skip it
      (when (not= old new)
        (let [sold-out (count (filter (fn [pid] 
                                        (and (pos? (get old pid 0))
                                             (zero? (get new pid))))
                                      (keys new)))]
          (swap! audit #(-> % 
                            (update :transitions inc)
                            (update :sold-out + sold-out))))))))

(defn remove-audit-watch! []
  (remove-watch inventory :audit))