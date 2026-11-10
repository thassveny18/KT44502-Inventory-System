StockAtom: A Concurrent Inventory System Demonstrating
Identity and State Separation Using Clojure Atoms
KT44502 Assignment 2 - Option B

Repository: https://github.com/thassveny18/KT44502-Inventory-System

ENVIRONMENT
  Ubuntu (WSL2) on Windows, OpenJDK 17, Leiningen 2.x, Clojure 1.12.0

INSTALL (Ubuntu)
  sudo apt update
  sudo apt install -y openjdk-17-jdk curl git
  curl -O https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
  chmod +x lein && sudo mv lein /usr/local/bin/

RUN
  git clone https://github.com/thassveny18/KT44502-Inventory-System.git
  cd KT44502-Inventory-System
  chmod +x run.sh
  ./run.sh            # environment info + unit tests + simulation

  Or separately:
  lein test           # 10 unit tests
  lein run            # concurrent simulation

EXPECTED OUTPUT
  Reader snapshots printed while 5 buyer and 3 restocker threads run,
  then a verification block ending in:  RESULT : CONSISTENT (PASS)
  (Exact numbers differ each run because thread scheduling is random.)

PROJECT STRUCTURE
  src/inventory_atom/logic.clj    pure functions (no state)
  src/inventory_atom/store.clj    the atom: identity, validator, watch
  src/inventory_atom/workers.clj  buyer / restocker / reader threads
  src/inventory_atom/core.clj     -main: simulation + verification
  test/inventory_atom/            unit + concurrency tests