(ns metosin.arcone.db.ds
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [next.jdbc :as jdbc]
            [next.jdbc.connection]
            [metosin.arcone.db.json]
            [metosin.arcone.util.validate :refer [validate!]])
  (:import (com.zaxxer.hikari HikariDataSource)))


;;
;; Datasource:
;;


(def DbSpec
  [:map {:closed true}
   [:host :string]
   [:port [:int {:default 5432}]]
   [:dbname :string]
   [:username :string]
   [:password :string]
   [:dbtype [:string {:default "postgres"}]]
   [:server-role :string]])


(defmethod ig/init-key ::ds [_ db-spec]
  (let [db-spec (validate! DbSpec db-spec)]
    (log/info "DB - Creating connection pool")
    (let [ds (next.jdbc.connection/->pool HikariDataSource (-> db-spec
                                                               (assoc :connectionInitSql (str "set role " (:server-role db-spec) ";"))
                                                               (dissoc :server-role)))]
      (with-open [conn (jdbc/get-connection ds)]
        (jdbc/execute-one! conn ["select true"]))
      ds)))


(defmethod ig/halt-key! ::ds [_ ds]
  (log/info "DB - Closing connection pool")
  (when ds
    (let [^java.io.Closeable pool (jdbc/get-datasource ds)]
      (.close pool))))
