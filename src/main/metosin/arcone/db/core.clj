(ns metosin.arcone.db.core
  (:require [next.jdbc :as jdbc]))


;;
;; Get DB connectable from context. Use transaction, connection, or 
;; datasource.
;;


(defn connectable [ctx]
  (or (-> ctx ::tx)
      (-> ctx ::conn)
      (-> ctx ::ds)))


;;
;; Middleware:
;;


(defn with-ds [handler ds]
  (fn [req]
    (-> (assoc req ::ds ds)
        (handler))))


(defn with-conn [handler]
  (fn [req]
    (with-open [conn (jdbc/get-connection (::ds req))]
      (-> (assoc req ::conn conn)
          (handler)))))


(defn with-tx [handler]
  (fn [req]
    (with-open [conn (jdbc/get-connection (::ds req))]
      (next.jdbc/with-transaction [tx conn]
        (-> (assoc req ::conn conn ::tx tx)
            (handler))))))
