(ns metosin.arcone.http.server
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [metosin.nima-ring.server :as server]
            [metosin.nima-ring.static-content :as static-content]
            [metosin.arcone.util.validate :refer [validate!]]))


(set! *warn-on-reflection* true)


(def Config
  [:map
   [:host :string]
   [:port {:optional true} :int]
   [:routing :any]])


(defmethod ig/init-key ::server [_ config]
  (let [{:keys [host port routing]} (validate! Config config)]
    (log/info "Nima HTTP server starting...")
    (let [server (server/create-server routing {:host host
                                                :port port})]
      (log/infof "Nima HTTP server listening at http://%s:%d" host (server/port server))
      server)))


(defmethod ig/halt-key! ::server [_ server]
  (log/info "Nima HTTP server stopping...")
  (server/shutdown server))


(defmethod ig/init-key ::static-files [_ _]
  (static-content/static-files-service "public" {:index       "index.html"
                                                 :path-mapper :html5}))
