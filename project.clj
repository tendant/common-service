(defproject tendant/common-service "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [juxt/crux-core "20.06-1.9.1-beta"]
                 [juxt/crux-rocksdb "20.06-1.9.1-beta"]
                 [juxt/crux-lmdb "20.06-1.9.1-alpha"]
                 [juxt/crux-kafka "20.06-1.9.1-beta"]
                 [juxt/crux-kafka-embedded "20.06-1.9.1-beta"]
                 [juxt/crux-jdbc "20.06-1.9.1-beta"]
                 [org.postgresql/postgresql "42.2.14"]
                 [juxt/crux-http-server "20.06-1.9.1-alpha"]
                 [juxt/crux-http-client "20.06-1.9.1-beta"]
                 [yogthos/config "1.1.5"]]
  :repl-options {:init-ns common.crux-svc})
