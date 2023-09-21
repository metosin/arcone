(ns metosin.arcone.test.db-fixture
  (:require [next.jdbc :as jdbc]
            [next.jdbc.connection]
            [metosin.arcone.test.flyway :as flyway])
  (:import (com.zaxxer.hikari HikariDataSource)))


(def ^:dynamic *test-dbname* nil)
(def ^:dynamic *test-ds* nil)


(defn get-connection
  ([]
   (jdbc/get-connection *test-ds*))
  ([username password]
   (jdbc/get-connection {:dbtype "postgres"
                         :host   "localhost"
                         :port   5432
                         :dbname *test-dbname*}
                        {:user     username
                         :password password})))


(defn- get-admin-connection [{:keys [host port dbname username password]
                              :or   {host     "localhost"
                                     port     5432
                                     username "postgres"
                                     password "postgres"}}]
  (jdbc/get-connection {:dbtype "postgres"
                        :host   host
                        :port   port
                        :dbname dbname}
                       {:user        username
                        :password    password
                        :auto-commit true}))


(defn- create-db [admin-db-spec dbname]
  (with-open [conn (get-admin-connection admin-db-spec)]
    (jdbc/execute-one! conn [(str "create database " dbname " with lc_ctype='C.UTF-8'")])))


(defn- drop-db [admin-db-spec dbname]
  (with-open [conn (get-admin-connection admin-db-spec)]
    (jdbc/execute-one! conn [(str "drop database " dbname)])))


(def ^:private db-spec-defaults {:host     "localhost"
                                 :port     5432
                                 :dbname   "postgres"
                                 :username "postgres"
                                 :password "postgres"
                                 :dbtype   "postgres"})


(defn with-test-db
  ([] (with-test-db nil))
  ([{:keys [db-spec]}]
   (fn [f]
     (let [test-dbname   (str "test_db_" (System/currentTimeMillis))
           admin-db-spec (merge db-spec-defaults db-spec)]
       (create-db admin-db-spec test-dbname)
       (try
         (binding [*test-dbname* test-dbname]
           (f))
         (finally
           (future
             (try
               (drop-db admin-db-spec test-dbname)
               (catch Exception e
                 (println "WARNING: Error while running cleanup on" test-dbname)
                 (println e))))))))))


(defn with-test-fixture [conf]
  (fn [f]
    (let [flyway (flyway/flyway (update conf :db-spec (fn [db-spec]
                                                        (merge db-spec-defaults db-spec {:dbname *test-dbname*}))))]
      (flyway/migrate flyway)
      (println "Migrations:")
      (doseq [[migration-name migration-location] (flyway/info flyway)]
        (println "   " migration-name " (" migration-location ")"))
      (try
        (f)
        (finally
          (when (-> conf :flyway :clean?)
            (flyway/clean flyway)))))))


(defn with-test-datasource
  ([] (with-test-datasource nil))
  ([{:keys [db-spec ds]}]
   (fn [f]
     (let [test-db-spec (-> (merge db-spec-defaults db-spec {:dbname *test-dbname*})
                            (merge ds))]
       (with-open [ds (next.jdbc.connection/->pool HikariDataSource test-db-spec)]
         (with-open [conn (jdbc/get-connection ds)]
           (jdbc/execute-one! conn ["select 1"]))
         (binding [*test-ds* ds]
           (f)))))))
