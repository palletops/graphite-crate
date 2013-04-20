{:dev
 {:dependencies [[com.palletops/pallet "0.8.0-beta.7" :classifier "tests"]
                 [com.palletops/crates "0.1.0"]
                 [ch.qos.logback/logback-classic "1.0.9"]]
  :plugins [[com.palletops/pallet-lein "0.6.0-beta.7"]
            [codox/codox.leiningen "0.6.4"]
            [lein-marginalia "0.7.1"]
            [lein-resource "0.3.2"]]
  :aliases {"live-test-up"
            ["pallet" "up"
             "--phases" "install,configure,test"
             "--selector" "live-test"]
            "live-test-down" ["pallet" "down" "--selector" "live-test"]
            "live-test" ["do" "live-test-up," "live-test-down"]}}
 :doc {:dependencies [[com.palletops/pallet-codox "0.1.0"]]
       :plugins [[codox/codox.leiningen "0.6.4"]
                 [lein-marginalia "0.7.1"]]
       :codox {:writer codox-md.writer/write-docs
               :output-dir "doc/0.8/api"
               :src-dir-uri "https://github.com/pallet/graphite-crate/blob/develop"
               :src-linenum-anchor-prefix "L"}
       :aliases {"marg" ["marg" "-d" "doc/0.8/annotated"]
                 "codox" ["doc"]
                 "doc" ["do" "codox," "marg"]}}
 :no-checkouts {:checkout-deps-shares ^:replace []} ; disable checkouts
 :release
 {:plugins [[lein-set-version "0.3.0"]]
  :set-version
  {:updates [{:path "README.md"
              :no-snapshot true
              :search-regex
              #"com.palletops/graphite-crate \"\d+\.\d+\.\d+\""}]}}}
