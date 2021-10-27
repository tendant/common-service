(ns common.query-test
  (:require [clojure.test :refer :all]
            [common.crux-svc :refer :all]
            [crux.api :as crux]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(def node (crux.api/start-node {}))
;; (def node (crux/new-api-client (str "http://localhost:" 3000)))

(defn string-n [length]
  (gen/fmap #(apply str %)
            (gen/vector gen/char-alpha length)))

(deftest test-added-field
  (testing "added field"
    (let [entity-type :entity/added-field-2
          entity-1 (create-entity-sync node entity-type {:name "no projectid"})
          entity-2 (create-entity-sync node entity-type {:name "with nil projectid"
                                                         :project-id  nil})
          entity-3 (create-entity-sync node entity-type {:name "with nil projectid"
                                                         :project-id  "project-id-1"})
          result (crux/q (crux/db node)
                         {:find '[?e]
                          :where '[[?e :entity/type entity-type]]
                          :args [{'entity-type entity-type}]})
          result-with-project (crux/q (crux/db node)
                                      {:find '[?e]
                                       :where '[[?e :entity/type entity-type]
                                                [?e :project-id project-id]]
                                        :args [{'project-id "project-id-1"
                                                'entity-type entity-type}]})
          result-without-project (crux/q (crux/db node)
                                         {:find '[?e]
                                          :where '[[?e :entity/type entity-type]
                                                   [(get-attr ?e :project-id nil) [?project-id]]
                                                   [(nil? ?project-id)]]
                                          :args [{'entity-type entity-type}]})]
      (is result)
      (is (= 3
             (count result)))
      (is (= 1
             (count result-with-project)))
      (is result-without-project)
      (is (= 2
             (count result-without-project)))
      (is (= #{[(:crux.db/id entity-1)]
               [(:crux.db/id entity-2)]}
             result-without-project)))))

(deftest test-timestamp-field
  (testing "Timestamp field"
    (let [entity-type :entity/timestamp-field
          entity-1 (create-entity-sync node entity-type {:name "before entity"
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
                                {:find '[(pull ?e [*])]
                                 :where '[[?e :entity/type entity-type]
                                          [?e :created-at ?created-at]
                                          [(< ?created-at middle-date)]]
                                 :args [{'middle-date middle-date
                                         'entity-type entity-type}]})
          after-result (crux/q (crux/db node)
                               {:find '[(pull ?e [*])]
                                :where '[[?e :entity/type entity-type]
                                         [?e :created-at ?created-at]
                                         [(> ?created-at middle-date)]]
                                :args [{'middle-date middle-date
                                        'entity-type entity-type}]})
          all-result (crux/q (crux/db node)
                             {:find '[(pull ?e [*])]
                              :where '[[?e :entity/type entity-type]
                                       [?e :created-at ?created-at]
                                       [(< ?created-at future-date)]]
                              :args [{'future-date future-date
                                      'entity-type entity-type}]})
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

(deftest test-relationship
  (testing "Relationship"
    (let [entity-content :entity/relationship-content
          entity-content-derived :entity/relationship-content-derived
          content (create-entity-sync node entity-content {:name "original content"})
          content-id (:crux.db/id content)
          derived-content-1 (create-entity-sync node entity-content-derived {:name "derived content 1"
                                                                             :orig-content-id content-id
                                                                             :relationship "thumbnail_256"})
          derived-content-2 (create-entity-sync node entity-content-derived {:name "derived content 2"
                                                                             :orig-content-id content-id
                                                                             :relationship "thumbnail_480"})
          derived-content-3 (create-entity-sync node entity-content-derived {:name "derived content 3"
                                                                             :orig-content-id content-id
                                                                             :relationship "720p"})
          derived-contents (find-entities-by-attr node entity-content-derived :orig-content-id content-id)]
      (is content-id)
      (is derived-content-1)
      (is derived-content-2)
      (is derived-content-3)
      (is (= 3
             (count derived-contents))))))

(deftest test-user-actions
  (testing "User actions"
    (let [entity-user :entity/user-actions-user
          user (create-entity-sync node entity-user {:name "user 1"})
          user-act-type :sns/user-act
          user-id (:crux.db/id user)
          user-act-play (create-entity-sync node user-act-type {:subject-id user-id
                                                                :subject-type :cms/thing
                                                                :subject-action :play})
          user-act-like (create-entity-sync node user-act-type {:subject-id user-id
                                                                :subject-type :cms/thing
                                                                :subject-action :like})
          - (create-entity-sync node user-act-type {:subject-id user-id
                                                    :subject-type :cms/thing
                                                    :subject-action :like})
          user-act-watch (create-entity-sync node user-act-type {:subject-id user-id
                                                                 :subject-type :cms/thing
                                                                 :subject-action :watch})
          _ (create-entity-sync node user-act-type {:subject-id user-id
                                                    :subject-type :cms/thing
                                                    :subject-action :watch})
          _ (create-entity-sync node user-act-type {:subject-id user-id
                                                    :subject-type :cms/thing
                                                    :subject-action :watch})
          user-act-share (create-entity-sync node user-act-type {:subject-id user-id
                                                                 :subject-type :cms/thing
                                                                 :subject-action :share})
          _ (create-entity-sync node user-act-type {:subject-id user-id
                                                    :subject-type :cms/thing
                                                    :subject-action :share})
          _ (create-entity-sync node user-act-type {:subject-id user-id
                                                    :subject-type :cms/thing
                                                    :subject-action :share})
          _ (create-entity-sync node user-act-type {:subject-id user-id
                                                    :subject-type :cms/thing
                                                    :subject-action :share})
          result (crux/q (crux/db node)
                         {:find '[play like watch share]
                          :where '[[(q {:find [(count ?e)]
                                        :where [[?e :entity/type :sns/user-act]
                                                [?e :subject-id ?sid]
                                                [?e :subject-type ?stype]
                                                [?e :subject-action :play]]})
                                    [[play]]]
                                   [(q {:find [(count ?e)]
                                        :where [[?e :entity/type :sns/user-act]
                                                [?e :subject-id ?sid]
                                                [?e :subject-type ?stype]
                                                [?e :subject-action :like]]})
                                    [[like]]]
                                   [(q {:find [(count ?e)]
                                        :where [[?e :entity/type :sns/user-act]
                                                [?e :subject-id ?sid]
                                                [?e :subject-type ?stype]
                                                [?e :subject-action :watch]]})
                                    [[watch]]]
                                   [(q {:find [(count ?e)]
                                        :where [[?e :entity/type :sns/user-act]
                                                [?e :subject-id ?sid]
                                                [?e :subject-type ?stype]
                                                [?e :subject-action :share]]})
                                    [[share]]]
                                   ]
                           :args [{'?sid user-id
                                   '?stype :cms/thing}]})]
      (is user-id)
      (is user)
      (is (not (empty? result)))
      (is (= [1 2 3 4]
             (first result))))))