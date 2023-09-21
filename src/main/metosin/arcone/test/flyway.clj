(ns metosin.arcone.test.flyway
  (:import (org.flywaydb.core Flyway)
           (org.flywaydb.core.api MigrationInfo)))


(set! *warn-on-reflection* true)


(defn- string-array ^"[Ljava.lang.String;" [args]
  (into-array String args))


(defn flyway ^Flyway [{:keys [db-spec flyway]}]
  (let [{:keys [host port dbname username password]} db-spec
        {:keys [schemas locations placeholders]}     flyway]
    (-> (Flyway/configure)
        (.dataSource (str "jdbc:postgresql://" host ":" port "/" dbname) username password)
        (.locations (string-array locations))
        (.schemas (string-array schemas))
        (.cleanDisabled false)
        (.placeholders (or placeholders {}))
        (.load))))


(defn migrate [^Flyway flyway]
  (let [result (.migrate flyway)]
    (when-not (.success result)
      (throw (ex-info "DB migration failed" {})))
    result))


(defn info [^Flyway flyway]
  (->> (.info flyway)
       (.all)
       (mapv (fn [^MigrationInfo mi]
               [(.getDisplayName (.getState mi)) (.getPhysicalLocation mi)]))))


(defn clean [^Flyway flyway]
  (let [result (.clean flyway)]
    {:cleaned (.-schemasCleaned result)
     :dropped (.-schemasDropped result)}))

