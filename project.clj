(defproject tendant/common-service "0.1.3"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [juxt/crux-core "20.09-1.12.1-beta"]
                 [juxt/crux-jdbc "20.09-1.12.1-beta"]
                 [yogthos/config "1.1.5"]]
  :repl-options {:init-ns common.crux-svc})
