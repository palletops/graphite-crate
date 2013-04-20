;;; Pallet project configuration file

(require
 '[pallet.crate.graphite-test :refer [graphite-test-spec]]
 '[pallet.crates.test-nodes :refer [node-specs]])

(defproject graphite-crate
  :provider node-specs                  ; supported pallet nodes
  :groups [graphite-test-spec])
