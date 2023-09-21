(ns metosin.arcone.db.jdbc
  (:require [next.jdbc]))


(defn execute!
  ([ds sqlvec]
   (execute! ds sqlvec nil))
  ([ds sqlvec opts]
   (next.jdbc/execute! ds sqlvec opts)))


(defn execute-one!
  ([ds sqlvec]
   (execute-one! ds sqlvec nil))
  ([ds sqlvec opts]
   (next.jdbc/execute-one! ds sqlvec opts)))
