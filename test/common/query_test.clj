(ns common.query-test
  (:require [clojure.test :refer :all]
            [common.crux-svc :refer :all]
            [crux.api :as crux]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(def node (crux.api/start-node {}))

(def entity-type :entity/query-test)

(defn string-n [length]
  (gen/fmap #(apply str %)
            (gen/vector gen/char-alpha length)))

(deftest test-added-filed
  (testing "added field"
    (let [entity-1 (create-entity-sync node entity-type {:name "no projectid"})
          entity-2 (create-entity-sync node entity-type {:name "with nil projectid"
                                                         :project-id  nil})
          entity-3 (create-entity-sync node entity-type {:name "with nil projectid"
                                                         :project-id  "project-id-1"})
          result (crux/q (crux/db node)
                         '{:find [?e]
                           :where [[?e :entity/type entity-type]]})
          result-with-project (crux/q (crux/db node)
                                      {:find '[?e]
                                       :where '[[?e :entity/type entity-type]
                                                [?e :project-id project-id]]
                                        :args [{'project-id "project-id-1"}]})]
      (is result)
      (is (= 3
             (count result)))
      (is (= 1
             (count result-with-project))))))