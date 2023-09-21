(ns metosin.arcone.db.hugsql
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [hugsql.core :as hugsql]
            [metosin.arcone.db.jdbc :as jdbc]))


(defonce ^:private domains (atom {}))


(defn- relative-to-current-ns [current-ns-name sql-file]
  (-> (str/replace current-ns-name
                   #"[.-]"
                   {"." "/"
                    "-" "_"})
      (str/split #"/")
      (butlast)
      (concat [sql-file])
      (->> (str/join "/"))))


(defn- get-resource [current-ns-name sql-file]
  (or (io/resource sql-file)
      (io/resource (relative-to-current-ns current-ns-name sql-file))
      (throw (ex-info (str "can't find SQL file: \"" sql-file "\"") {:sql-file sql-file}))))


(defn- load-sql-resource [domain sql-res]
  (-> (hugsql/map-of-sqlvec-fns sql-res {:fn-suffix nil})
      (update-keys (comp (partial keyword domain) name))))


(defn- get-domain-and-sql-resources [current-ns-name sql-files]
  (let [[domain sql-files] (if (keyword? (first sql-files))
                             [(name (first sql-files)) (rest sql-files)]
                             [current-ns-name sql-files])]
    [domain (map (partial get-resource current-ns-name) sql-files)]))


(defn register-sql [& sql-files]
  (let [current-ns-name        (name (ns-name *ns*))
        [domain sql-resources] (get-domain-and-sql-resources current-ns-name sql-files)]
    (->> (map (partial load-sql-resource domain) sql-resources)
         (reduce merge)
         (swap! domains merge))))


(defn- sqlvec [query params]
  (let [sqlvec-fn (-> (get-in @domains [query :fn])
                      (or (throw (ex-info (str "unknown HugSQL query: " (pr-str query))
                                          {:query query}))))]
    (sqlvec-fn params)))


(defn execute! [ds query params]
  (jdbc/execute! ds (sqlvec query params)))


(defn execute-one! [ds query params]
  (jdbc/execute-one! ds (sqlvec query params)))


(comment
  (get-domain-and-sql-resources :foo.bar ["foo.sql"])
  ;
  )