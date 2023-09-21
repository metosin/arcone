(ns metosin.arcone.config
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [aero.core :as aero]))


(defmethod aero/reader 'file [_ _ filename]
  (let [f (io/file filename)]
    (when-not (.canRead f)
      (throw (ex-info (str "can't read from file \"" filename "\"") {:filename filename})))
    (str/trim (slurp f))))


(defn load-config [mode]
  (-> "config.edn"
      (clojure.java.io/resource)
      (aero/read-config {:profile mode})))


(comment
  (load-config :dev)
  ;
  )