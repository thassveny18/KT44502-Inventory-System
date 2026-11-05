(ns inventory-atom.core-test
  (:require [clojure.test :refer [deftest is]]
            [inventory-atom.core :as core]
            [inventory-atom.logic :as logic]
            [inventory-atom.store :as store]
            [inventory-atom.workers :as workers]))

(deftest concurrent-consistency-test
  (store/reset-inventory!)
  (let [initial (logic/total-stock @store/inventory)
        results (mapv deref (workers/start-workers 8 4 300))
        report  (core/verify initial (core/summarise results) @store/inventory)]
    (is (:consistent? report))
    (is (= (:expected report) (:actual report)))))