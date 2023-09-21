(ns user
  (:require [integrant.repl :as igr]
            [integrant.repl.state :as state]
            [clojure.tools.logging :as log]
            [kaocha.repl :as k]))


;;
;; Integrant repl setup:
;;


(igr/set-prep!
 (fn [] ((requiring-resolve 'rockstore.server.system/init-components))))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn start []
  (log/info "user/start: system starting...")
  (igr/init))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn stop []
  (log/info "user/stop: system stopping...")
  (igr/halt))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn reset []
  (log/info "user/start: system resetting...")
  (igr/reset))


(defn system [] state/system)


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ds [] (-> (system) :arcone.db.ds/ds))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn run-unit-tests []
  (println "run-unit-tests...")
  (k/run :unit))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn run-all-tests []
  (println "run-all-tests...")
  (run-unit-tests))
