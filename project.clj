(defproject tendant/common-service "0.2.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [pro.juxt.crux/crux-core "1.18.1"]
                 [pro.juxt.crux/crux-jdbc "1.18.1"]
                 [pro.juxt.crux/crux-s3 "1.18.1"]
                 [pro.juxt.crux/crux-rocksdb "1.18.1"]
                 [yogthos/config "1.1.7"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.0"]]}}
  :repl-options {:init-ns common.crux-svc})
