(ns metosin.arcone.http.static-content
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [ring.util.mime-type]
            [integrant.core :as ig])
  (:import (java.io File)
           (java.nio.file Path)
           (java.util.zip Adler32)))


(set! *warn-on-reflection* true)


(def default-mime-types (merge ring.util.mime-type/default-mime-types
                               {"js"   "application/javascript; charset=UTF-8"
                                "html" "text/html; charset=UTF-8"
                                "css"  "text/css; charset=UTF-8"
                                "json" "application/json; charset=UTF-8"}))


(defn- list-files [^File dir]
  (mapcat (fn [^File f]
            (when-not (str/starts-with? (.getName f) ".")
              (if (.isDirectory f)
                (list-files f)
                [f])))
          (.listFiles dir)))


(defn- file-name-ext [fname]
  (let [dot (str/last-index-of fname ".")]
    (when dot
      (subs fname (inc dot)))))


(defn- file-etag [^File f]
  (let [hash   (Adler32.)
        buffer (byte-array 4096)]
    (with-open [i (io/input-stream f)]
      (loop [c (.read i buffer)]
        (when (pos? c)
          (.update hash buffer 0 c)
          (recur (.read i buffer)))))
    (format "%08x" (.getValue hash))))


(defn- versioned? [^File resource]
  (some? (re-find #"\.\d+\." (.getName resource))))


(defn- file->desc [mime-types ^Path root-path ^File resource]
  (let [uri (str "/" (.relativize root-path (.toPath resource)))
        gz? (str/ends-with? uri ".gz")
        uri (if gz? (subs uri 0 (- (count uri) 3)) uri)]
    [uri {:file     resource
          :modified (.lastModified resource)
          :headers  {"content-length"   (.length resource)
                     "content-encoding" (if gz? "gzip" "identity")
                     "content-type"     (mime-types (file-name-ext uri) "application/octet-stream")
                     "cache-control"    (if (versioned? resource)
                                          "public, max-age=604800, immutable"
                                          "no-cache")
                     "etag"             (file-etag resource)}}]))


(defn- load-resources [mime-types ^File root-dir]
  (let [resources (->> (list-files root-dir)
                       (map (partial file->desc mime-types (.toPath root-dir)))
                       (into {}))]
    (if-let [index-html (resources "/index.html")]
      (assoc resources "/" index-html)
      resources)))


(comment
  (get (load-resources default-mime-types (io/file "public")) "/"))


(defn- refresh-resource-desc [dev-key resource-desc]
  (let [^File resource (:file resource-desc)]
    (-> resource-desc
        (assoc :modified (.lastModified resource))
        (update :headers assoc
                "content-length" (.length resource)
                "etag"           (str (file-etag resource) dev-key)))))


(defn- make-get-resource-dev [initial-resources]
  (let [dev-key   (str "-" (name (gensym)))
        resources (atom (reduce-kv (fn [acc k v]
                                     (assoc acc k (update-in v [:headers "etag"] str dev-key)))
                                   {}
                                   initial-resources))]
    (fn [uri]
      (when-let [resource (@resources uri)]
        (if (= (.lastModified ^File (:file resource))
               (:modified resource))
          resource
          (let [resource (refresh-resource-desc dev-key resource)]
            (swap! resources assoc uri resource)
            resource))))))


(defmethod ig/init-key ::resources [_ {:keys [root mime-types mime-type-overrides]}]
  (load-resources (merge (or mime-types default-mime-types) mime-type-overrides)
                  (io/file (if (str/ends-with? root "/")
                             (subs root 0 (dec (count root)))
                             root))))


(defmethod ig/init-key ::handler [_ {:keys [mode security-headers resources]}]
  (let [get-resource (case mode
                       :prod resources
                       :dev (make-get-resource-dev resources))]
    (fn [req]
      (when-let [resource (and (#{:get :head} (:request-method req))
                               (get-resource (:uri req)))]
        (let [match? (= (-> req :headers (get "if-none-match"))
                        (-> resource :headers (get "etag")))]
          {:status  (if match? 304 200)
           :headers (merge security-headers (:headers resource))
           :body    (if (or match? (= (:request-method req) :head))
                      nil
                      (:file resource))})))))

(comment


  (def resources (load-resources default-mime-types
                                 (io/file "public")))

  (resources "/styles.css")
  ;; =>  {:file    #object[java.io.File 0xb847570 "public/styles.css"]
  ;;      :headers {"content-length"   12823
  ;;                "content-encoding" "identity"
  ;;                "content-type"     "text/css"
  ;;                "cache-control"    "no-cache"
  ;;                "etag"             "453734b3"}}

  (resources "/js/alpinejs.3.12.3.min.js"))
  ;; =>  {:file    #object[java.io.File 0x6d9ab6c5 "public/js/alpinejs.3.12.3.min.js.gz"]
  ;;      :headers {"content-length"   15590
  ;;                "content-encoding" "gzip"
  ;;                "content-type"     "application/javascript"
  ;;                "cache-control"    "public, max-age=604800, immutable"
  ;;                "etag"             "0c3f6483"}} 
  