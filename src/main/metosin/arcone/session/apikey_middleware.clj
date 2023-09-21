(ns metosin.arcone.session.apikey-middleware
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [ring.util.http-response :as resp]
            [metosin.arcone.session.core :as session]
            [metosin.arcone.util.validate :refer [validate!]]))


(def ^:private ApikeySessionMiddlewareConfig
  [:map
   [:apikey-header-name :string]
   [:get-session fn?]])


(defmethod ig/init-key ::middleware [_ config]
  (let [{:keys [apikey-header-name get-session]} (validate! ApikeySessionMiddlewareConfig config)
        apikey-path                              [:headers apikey-header-name]]
    (fn [handler]
      (fn [req]
        (if-let [apikey (get-in req apikey-path)]
          (if-let [account (get-session apikey)]
            (handler (assoc req
                            ::session/session account
                            ::session/source :apikey
                            ::session/key apikey))
            (do (log/warnf "use of unknown apikey: apikey=[%s]" apikey)
                (resp/forbidden {:message "forbidden"})))
          (handler req))))))
