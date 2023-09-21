(ns metosin.arcone.session.cookie-middleware
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [metosin.arcone.session.core :as session]
            [metosin.arcone.util.validate :refer [validate!]]
            [ring.util.http-response :as resp]))


; There's no well defined way to remove a cookie. The commonly used and generally
; working way to remove cookie is to set the cookie to value with `expires` set 
; to Thu, 01 Jan 1970 00 :00:00 GMT. This is sometimes called "cookie depth charge".

(def ^:private cookie-depth-charge {:value     ""
                                    :path      "/"
                                    :http-only true
                                    :same-site :strict
                                    :expires   "Thu, 01 Jan 1970 00:00:00 GMT"})


(def ^:private SessionCookieMiddlewareConfig
  [:map
   [:session-cookie-name :string]
   [:max-age :int]
   [:get-session fn?]])


(defmethod ig/init-key ::middleware [_ config]
  (let [{:keys [session-cookie-name max-age get-session]} (validate! SessionCookieMiddlewareConfig config)
        cookie-value-path                                 [:cookies session-cookie-name :value]]
    (fn [handler]
      (fn [req]
        (let [resp (if-let [cookie-value (get-in req cookie-value-path)]
                     (if-let [session (get-session cookie-value)]
                       (handler (assoc req
                                       ::session/session session
                                       ::session/source :cookie
                                       ::session/key cookie-value))
                       (do (log/warnf "use of unknown session cookie value: value=[%s]" cookie-value)
                           (resp/forbidden {:message "forbidden"})))
                     (handler req))]
          (if (contains? resp ::session/session)
            (assoc-in resp [:cookies session-cookie-name]
                      (if-let [session (::session/session resp)]
                        {:value     session
                         :path      "/"
                         :http-only true
                         :same-site :strict
                         :max-age   max-age}
                        cookie-depth-charge))
            resp))))))
