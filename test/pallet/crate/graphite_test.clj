(ns pallet.crate.graphite-test
  (:use
   clojure.test
   pallet.crate.graphite
   [pallet.actions :only [package-manager]]
   [pallet.algo.fsmop :only [complete?]]
   [pallet.api :only [lift plan-fn group-spec server-spec]]
   [pallet.crate.automated-admin-user :only [automated-admin-user]]
   [pallet.live-test :only [images test-nodes]]))

(deftest ^:live-test live-test
  (let [settings {}]
    (doseq [image (images)]
      (test-nodes
       [compute node-map node-types [:install :configure]]
       {:graphite
        (group-spec
         "graphite"
         :image image
         :count 1
         :extends [(graphite settings)]
         :phases {:bootstrap (plan-fn (automated-admin-user))
                  :install (plan-fn (package-manager :update))})}))))
