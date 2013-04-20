(defproject com.palletops/graphite-crate "0.8.0-alpha.1"
  :description "Crate for graphite installation"
  :url "http://github.com/palletops/graphite-crate"
  :license {:name "All rights reserved"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.palletops/pallet "0.8.0-beta.7"]]
  :resource {:resource-paths ["doc-src"]
             :target-path "target/classes/pallet_crate/collectd_crate/"
             :includes [#"doc-src/USAGE.*"]}
  :prep-tasks ["resource" "crate-doc"])
