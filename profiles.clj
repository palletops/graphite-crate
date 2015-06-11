{:no-checkouts {:checkout-deps-shares ^{:replace true} []},
 :dev {:dependencies [[com.palletops/pallet "0.8.0" :classifier "tests"]
                      [com.palletops/crates "RELEASE"]
                      [ch.qos.logback/logback-classic "1.0.9"]],
       :plugins [[lein-pallet-release "RELEASE"]]}}
