(ns inventory-atom.store-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [inventory-atom.store :as store]))

(use-fixtures :each (fn [t] (store/reset-inventory!) (t)))

(deftest attempt-buy-test
  (is (true? (store/attempt-buy! :apple 5)))
  (is (= 45 (:apple @store/inventory)))
  (is (false? (store/attempt-buy! :durian 999)))
  (is (= 10 (:durian @store/inventory))))

(deftest restock-test
  (store/do-restock! :cherry 7)
  (is (= 27 (:cherry @store/inventory))))

(deftest validator-test
  (testing "the validator refuses a negative-stock state"
    (is (thrown? IllegalStateException
                 (swap! store/inventory assoc :apple -1)))
    (is (= 50 (:apple @store/inventory)))))

(deftest snapshot-test
  (testing "a dereferenced snapshot does not change after later updates"
    (let [snap @store/inventory]
      (store/do-restock! :apple 100)
      (is (= 50 (:apple snap)))
      (is (= 150 (:apple @store/inventory))))))