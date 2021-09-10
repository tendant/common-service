(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]
            [simple.build :as sb]))

(def lib 'org.clojars.wang/common-service)

;; if you want a version of MAJOR.MINOR.COMMITS:
(def version (format "1.0.%s" (b/git-count-revs nil)))

(def scm {:url "https://github.com/tendant/common-service"})

(defn jar
  [opts]
  (-> opts
      (assoc :lib lib :version version :scm scm)
      (bb/clean)
      (bb/jar)))

(defn uberjar
  [opts]
  (-> opts
      (assoc :lib lib :version version :scm scm)
      (bb/clean)
      (sb/uberjar)))

(defn install
  [opts]
  (-> opts
      (assoc :lib lib :version version :scm scm)
      (sb/install)))

(defn release
  [opts]
  (-> opts
      (assoc :lib lib :version version :scm scm)
      (sb/release)))