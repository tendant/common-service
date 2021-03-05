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

(deftest test-timestamp-field
  (testing "Timestamp field"
    (let [entity-1 (create-entity-sync node entity-type {:name "before entity"
                                                         :created-at (java.util.Date.)})
          _ (Thread/sleep 10)
          middle-date (java.util.Date.)
          _ (Thread/sleep 10)
          entity-2 (create-entity-sync node entity-type {:name "after entity"
                                                         :created-at (java.util.Date.)})
          future-date (java.util.Date.)
          result (crux/q (crux/db node)
                         '{:find [?e]
                           :where [[?e :entity/type entity-type]
                                   [?e :created-at ?created-at]]})
          before-result (crux/q (crux/db node)
                                {:find '[?e]
                                 :where '[[?e :entity/type entity-type]
                                          [?e :created-at ?created-at]
                                          [(< ?created-at middle-date)]]
                                 :args [{'middle-date middle-date}]
                                 :full-results? true})
          after-result (crux/q (crux/db node)
                                {:find '[?e]
                                 :where '[[?e :entity/type entity-type]
                                          [?e :created-at ?created-at]
                                          [(> ?created-at middle-date)]]
                                 :args [{'middle-date middle-date}]
                                 :full-results? true})
          all-result (crux/q (crux/db node)
                                {:find '[?e]
                                 :where '[[?e :entity/type entity-type]
                                          [?e :created-at ?created-at]
                                          [(< ?created-at future-date)]]
                                 :args [{'future-date future-date}]
                                 :full-results? true})
          ]
      (is before-result)
      (is (= 1 (count before-result)))
      (is (= "before entity"
             (:name (ffirst before-result))))
      (is after-result)
      (is (= 1 (count after-result)))
      (is (= "after entity"
             (:name (ffirst after-result))))
      (is all-result)
      (is (= 2 (count all-result))))))