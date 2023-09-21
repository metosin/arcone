(ns metosin.arcone.dev.dev-http-handler.config
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))


;;
;; Serving static content:
;;


(def default-content-types
  (with-open [in (-> (io/resource "metosin/arcone/dev/dev_http_handler/mime-types.edn")
                     (io/reader)
                     (java.io.PushbackReader.))]
    (edn/read in)))


(def ^:dynamic config
  {:proxy         {:host   "localhost"
                   :port   (parse-long (or (System/getenv "PORT") "8080"))
                   :prefix "/api/"}
   :root          "public"
   :index         "index.html"
   :content-types {}})


(comment

  ; Load mime types from Apache repo and generate mime-types.edn file from it.
  ; Generates file to current working directory. Move it to this directory if
  ; you wan't to make it a new mime-type map.

  (require '[clojure.string :as str])

  (def source "http://svn.apache.org/viewvc/httpd/httpd/branches/2.2.x/docs/conf/mime.types?revision=1750837&view=co")

  (with-open [in  (io/reader source)
              out (io/writer "mime-types.edn")]
    (binding [*print-length* nil
              *out*          out]
      (println "{")
      (doseq [[ext mime-type] (->> (line-seq in)
                                   (keep (fn [line]
                                           (let [[_ content-type exts] (re-matches #"^([a-z][a-z0-9+./]+)\s+(.*)$" line)]
                                             (when content-type
                                               [content-type (str/split exts #"\s+")]))))
                                   (mapcat (fn [[content-type exts]]
                                             (map (fn [ext]
                                                    [ext content-type])
                                                  exts)))
                                   (into {}))]
        (println (format "   %-10s  %s"
                         (pr-str ext)
                         (pr-str mime-type))))
      (println "}")))

  (count default-content-types)
  (require 'ring.util.mime-type)
  (count ring.util.mime-type/default-mime-types)
  (->> ring.util.mime-type/default-mime-types
       (keep (fn [[k v]]
               (when-not (= (default-content-types k) v)
                 [k v (default-content-types k)])))
       (vec))
  [["iso" "application/x-iso9660-image" nil]
   ["ttf" "font/ttf" nil]
   ["js" "text/javascript" "application/javascript"]
   ["flv" "video/x-flv" nil]
   ["ts" "video/mp2t" nil]
   ["xml" "text/xml" "application/xml"]
   ["woff2" "font/woff2" nil]
   ["mjs" "text/javascript" nil]
   ["dmg" "application/octet-stream" nil]
   ["flac" "audio/flac" nil]
   ["crt" "application/x-x509-ca-cert" nil]
   ["dll" "application/octet-stream" nil]
   ["xpm" "image/x-xpixmap" nil]
   ["cer" "application/pkix-cert" nil]
   ["class" "application/octet-stream" nil]
   ["tar" "application/x-tar" nil]
   ["deb" "application/x-deb" nil]
   ["xwd" "image/x-xwindowdump" nil]
   ["woff" "font/woff" nil]
   ["lzh" "application/octet-stream" nil]
   ["gz" "application/gzip" nil]
   ["exe" "application/octet-stream" nil]
   ["appcache" "text/cache-manifest" nil]
   ["avi" "video/x-msvideo" nil]
   ["wmv" "video/x-ms-wmv" nil]
   ["xbm" "image/x-xbitmap" nil]
   ["ico" "image/x-icon" nil]
   ["pnm" "image/x-portable-anymap" nil]
   ["xls" "application/vnd.ms-excel" nil]
   '["ppt" "application/vnd.ms-powerpoint" nil]
   ["swf" "application/x-shockwave-flash" nil]
   ["m4v" "video/mp4" nil]
   ["eot" "application/vnd.ms-fontobject" nil]
   ["bin" "application/octet-stream" nil]
   ["crl" "application/pkix-crl" nil]
   ["ras" "image/x-cmu-raster" nil]
   ["rd" "text/plain" nil]
   ["jar" "application/java-archive" nil]
   ["asc" "text/plain" nil]
   ["mpd" "application/dash+xml" nil]
   ["m3u8" "application/x-mpegurl" "application/vnd.apple.mpegurl"]
   ["7z" "application/x-7z-compressed" nil]
   ["bz2" "application/x-bzip" nil]
   ["dart" "application/dart" "application/vnd.dart"]
   ["dvi" "application/x-dvi" nil]
   ["rar" "application/x-rar-compressed" nil]
   ["aac" "audio/aac" nil]
   ["pbm" "image/x-portable-bitmap" nil]
   ["ppm" "image/x-portable-pixmap" nil] ["pgm" "image/x-portable-graymap" nil] ["edn" "application/edn" nil] ["etx" "text/x-setext" nil] ["lha" "application/octet-stream" nil] ["rb" "text/plain" nil] ["dms" "application/octet-stream" nil]]

  ;; => (["iso" "application/x-iso9660-image" nil] ["ttf" "font/ttf" nil] ["js" "text/javascript" "application/javascript"] ["flv" "video/x-flv" nil] ["ts" "video/mp2t" nil] ["xml" "text/xml" "application/xml"] ["woff2" "font/woff2" nil] ["mjs" "text/javascript" nil] ["dmg" "application/octet-stream" nil] ["flac" "audio/flac" nil] ["crt" "application/x-x509-ca-cert" nil] ["dll" "application/octet-stream" nil] ["xpm" "image/x-xpixmap" nil] ["cer" "application/pkix-cert" nil] ["class" "application/octet-stream" nil] ["tar" "application/x-tar" nil] ["deb" "application/x-deb" nil] ["xwd" "image/x-xwindowdump" nil] ["woff" "font/woff" nil] ["lzh" "application/octet-stream" nil] ["gz" "application/gzip" nil] ["exe" "application/octet-stream" nil] ["appcache" "text/cache-manifest" nil] ["avi" "video/x-msvideo" nil] ["wmv" "video/x-ms-wmv" nil] ["xbm" "image/x-xbitmap" nil] ["ico" "image/x-icon" nil] ["pnm" "image/x-portable-anymap" nil] ["xls" "application/vnd.ms-excel" nil] ["ppt" "application/vnd.ms-powerpoint" nil] ["swf" "application/x-shockwave-flash" nil] ["m4v" "video/mp4" nil] ["eot" "application/vnd.ms-fontobject" nil] ["bin" "application/octet-stream" nil] ["crl" "application/pkix-crl" nil] ["ras" "image/x-cmu-raster" nil] ["rd" "text/plain" nil] ["jar" "application/java-archive" nil] ["asc" "text/plain" nil] ["mpd" "application/dash+xml" nil] ["m3u8" "application/x-mpegurl" "application/vnd.apple.mpegurl"] ["7z" "application/x-7z-compressed" nil] ["bz2" "application/x-bzip" nil] ["dart" "application/dart" "application/vnd.dart"] ["dvi" "application/x-dvi" nil] ["rar" "application/x-rar-compressed" nil] ["aac" "audio/aac" nil] ["pbm" "image/x-portable-bitmap" nil] ["ppm" "image/x-portable-pixmap" nil] ["pgm" "image/x-portable-graymap" nil] ["edn" "application/edn" nil] ["etx" "text/x-setext" nil] ["lha" "application/octet-stream" nil] ["rb" "text/plain" nil] ["dms" "application/octet-stream" nil])


  ;
  )
