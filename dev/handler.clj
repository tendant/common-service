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
            [taoensso.timbre :as log]
            [config.core :refer [env]]
            [ring.util.response :as resp :refer [response file-response resource-response redirect]]
            [config.core :as config]
            [common.crux-svc :as crux-svc]
            )
  (:gen-class))

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

(defn graphql-session-response [result]
  (let [resp (response result)]
    (if-let [token (or (get-in result [:data "login" "token"])
                       (get-in result [:data "register" "token"]))]
      (assoc resp :cookies {"token" {:value token
                                     :path "/"
                                     :max-age (* 12 3600)}}) ; 12 hours
      resp)))

(def ^:private app-path "/app")

(defn app-index-route
  [& [fragment]]
  (str app-path fragment))

(defroutes app-routes
  (GET "/" [:as req]
       "OK")
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
      (wrap-debugger "app")))

(defn -main [& args]
  (let [port (try
               (Integer/parseInt (first args))
               (catch Exception _
                 9090))
        _ (println "Start jdbc node...")
        node (crux-svc/start-jdbc-node)
        _ (println "Started jdbc node.")]
    (http/run-server app {:port port})
    (log/debugf "Server started on port %s....%n" port)))
