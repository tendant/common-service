(ns handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.exceptions :as exceptions]
            [org.httpkit.server :as http]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
            [config.core :refer [env]]
            [ring.util.response :as resp :refer [response file-response resource-response redirect]]
            [config.core :as config]
            [crux.api :as crux]
            [common.crux-svc :as cs]
            )
  (:gen-class))

(defn- start-standalone-node ^crux.api.ICruxAPI [storage-dir]
  (crux/start-node {}))

(def ^:private crux-node (atom nil))

(defn- get-node []
  (when (nil? @crux-node)
    (log/debug "Start new crux node...")
    (reset! crux-node (start-standalone-node "crux-store"))
    (log/debug "Done new crux node."))
  @crux-node)

(defn warm-up-crux-node
  []
  (get-node)
  ;; wait for crux node to be ready
  (Thread/sleep 200)
  )

(defn wrap-crux-node
  [handler]
  (fn [req]
    (->> (get-node)
         (assoc req :crux-node)
         handler)))

(defn default-pre-hook
  [e req uuid]
  (log/debugf "error-id(%s): %s" uuid e))

(defn wrap-debugger
  "Adds debugger"
  {:no-doc true}
  [handler name]
  (fn [request]
    (log/debug "BEFORE " name (:request-method request) " " (:uri request) request)
    (let [resp (handler request)
          ip (:remote-addr request)
          uri (:uri request)
          headers (:headers request)]
      (log/debugf "AFTER %s %s %s: response code: %s." name (:request-method request) (:uri request) (:status resp))
      (if true
        (log/debug resp))
      resp)))

(def ^:private app-path "/app")

(defn app-index-route
  [& [fragment]]
  (str app-path fragment))

(defroutes app-routes
  (GET "/" [:as req]
    (let [node (:crux-node req)]
      (->> (cs/entities node :entity/contact)
           (json/encode)
           (#(resp/response {:status 200
                             :body %})))))
  (POST "/new-contact" [:as req]
    (let [node (:crux-node req)
          n (rand-int 100)]
      (->> (cs/create-entity-sync node :entity/contact {:name (format "test contact %s" n)})
           (json/encode)
           (#(resp/response {:status 200
                             :body %})))))
  (route/not-found "Not Found"))

(def app
  (-> (wrap-reload #'app-routes)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-json-params)
      (wrap-multipart-params)
      (exceptions/wrap-exceptions {:error-fns exceptions/default-error-fns
                                   :pre-hook default-pre-hook})
      (wrap-json-response)
      (wrap-cors :access-control-allow-origin [#"http://.*:3000" #"https://.*:3000" #"https://.*localhost/*.*"]
                 :access-control-allow-methods [:get :put :post :delete])
      (wrap-reload)
      (wrap-crux-node)
      (wrap-debugger "app")))

(defn -main [& args]
  (let [port (try
               (Integer/parseInt (first args))
               (catch Exception _
                 9090))]
    (warm-up-crux-node)
    (http/run-server app {:port port})
    (log/debugf "Server started on port %s....%n" port)))
