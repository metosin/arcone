(ns metosin.arcone.util.validate
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [malli.core :as m]
            [malli.transform :as mt]
            [malli.error :as me]))


(def ^:private transformer (mt/default-value-transformer {::mt/add-optional-keys true}))


(defn validate! [schema data]
  (let [data (m/decode schema data transformer)]
    (when-not (m/validate schema data)
      (log/fatal "assertion failed:\n"
                 (m/explain schema data)
                 "\n"
                 (me/humanize (m/explain schema data))
                 "\n  "
                 (str/join "\n   " (->> (Thread/currentThread)
                                        (.getStackTrace)
                                        (drop 1)
                                        (drop-while (fn [^StackTraceElement ste]
                                                      (str/starts-with? (.getClassName ste) "arcone.util.assert")))
                                        (take 5)
                                        (map (fn [^StackTraceElement ste]
                                               (str (.getClassName ste)
                                                    "/"
                                                    (.getMethodName ste)
                                                    " ("
                                                    (.getFileName ste)
                                                    " : "
                                                    (.getLineNumber ste)
                                                    ")"))))))
      (throw (ex-info "assertion failed" {:schema schema
                                          :data   data})))
    data))
