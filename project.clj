(defproject tendant/common-service "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [juxt/crux-core "20.09-1.11.0-beta"]
                 [juxt/crux-jdbc "20.09-1.11.0-beta"]
                 [yogthos/config "1.1.5"]]
  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[compojure "1.6.1"]
                        [ring "1.8.1"]
                        [ring/ring-json "0.5.0"]
                        [ring-cors "0.1.13"]
                        [tendant/ring-exceptions "0.1.6"]
                        [com.taoensso/timbre "4.10.0"]
                        [http-kit "2.3.0"]
                        [clojure.java-time "0.3.2"]
                        [tendant/proxy-request "0.1.0"]
                        [org.slf4j/slf4j-api "1.7.21"]
                        [org.slf4j/log4j-over-slf4j "1.7.30"]]}}
  :main handler
  :repl-options {:init-ns common.crux-svc})
