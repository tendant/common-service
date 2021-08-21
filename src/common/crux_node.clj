(ns common.crux-node
  (:require [crux.api :as crux]
            [clojure.java.io :as io]
            [config.core :as config])
  (:import java.time.Duration))

(defn- mem-node-config
  []
  {})

;; enviroment varaibles:
;; CRUX_CHECKPOINTER_TYPE: "filesystem" or "s3"
;;
(defn- checkpointer-store
  []
  (case (:crux-checkpointer-type config/env)
    "filesystem" {:crux/module 'crux.checkpoint/->filesystem-checkpoint-store
                  :path (:crux-checkpointer-filesystem-path config/env)}
    "s3" {:crux/module 'crux.s3.checkpoint/->cp-store
          :bucket (:crux-checkpointer-s3-bucket config/env)
          :prefix (:crux-checkpointer-s3-prefix config/env)}))

(defn- jdbc-node-config []
  (let [config (cond-> {:crux.jdbc/connection-pool {:dialect {:crux/module 'crux.jdbc.psql/->dialect}
                                                    :db-spec {:dbtype (:crux-jdbc-dbtype config/env)
                                                              :dbname (:crux-jdbc-dbname config/env)
                                                              :host (:crux-jdbc-host config/env)
                                                              :user (:crux-jdbc-user config/env)
                                                              :password (:crux-jdbc-password config/env)}
                                                    :pool-opts {:maximumPoolSize (or (:db-maximum-pool-size config/env) 2)}}
                        :crux/tx-log {:crux/module 'crux.jdbc/->tx-log
                                      :connection-pool :crux.jdbc/connection-pool}
                        :crux/document-store {:crux/module 'crux.jdbc/->document-store
                                              :connection-pool :crux.jdbc/connection-pool}
                        :crux/index-store {:kv-store {:crux/module 'crux.rocksdb/->kv-store
                                                      :db-dir (io/file "./rocksdb")}}}
                 (:crux-checkpointer-enabled config/env) (assoc-in [:crux/index-store :kv-store :checkpointer]
                                                                    {:crux/module 'crux.checkpoint/->checkpointer
                                                                     :approx-frequency (Duration/ofMinutes (:crux-checkpointer-frequency-of-minutes config/env))
                                                                     :store (checkpointer-store)}))]
    config))

(defn- node-config
  []
  (let [typed-config (case (:crux-node-type config/env)
                       "jdbc" (jdbc-node-config)
                       "mem" (mem-node-config))]
    (cond-> typed-config
      (:crux-checkpointer-enabled config/env) (assoc-in [:crux/index-store :kv-store :checkpointer]
                                                        {:crux/module 'crux.checkpoint/->checkpointer
                                                         :approx-frequency (Duration/ofMinutes (:crux-checkpointer-frequency-of-minutes config/env))
                                                         :store (checkpointer-store)}))))

(defn start-crux-node
  []
  (let [config (node-config)]
    (crux/start-node config)))
