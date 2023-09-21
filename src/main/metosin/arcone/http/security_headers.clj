(ns metosin.arcone.http.security-headers
  (:require [clojure.string :as str]
            [integrant.core :as ig]))


; see: https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html#content-security-policy-csp
;      https://cheatsheetseries.owasp.org/cheatsheets/Content_Security_Policy_Cheat_Sheet.html
;      https://owasp.org/www-project-secure-headers/


(def ^:private content-security-policy "content-security-policy")
(def ^:private content-security-policy-report-only "content-security-policy-report-only")


(def ^:private default-security-headers
  (let [self "'self'"
        none "'none'"]
    {"x-content-type-options"              "nosniff"
     "x-frame-options"                     "DENY"
     "referrer-policy"                     "strict-origin-when-cross-origin"
     "cross-origin-opener-policy"          "same-origin"
     "cross-origin-resource-policy"        "same-site"
     "cross-origin-embedder-policy"        "require-corp"
     content-security-policy             {"default-src"     self
                                          "frame-ancestors" none}
     content-security-policy-report-only {"default-src"     none
                                          "frame-ancestors" self
                                          "form-action"     self
                                          "script-src"      self
                                          "connect-src"     self
                                          "img-src"         self
                                          "style-src"       self
                                          "font-src"        self
                                          "manifest-src"    self}}))


(defmethod ig/init-key ::security-headers [_ {:keys [https? google-fonts?]}]
  (let [security-headers (cond-> default-security-headers
                           https? (-> (assoc "strict-transport-security" "max-age=86400; includeSubDomains; preload")
                                      (update content-security-policy assoc "upgrade-insecure-requests" "")
                                      (update content-security-policy-report-only assoc "upgrade-insecure-requests" ""))
                           google-fonts? (-> (update content-security-policy assoc
                                                     "style-src" "'self' fonts.googleapis.com"
                                                     "font-src"  "fonts.gstatic.com")
                                             (update content-security-policy-report-only assoc
                                                     "style-src" "'self' fonts.googleapis.com"
                                                     "font-src"  "fonts.gstatic.com")))]
    (reduce-kv (fn [acc k v]
                 (assoc acc k (if (map? v)
                                (->> v
                                     (map (fn [[k v]] (str k " " v)))
                                     (str/join "; "))
                                v)))
               {}
               security-headers)))


(defmethod ig/init-key ::middleware [_ {:keys [security-headers]}]
  (fn [handler]
    (fn [req]
      (let [resp (handler req)]
        (if (and (-> req :headers (get "hx-request") (not= "true"))
                 (-> resp :headers (get "content-type" "") (str/starts-with? "text/html")))
          (update resp :headers merge security-headers)
          resp)))))
