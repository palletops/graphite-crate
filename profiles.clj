{:dev
 {:dependencies [[com.palletops/pallet "0.8.0-RC.9" :classifier "tests"]
                 [com.palletops/crates "RELEASE"]
                 [ch.qos.logback/logback-classic "1.0.9"]]
  :plugins [[com.palletops/pallet-lein "0.6.0-beta.7"]
            [lein-resource "0.3.2"]]
  :aliases {"live-test-up"
            ["pallet" "up"
             "--phases" "install,configure,test"
             "--selector" "live-test"]
            "live-test-down" ["pallet" "down" "--selector" "live-test"]
            "live-test" ["do" "live-test-up," "live-test-down"]}}
 :no-checkouts {:checkout-deps-shares ^:replace []} ; disable checkouts
 }
