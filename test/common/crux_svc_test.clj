(ns common.crux-svc-test
  (:require [clojure.test :refer :all]
            [common.crux-svc :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(def node (crux.api/start-node {}))

(def entity-type :entity/my-test)

(def prop-params (gen/not-empty (gen/map gen/keyword gen/string-alphanumeric)))

(def prop-create-entity-sync
  (prop/for-all [params prop-params]
    (let [result (create-entity-sync node :entity/my-test params)]
      (is result)
      (is (:crux.db/id result)))))

(deftest test-create-entity-sync
  (testing "Create entity sync"
    (tc/quick-check 100 prop-create-entity-sync)))

(def prop-create-entity
  (prop/for-all [params prop-params]
    (is (create-entity node :entity/my-test params))))

(deftest test-create-entity
  (testing "Create entity"
    (tc/quick-check 100 prop-create-entity)))

(def prop-retrieve-entity
  (prop/for-all [params prop-params]
    (let [entity (create-entity-sync node :entity/my-test params)
          entity-id (:crux.db/id entity)
          result (retrieve-entity-by-id node entity-id)]
      (is (= (assoc params :entity/type :entity/my-test)
             (dissoc result :crux.db/id))))))

(deftest test-retrieve-entity
  (testing "Retrieve entity"
    (tc/quick-check 100 prop-retrieve-entity)))

(def prop-find-entity-by-id-and-type
  (prop/for-all [params prop-params]
    (let [entity (create-entity-sync node :entity/my-test params)
          entity-id (:crux.db/id entity)
          result (find-entity-by-id-and-type node entity-id :entity/my-test)]
      (is result)
      (is (= entity-id (:crux.db/id result))))))

(deftest test-find-entity-by-id-and-type
  (testing "Find entity by id and type"
    (tc/quick-check 100 prop-find-entity-by-id-and-type)))

(def prop-find-entities-by-attr
  (prop/for-all [params prop-params
                 attr-name gen/keyword
                 attr-value (gen/not-empty gen/string-alphanumeric)]
    (let [entity-params (assoc params attr-name attr-value)
          entity (create-entity-sync node entity-type entity-params)
          entity-id (:crux.db/id entity)
          results (find-entities-by-attr node entity-type attr-name attr-value)]
      (println "results:" results)
      (is (not-empty results))
      (is (= entity-id
             (:crux.db/id (first results))))
      (is (= 1 (count results))))))

(deftest test-find-entities-by-attr
  (testing "Find entities by attr"
    (tc/quick-check 100 prop-find-entities-by-attr)))

(def prop-find-entities-by-attrs
  (prop/for-all [params prop-params
                 attr-name-a gen/keyword
                 attr-value-a (gen/not-empty gen/string-alphanumeric)
                 attr-name-b gen/keyword
                 attr-value-b (gen/not-empty gen/string-alphanumeric)]
    (let [entity-params (assoc params
                               attr-name-a attr-value-a
                               attr-name-b attr-value-b)
          entity (create-entity-sync node entity-type entity-params)
          entity-id (:crux.db/id entity)
          results (find-entities-by-attrs node entity-type {attr-name-a attr-value-a
                                                            attr-name-b attr-value-b})]
      (println "results:" results)
      (is (not-empty results))
      (is (= entity-id
             (:crux.db/id (first results))))
      (is (= 1 (count results))))))

(def prop-find-entities-by-attrs-2
  (prop/for-all [params-1 prop-params
                 params-2 prop-params
                 attr-name-a gen/keyword
                 attr-value-a (gen/not-empty gen/string-alphanumeric)
                 attr-name-b gen/keyword
                 attr-value-b (gen/not-empty gen/string-alphanumeric)]
    (let [entity-params-1 (assoc params-1
                                 attr-name-a attr-value-a
                                 attr-name-b attr-value-b)
          entity-params-2 (assoc params-2
                                 attr-name-a attr-value-a
                                 attr-name-b attr-value-b)
          entity-1 (create-entity-sync node entity-type entity-params-1)
          entity-1-id (:crux.db/id entity-1)
          entity-2 (create-entity-sync node entity-type entity-params-2)
          entity-2-id (:crux.db/id entity-2)
          results (find-entities-by-attrs node entity-type {attr-name-a attr-value-a
                                                            attr-name-b attr-value-b})]
      (println "results:" results)
      (is (not-empty results))
      (is (= 2 (count results)))
      (is #{entity-1-id entity-2-id}
          (set (map :crux.db/id results))))))

(deftest test-find-entities-by-attr
  (testing "Find entities by attr"
    (tc/quick-check 100 prop-find-entities-by-attrs)))
(deftest test-find-entities-by-ids
  (testing "Find Entities by ids"
    (let [entities (->> (range 10)
                        (map #(create-entity-sync node entity-type {:name (format "test find-entities-by-ids %s" %)})))
          ids (->> (map :crux.db/id entities)
                   (into []))
          find-one-entity (find-entities-by-ids node [(first ids)])
          find-all-entities (find-entities-by-ids node ids)]
      (is (= 1 (count find-one-entity)))
      (is (= (first ids) (:crux.db/id (first find-one-entity))))
      (is (= 10 (count find-all-entities))))))


(deftest test-find-entities-by-ids-and-type
  (testing "Find Entities by ids and type"
    (let [entities (->> (range 10)
                        (map #(create-entity-sync node entity-type {:name (format "test find-entities-by-ids-and-type %s" %)})))
          ids (->> (map :crux.db/id entities)
                   (into []))
          find-one-entity (find-entities-by-ids-and-type node [(first ids)] entity-type)
          find-all-entities (find-entities-by-ids-and-type node ids entity-type)]
      (is (= 1 (count find-one-entity)))
      (is (= (first ids) (:crux.db/id (first find-one-entity))))
      (is (= 10 (count find-all-entities))))))