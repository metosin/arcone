(ns metosin.arcone.dev.dev-http-handler.serve-content
  (:require [clojure.java.io :as io]
            [ring.util.http-response :as resp]
            [metosin.arcone.dev.dev-http-handler.config :as config]))


;;
;; Serving static content:
;;


(defn serve-content [{:keys [uri request-method]}]
  (let [root           (io/file (-> config/config :root))
        file           (io/file root (subs uri 1))
        file           (if (.isFile file)
                         file
                         (io/file root (-> config/config :index)))
        [_ ext]        (re-matches #".*\.([^.]+)$" (.getName file))
        content-type   (or (get-in config/config [:content-types ext])
                           (get config/default-content-types ext)
                           "application/octet-stream")
        content-length (.length file)]
    (-> (resp/ok (if (= request-method :get) file nil))
        (update :headers assoc
                "content-type" content-type
                "content-length" (str content-length)))))
