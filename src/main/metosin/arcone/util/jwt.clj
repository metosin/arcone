(ns metosin.arcone.util.jwt
  (:require [buddy.sign.jwt :as jwt])
  (:import (java.time Instant
                      Duration)))


(def ^:private buddy-opts {:alg :hs512})


(defn make-jwt [claims secret max-age-sec]
  (let [now (Instant/now)]
    (jwt/sign (assoc claims
                     :iat now
                     :exp (.plus now (Duration/ofSeconds (int max-age-sec))))
              secret
              buddy-opts)))


(defn open-jwt [jwt secret]
  (try
    (jwt/unsign jwt secret buddy-opts)
    (catch clojure.lang.ExceptionInfo e
      (when-not (-> e (ex-data) :type (= :validation))
        (throw e)))))
