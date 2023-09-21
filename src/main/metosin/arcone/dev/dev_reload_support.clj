(ns metosin.arcone.dev.dev-reload-support
  (:require  [clojure.walk]
             [clojure.tools.logging :as log]
             [integrant.core :as ig]
             [muuntaja.core]
             [jsonista.core :as json]
             [ring.middleware.params]
             [ring.middleware.cookies]
             [reitit.ring]
             [reitit.ring.coercion]
             [reitit.coercion.malli]
             [reitit.middleware]
             [reitit.ring.middleware.muuntaja]
             [metosin.nima-ring.sse :as sse])
  (:import (java.nio.file FileSystems
                          Paths
                          WatchKey
                          WatchEvent
                          WatchEvent$Kind
                          StandardWatchEventKinds)))


;;
;; Reload manager:
;;


(def watch-kinds ^"[Ljava.nio.file.WatchEvent$Kind;" (into-array WatchEvent$Kind [StandardWatchEventKinds/ENTRY_CREATE
                                                                                  StandardWatchEventKinds/ENTRY_MODIFY
                                                                                  StandardWatchEventKinds/ENTRY_DELETE]))


(defmethod ig/init-key ::reload-manager [_ {:keys [path]}]
  (let [watch-service (.newWatchService (FileSystems/getDefault))
        listeners     (atom {})]
    (-> (Paths/get path (into-array String []))
        (.register watch-service watch-kinds))
    (.start (Thread/ofVirtual) (fn []
                                 (try
                                   (while true
                                     (let [^WatchKey k (.take watch-service)]
                                       (try
                                         (let [event (mapv (fn [^WatchEvent e]
                                                             [(subs (.name (.kind e)) 6)
                                                              (str "/" (.context e))])
                                                           (.pollEvents k))]
                                           (doseq [listener (vals @listeners)]
                                             (try
                                               (listener event)
                                               (catch Exception e
                                                 (log/error e "reload-manager: error while notifying listener")))))
                                         (finally
                                           (.reset k)))))
                                   (catch InterruptedException _
                                     (log/info "reload-handler: interrupted, closing down"))
                                   (finally
                                     (.close watch-service)))))
    {:watch-service watch-service
     :listeners     listeners}))


(defmethod ig/resolve-key ::reload-manager [_ {:keys [listeners]}]
  listeners)


(defmethod ig/halt-key! ::reload-manager [_ {:keys [listeners ^java.io.Closeable watch-service]}]
  (doseq [listener (vals @listeners)]
    (listener ["RESET"]))
  (.close watch-service))


;;
;; Reload handler:
;;


(defmethod ig/init-key ::reload-handler [_ {:keys [reload-manager]}]
  (fn [req]
    (log/info "reload-handler: new SSE client registered")
    (let [emitter      (sse/sse-emitter req)
          queue        (java.util.concurrent.LinkedBlockingQueue.)
          listener-key (gensym)
          listener     (fn [event] (.put queue event))]
      (swap! reload-manager assoc listener-key listener)
      (try
        (loop [n 0]
          (let [[event-type filename] (.take queue)]
            (log/infof "reload-handler: new event: event-type=[%s], filename=[%s]", event-type filename)
            (emitter {:id   (str "#" n)
                      :name event-type
                      :data filename}))
          (recur (inc n)))
        (catch Exception e
          (if (-> e (.getCause) (.getMessage) (= "Broken pipe"))
            (log/info "reload-handler: SSE client closed connection")
            (log/error e "reload-handler: unexpected error")))
        (finally
          (swap! reload-manager dissoc listener-key)
          (emitter))))))
