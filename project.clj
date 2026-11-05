
(defproject inventory-atom "0.1.0"
  :description "StockAtom: A Concurrent Inventory System Demonstrating Identity and State Separation Using Clojure Atoms (KT44502 Assignment 2, Option B)"
  :url "https://github.com/thassveny18/KT44502-Inventory-System"
  :dependencies [[org.clojure/clojure "1.12.0"]]
  :main ^:skip-aot inventory-atom.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})