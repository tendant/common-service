(ns common.crux-svc
  (:require [crux.api :as crux]
            [config.core :refer [env]]
            [clojure.java.io :as io]))

(defn- gen-uuid*
  []
  (java.util.UUID/randomUUID))

(defn- uuid
  [s]
  (java.util.UUID/fromString s))

(defn create-entity
  [node entity-type params]
  (let [crux-id (:crux.db/id params (gen-uuid*))
        crux-tx (crux/submit-tx
                 node
                 [[:crux.tx/put
                   (assoc params
                          :crux.db/id crux-id
                          :entity/type entity-type)
                   (:crux.db/valid-time params)]])]
    (-> params
        (assoc :crux.db/id crux-id)
        (assoc :crux.tx crux-tx))))

(defn create-entity-sync
  [node entity-type params]
  (let [crux-id (:crux.db/id params (gen-uuid*))
        crux-tx (crux/submit-tx
                 node
                 [[:crux.tx/put
                   (assoc params
                          :crux.db/id crux-id
                          :entity/type entity-type)
                   (:crux.db/valid-time params)]])]
    (crux/await-tx node crux-tx)
    (-> params
        (assoc :crux.db/id crux-id)
        (assoc :crux.tx crux-tx))))

(defn retrieve-entity-by-id
  [node id]
  (crux/entity (crux/db node) id))

(defn find-entity-by-id
  [node id]
  (->> (crux/q (crux/db node)
               {:find '[?e]
                :where '[[?e :crux.db/id ?id]]
                :args [{'?id id}]})
       (map (fn [[id]]
              (retrieve-entity-by-id node id)))
       (first)))

(defn find-entity-by-id-and-type
  [node id entity-type]
  (->> (crux/q (crux/db node)
               {:find '[?e]
                :where '[[?e :crux.db/id ?id]
                         [?e :entity/type ?t]]
                :args [{'?id id '?t entity-type}]})
       (map (fn [[id]]
              (retrieve-entity-by-id node id)))
       (first)))

(defn find-entities-by-ids
  [node ids]
  {:pre [(set? ids)]}
  (->> (crux/q (crux/db node)
               {:find '[?e]
                :where '[[?e :crux.db/id ?id]
                         [(contains? ?ids ?id)]]
                :args [{'?ids ids}]})
       (map (fn [[id]]
              (retrieve-entity-by-id node id)))))

(defn find-entities-by-ids-and-type
  [node ids entity-type]
  {:pre [(set? ids)]}
  (->> (crux/q (crux/db node)
               {:find '[?e]
                :where '[[?e :crux.db/id ?id]
                         [(contains? ?ids ?id)]
                         [?e :entity/type ?t]]
                :args [{'?ids ids '?t entity-type}]})
       (map (fn [[id]]
              (retrieve-entity-by-id node id)))))

(defn find-entities-by-attr
  [node entity-type attr value]
  (->> (crux/q (crux/db node)
               {:find '[?e]
                :where [['?e attr '?v]
                        ['?e :entity/type '?t]]
                :args [{'?v value '?t entity-type}]})
       (map (fn [[id]]
              (retrieve-entity-by-id node id)))))

(defn find-entities-by-attrs
  [node entity-type attrs]
  {:pre [(map? attrs)]}
  (->> (crux/q (crux/db node)
               `{:find [~'?e]
                 :where ~(reduce 
                          (fn [q [a v]]
                            (conj q ['?e a v]))
                          [['?e :entity/type entity-type]]
                          attrs)
                 })
       (map (fn [[id]]
              (retrieve-entity-by-id node id)))))

(defn find-entities-by-attrs-with-order-by-and-limit*
  [node entity-type attrs order-by limit]
  {:pre [(map? attrs)
         (map? order-by)
         (int? limit)]}
  (let [[ok ov] (first (seq order-by))
        avs (dissoc attrs ok)
        ab (get attrs ok)
        ]
    (->> (crux/q (crux/db node)
                 `{:find ~(cond-> ['?e]
                            (some? ov)
                            (conj '?o))
                   :where ~(cond-> (reduce
                                    (fn [q [a v]]
                                      (conj q ['?e a v]))
                                    [['?e :entity/type entity-type]]
                                    avs)

                             (some? ov)
                             (conj ['?e ok '?o])

                             (some? ab)
                             (conj (case ov
                                     :desc [`(< ~'?o ~ab)]
                                     :asc [`(> ~'?o ~ab)]))
                                   )
                   :order-by ~(cond-> []
                                (some? ov)
                                (conj ['?o ov]))
                   :limit ~limit
                   })
         (map (fn [[id]]
                (retrieve-entity-by-id node id))))))

(defn find-entity-by-attr
  [node entity-type attr value]
  (first (find-entities-by-attr node entity-type attr value)))

(defn find-entity-by-attrs
  [node entity-type attrs]
  (first (find-entities-by-attrs node entity-type attrs)))

(defn entities
  [node entity-type]
  (->> (crux/q (crux/db node)
               {:find '[?e]
                :where '[[?e :entity/type ?t]]
                :args [{'?t entity-type}]})
       (map (fn [[id]]
              (retrieve-entity-by-id node id)))))

(defn retrieve-entity-tx-by-id
  [node id]
  (crux/entity-tx (crux/db node) id))

(defn assoc-entity-tx
  [node entity]
  (-> (retrieve-entity-tx-by-id node (:crux.db/id entity))
      (select-keys [:crux.db/valid-time :crux.tx/tx-time])
      (merge entity)))

(defn find-histories-by-id
  [node id]
  (crux/entity-history (crux/db node) id :asc))

(defn update-entity
  [node id params]
  (let [entity (retrieve-entity-by-id node id)
        updated (merge entity params {:crux.db/id id})]
    (when entity
      (crux/submit-tx
       node
       [[:crux.tx/put
         updated]])
      updated)))

(defn update-entity-with-type
  [node id entity-type params]
  (let [entity (retrieve-entity-by-id node id)
        updated (merge entity params {:crux.db/id id})]
    (when (and entity
               (= entity-type (:entity/type entity)))
      (crux/submit-tx
       node
       [[:crux.tx/put
         updated]])
      updated)))

(defn update-entity-sync
  [node id params]
  (let [entity (retrieve-entity-by-id node id)
        updated (merge entity params {:crux.db/id id})]
    (when entity
      (->> (crux/submit-tx
            node
            [[:crux.tx/put
              updated]])
           (crux/await-tx node ))
      updated)))

(defn update-entity-with-type-sync
  [node id entity-type params]
  (let [entity (retrieve-entity-by-id node id)
        updated (merge entity params {:crux.db/id id})]
    (when (and entity
               (= entity-type (:entity/type entity)))
      (->> (crux/submit-tx
            node
            [[:crux.tx/put
              updated]])
           (crux/await-tx node))
      updated)))

(defn delete-entity
  [node crux-id]
  (crux/submit-tx
   node
   [[:crux.tx/delete
     crux-id]]))

(comment
  (def node (crux.api/start-node {}))

  (dotimes [n 10]
    (create-entity node :entity/contact {:name (format "test contact %s" n)
                                         :created-at n}))

  (entities node :entity/contact)

  (find-entities-by-attrs-with-order-by-and-limit node :entity/contact {:created-at 3} {:created-at :asc} 5)

  (create-entity node :entity/contact {:openid "test open id"})

  (find-entity-by-id node (uuid "622a03c8-5e31-46ad-b847-75d473f93c06"))
  (retrieve-entity-tx-by-id node (uuid "622a03c8-5e31-46ad-b847-75d473f93c06"))
  
  (find-entities-by-attr node :entity/contact :openid "test open id")
  (find-entities-by-attrs node :entity/contact {:openid "test open id"})
  (update-entity node (uuid "c92bcef0-7622-420e-8a7d-22f191cd767d")
                 {:user-id "test user id"})

  (delete-entity node (uuid "b1281800-1411-4a90-a945-89d1e501743f"))
  
  (dotimes [n 100]
    (create-entity node :entity/organization {:name (format "test organization %s" n)}))

  (entities node :entity/organization)

  (update-entity node (uuid "id") {:name (format "updated organization %s" "id")}))