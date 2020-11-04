(ns common.crux-util
  (:require [clojure.set :as s]))

(defn- prefixify-map [prefix thing]
  (if (map? thing)
    (s/rename-keys
     thing
     (->> (keys thing)
          (map (fn [k]
                 [k
                  (keyword (str (name prefix) "." (name k)))]))
          (into {})))
    thing))

(defn- prefixify-vec [prefix thing]
  (let [rename (fn [el]
                 (if (map? el)
                   (seq
                    (s/rename-keys
                     el
                     (->> (keys el)
                          (map (fn [k]
                                 [k
                                  (keyword (str (name prefix) "." (name k)))]))
                          (into {}))))
                   el))]
    (if (vector? thing)
      (let [maps (filter map? thing)
            non-maps (remove map? thing)]
        (merge 
         (->> (map rename maps)
              (apply concat)
              (group-by key)
              (map (fn [[k vs]]
                     {k (map second vs)}))
              (into {}))
         (when (seq non-maps)
           {prefix non-maps})))
      thing)))

(defn prefixify-children [thing]
  (if (map? thing)
    (->> thing
         (map (fn [[k v]]
                (cond (map? v)
                  (let [prefixed (prefixify-map k v)]
                    (if (map? prefixed)
                      prefixed
                      {k v}))
                  
                  (vector? v)
                  (let [prefixed (prefixify-vec k v)]
                    (if (map? prefixed)
                      prefixed
                      {k v}))
                  
                  :default
                  {k v})))
         (apply merge))
    thing))

(defn ->base-level [m]
  (->> (clojure.walk/postwalk prefixify-children m)
       (map (fn [[k v]]
              {k (if (sequential? v)
                   (vec (flatten v))
                   v)}))
       (into {})
       ))
