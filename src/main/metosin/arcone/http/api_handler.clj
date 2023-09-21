(ns metosin.arcone.http.api-handler
  (:require  [clojure.string :as str]
             [clojure.tools.logging :as log]
             [clojure.walk]
             [integrant.core :as ig]
             [muuntaja.core]
             [jsonista.core :as json]
             [ring.util.http-response :as resp]
             [ring.middleware.params]
             [ring.middleware.cookies]
             [reitit.ring]
             [reitit.ring.coercion]
             [reitit.coercion.malli]
             [reitit.middleware]
             [reitit.ring.middleware.muuntaja]
             [reitit.ring.middleware.exception :as exception]
             [metosin.arcone.http.cache :as cache]))


(defn- exception-handler [handler e req]
  (when-not (-> e (ex-data) :type (= ::resp/response))
    (log/errorf e "error: %s %s"
                (-> req :request-method (name) (str/upper-case))
                (-> req :uri)))
  (handler e req))


(def default-middleware [cache/cache-middleware
                         ring.middleware.params/wrap-params
                         ring.middleware.cookies/wrap-cookies
                         reitit.ring.middleware.muuntaja/format-middleware
                         reitit.ring.coercion/coerce-exceptions-middleware
                         reitit.ring.coercion/coerce-request-middleware
                         reitit.ring.coercion/coerce-response-middleware
                         (exception/create-exception-middleware (assoc exception/default-handlers
                                                                       ::exception/wrap
                                                                       exception-handler))])


(defmethod ig/init-key ::handler [_ {:keys [routes middleware middleware-registry]}]
  (reitit.ring/ring-handler
   (reitit.ring/router routes
                       {:data                       {:muuntaja   muuntaja.core/instance
                                                     :coercion   reitit.coercion.malli/coercion
                                                     :middleware (into default-middleware middleware)}
                        :reitit.middleware/registry middleware-registry})))



(defn- deref-var-handlers [routes]
  (clojure.walk/prewalk (fn [v]
                          (if (var? v)
                            (deref v)
                            v))
                        routes))


(defmethod ig/init-key ::routes [_ {:keys [mode routes]}]
  (case mode
    :dev (do (log/warn "handler: DEV mode")
             routes)
    :prod (do (log/info "handler: PROD mode")
              (deref-var-handlers routes))))


(defmethod ig/init-key ::not-found [_ _]
  (let [not-found (-> {:type    :error
                       :message "route not found"}
                      (json/write-value-as-string)
                      (resp/not-found)
                      (update :headers assoc "content-type" "application/json"))]
    (fn [req]
      (log/warnf "route miss: %s [%s]"
                 (-> req :request-method (name) (str/upper-case))
                 (-> req :uri))
      not-found)))
