(ns common.crux-svc-test
  (:require [clojure.test :refer :all]
            [common.crux-svc :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(def node (crux.api/start-node {}))

(def prop-create-entity-sync
  (prop/for-all [params (gen/not-empty (gen/map gen/keyword gen/string-alphanumeric))]
    (is (create-entity-sync node :entity/my-test params))))

(deftest test-create-entity-sync
  (testing "Create entity sync"
    (tc/quick-check 100 prop-create-entity-sync)))