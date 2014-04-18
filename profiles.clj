{:no-checkouts {:checkout-deps-shares ^{:replace true} []},
 :dev {:dependencies [[com.palletops/pallet "0.8.0-RC.9" :classifier "tests"]
                      [com.palletops/crates "RELEASE"]
                      [ch.qos.logback/logback-classic "1.0.9"]],
       :plugins [[lein-pallet-release "0.1.6"]
                 [com.palletops/pallet-lein "0.6.0-beta.7"]
                 [lein-resource "0.3.2"]],
       :pallet-release {:url "https://pbors:${GH_TOKEN}@github.com/palletops/graphite-crate.git",
                        :branch "master"}}}
