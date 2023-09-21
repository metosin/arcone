(ns metosin.arcone.session.core
  (:require [ring.util.http-response :as resp]))


(defn get-session [req]
  (get req ::session))


(defn require-session
  ([] (require-session some? "session required"))
  ([pred] (require-session pred "session required"))
  ([pred error-message]
   (fn [handler]
     (fn [req]
       (if-not (-> req ::session pred)
         (resp/forbidden {:message error-message})
         (handler req))))))

