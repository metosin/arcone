(ns metosin.arcone.dev.dev-http-handler.proxy-request
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [jsonista.core :as json]
            [metosin.arcone.dev.dev-http-handler.config :as config]
            [metosin.arcone.dev.dev-http-handler.gzip-pipe :refer [gzip-pipe]])
  (:import (java.time Duration)
           (java.net.http HttpClient
                          HttpClient$Redirect
                          HttpClient$Version
                          HttpRequest
                          HttpRequest$BodyPublisher
                          HttpRequest$BodyPublishers
                          HttpResponse
                          HttpResponse$BodyHandlers)))


;;
;; Proxy request to server:
;;


(set! *warn-on-reflection* true)


(def ^:private ^HttpClient client (-> (HttpClient/newBuilder)
                                      (.connectTimeout (Duration/ofSeconds 5))
                                      (.executor (java.util.concurrent.Executors/newVirtualThreadPerTaskExecutor))
                                      (.followRedirects HttpClient$Redirect/NORMAL)
                                      (.version HttpClient$Version/HTTP_1_1)
                                      (.build)))


(def ^:private request-method->request-name (->> [:get :head :options :put :post :delete]
                                                 (reduce (fn [acc request-method]
                                                           (assoc acc request-method (-> request-method (name) (str/upper-case))))
                                                         {})))


(def ^:private restricted-request-header? #{"host" "connection" "content-length"})


(defn- body-publisher ^HttpRequest$BodyPublisher [body]
  (if (some? body)
    (HttpRequest$BodyPublishers/ofInputStream
     (reify java.util.function.Supplier
       (get [_this] body)))
    (HttpRequest$BodyPublishers/noBody)))


(defn- ring-req->java-req ^HttpRequest [{:keys [uri request-method headers body]}]
  (-> (HttpRequest/newBuilder)
      (.uri (java.net.URI/create (str "http://"
                                      (-> config/config :proxy :host)
                                      ":"
                                      (-> config/config :proxy :port)
                                      uri)))
      (.method (request-method->request-name request-method)
               (body-publisher body))
      (.headers (into-array String (->> headers
                                        (remove (comp restricted-request-header? key))
                                        (mapcat identity)
                                        (concat ["x-arcone-dev-proxy" "true"]))))
      (.build)))


(defn- java-resp->ring-resp [^HttpResponse resp]
  (let [headers (->> resp
                     (.headers)
                     (.map)
                     (reduce (fn [acc [k v]]
                               (assoc! acc k (first v)))
                             (transient {}))
                     (persistent!))]
    {:status  (.statusCode resp)
     :headers headers
     :body    (if (-> headers (get "content-encoding") (= "gzip"))
                (gzip-pipe (.body resp))
                (.body resp))}))


(def ^:private input-stream-body-handler (HttpResponse$BodyHandlers/ofInputStream))


(defn- send-request ^HttpResponse [^HttpRequest req]
  (.send client req input-stream-body-handler))


(defn proxy-request [req]
  (try
    (log/debugf "%s %s" (-> req :request-method (name) (str/upper-case)) (-> req :uri))
    (let [start (System/currentTimeMillis)
          resp  (-> (ring-req->java-req req)
                    (send-request)
                    (java-resp->ring-resp))
          end   (System/currentTimeMillis)]
      (log/infof "%s %s => %d (%d ms)"
                 (-> req :request-method (name) (str/upper-case))
                 (-> req :uri)
                 (-> resp :status)
                 (- end start))
      resp)
    (catch java.io.IOException e
      (log/warnf "IO-Error from proxy: %s %s"
                 (-> req :request-method (name) (str/upper-case))
                 (-> req :uri))
      {:status  500
       :headers {"content-type" "application/json"}
       :body    (-> {:type    :proxy-error
                     :error   (-> e (.getClass) (.getName))
                     :message (-> e (.getMessage))}
                    (json/write-value-as-bytes)
                    (java.io.ByteArrayInputStream.))})))
