(ns common.crux-svc
  (:require [crux.api :as crux]
            [config.core :refer [env]]
            [clojure.java.io :as io]))

(defn start-standalone-node ^crux.api.ICruxAPI [storage-dir]
  (crux/start-node {:crux.node/topology '[crux.standalone/topology]
                    :crux.kv/db-dir (str (io/file storage-dir "db"))}))

(defn start-jdbc-node []
  (crux/start-node {:crux.node/topology '[crux.jdbc/topology]
                    :crux.jdbc/dbtype (:crux-jdbc-dbtype env)
                    :crux.jdbc/dbname (:crux-jdbc-dbname env)
                    :crux.jdbc/host (:crux-jdbc-host env)
                    :crux.jdbc/user (:crux-jdbc-user env)
                    :crux.jdbc/password (:crux-jdbc-password env)}))

(defn start-jdbc-http-node [port]
  (crux/start-node {:crux.node/topology '[crux.jdbc/topology crux.http-server/module]
                    :crux.http-server/port port
                    ;; by default, the HTTP server is read-write - set this flag to make it read-only
                    :crux.http-server/read-only? false

                    :crux.jdbc/dbtype (:crux-jdbc-dbtype env)
                    :crux.jdbc/dbname (:crux-jdbc-dbname env)
                    :crux.jdbc/host (:crux-jdbc-host env)
                    :crux.jdbc/user (:crux-jdbc-user env)
                    :crux.jdbc/password (:crux-jdbc-password env)}))

(defn- start-node []
  ;; (start-standalone-node "crux-store")
  ;; (start-jdbc-http-node 10090)
  (start-jdbc-node)
  )

(def get-node (memoize start-node))

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
                          :entity/type entity-type)]])]
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

(defn find-entity-by-attr
  [node entity-type attr value]
  (first (find-entities-by-attr node entity-type attr value)))

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

(defn delete-entity
  [node crux-id]
  (crux/submit-tx
   node
   [[:crux.tx/delete
     crux-id]]))

(comment

  (def node (get-node))

  (dotimes [n 3]
    (create-entity node :entity/contact {:name (format "test contact %s" n)
                                         :age n}))

  (entities node :entity/contact)
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