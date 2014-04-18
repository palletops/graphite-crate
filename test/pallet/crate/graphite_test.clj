(ns pallet.crate.graphite-test
  (:require
   [clojure.test :refer :all]
   [pallet.crate.graphite :as graphite]
   [pallet.crate.network-service :refer [wait-for-port-listen]]
   [pallet.actions :refer [package-manager]]
   [pallet.algo.fsmop :refer [complete?]]
   [pallet.api :refer [lift plan-fn group-spec server-spec]]
   [pallet.crate.automated-admin-user :refer [automated-admin-user]]
   #_[pallet.live-test :refer [images test-nodes]]))

(def graphite-test-spec
  (group-spec "graphite"
    :count 1
    :extends [(graphite/server-spec {})]
    :phases {:bootstrap (plan-fn
                          (automated-admin-user)
                          (package-manager :update))
             :test (plan-fn
                     (wait-for-port-listen 8080))}
    :roles #{:live-test}))

#_(deftest ^:live-test live-test

  (doseq [image (images)]
    (test-nodes
        [compute node-map node-types [:install :configure]]
      {:graphite
       (assoc graphite-test-spec :image image)})))
