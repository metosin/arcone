(ns metosin.arcone.db.honey
  (:require [metosin.arcone.db.jdbc :as jdbc]
            [honey.sql :as sql]))


(defn execute! [ds query]
  (jdbc/execute! ds (sql/format query)))


(defn execute-one! [ds query]
  (jdbc/execute-one! ds (sql/format query)))
