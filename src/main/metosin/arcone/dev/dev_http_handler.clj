(ns metosin.arcone.dev.dev-http-handler
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as resp]
            [metosin.arcone.dev.dev-http-handler.config :as config]
            [metosin.arcone.dev.dev-http-handler.serve-content :refer [serve-content]]
            [metosin.arcone.dev.dev-http-handler.proxy-request :refer [proxy-request]]))


;;
;; Dev handler:
;;   Serves file resources and proxies HTTP requests to actual server.
;;   Proxy requests that begin with configurabe prefix (defaults to "/api/"). If request
;;   does not match prefix tries to serve it from file system. If the requested URI
;;   refers to file, serves that file. If not, serves the configurable default
;;   file (defaults to "index.html").
;;
;;   Intended to be used as `:handler` for Shadow-cljs dev http server. See 
;;   https://shadow-cljs.github.io/docs/UsersGuide.html#dev-http for more info.
;;
;;   Configure by updating dynamic var at `config/config`
;;


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn handler [{:keys [uri request-method]
                :as   req}]
  (cond
    (str/starts-with? uri (-> config/config :proxy :prefix))
    (proxy-request req)

    (not (#{:get :head} request-method))
    (resp/not-found)

    :else
    (serve-content req)))
