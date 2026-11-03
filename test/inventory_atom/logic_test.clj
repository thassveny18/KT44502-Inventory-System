(ns inventory-atom.logic-test
  (:require [clojure.test :refer [deftest is testing]]
            [inventory-atom.logic :as logic]))

(def sample {:apple 10 :banana 3})

(deftest buy-test
  (testing "buy reduces stock when enough is available"
    (is (= {:apple 7 :banana 3} (logic/buy sample :apple 3))))
  (testing "buy with insufficient stock returns the same map"
    (is (= sample (logic/buy sample :banana 5)))))

(deftest restock-test
  (is (= {:apple 15 :banana 3} (logic/restock sample :apple 5)))
  (is (= 4 (:cherry (logic/restock sample :cherry 4)))))

(deftest immutability-test
  (testing "the original map is never changed"
    (logic/buy sample :apple 3)
    (logic/restock sample :banana 100)
    (is (= {:apple 10 :banana 3} sample))))

(deftest referential-transparency-test
  (testing "same input always gives the same output"
    (is (= (logic/buy sample :apple 2) (logic/buy sample :apple 2)))
    (is (= (logic/total-stock sample) (logic/total-stock sample) 13))))

(deftest helpers-test
  (is (= [:banana] (logic/low-stock sample 5)))
  (is (true? (logic/valid-inventory? sample)))
  (is (false? (logic/valid-inventory? {:apple -1}))))