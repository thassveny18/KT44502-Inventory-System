(ns inventory-atom.logic
  "PURE business logic for the inventory.

   Rules for this namespace:
   - No atoms, no threads, no printing, no randomness.
   - Every function takes an immutable inventory map and returns a NEW value.
   - Because these functions are pure, they are safe to pass to swap!,
     which may call them more than once if another thread wins the race.")

(defn can-buy?
  "True if `inv` has at least `qty` units of product `pid`."
  [inv pid qty]
  (>= (get inv pid 0) qty))

(defn buy
  "Returns a NEW inventory with `qty` units of `pid` removed.
   If there is not enough stock, returns `inv` unchanged (the purchase is rejected).
   The original map is never modified (immutability)."
  [inv pid qty]
  (if (can-buy? inv pid qty)
    (update inv pid - qty)
    inv))

(defn restock
  "Returns a NEW inventory with `qty` units of `pid` added.
   fnil lets us restock a product that is not in the map yet."
  [inv pid qty]
  (update inv pid (fnil + 0) qty))

(defn total-stock
  "Total units across all products (higher-order function: reduce)."
  [inv]
  (reduce + 0 (vals inv)))

(defn low-stock
  "Sorted vector of product ids whose quantity is below `threshold`
   (higher-order functions: filter + map)."
  [inv threshold]
  (->> inv
       (filter (fn [[_ qty]] (< qty threshold)))
       (map key)
       sort
       vec))

(defn valid-inventory?
  "Invariant: every quantity is a non-negative integer.
   Used as the atom's validator in inventory-atom.store."
  [inv]
  (and (map? inv)
       (every? (fn [qty] (and (integer? qty) (not (neg? qty))))
               (vals inv))))

(defn format-snapshot
  "Pure: turns one snapshot into a printable line. Printing happens elsewhere."
  [n inv]
  (str "[reader #" n "] " (into (sorted-map) inv)
       " total=" (total-stock inv)
       " low=" (low-stock inv 5)
       " valid=" (valid-inventory? inv)))