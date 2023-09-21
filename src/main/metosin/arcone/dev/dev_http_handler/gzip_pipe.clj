(ns metosin.arcone.dev.dev-http-handler.gzip-pipe)


(defn gzip-pipe ^java.io.InputStream [^java.io.InputStream in]
  (let [pin  (java.io.PipedInputStream. 8192)
        pout (java.io.PipedOutputStream. pin)
        gout (java.util.zip.GZIPOutputStream. pout)]
    (.start (Thread/ofVirtual) (fn []
                                 (let [buffer (byte-array 8192)]
                                   (loop []
                                     (let [c (.read in buffer)]
                                       (if (pos? c)
                                         (do (.write gout buffer 0 c)
                                             (recur))
                                         (do (.close in)
                                             (.flush gout)
                                             (.close gout)
                                             (.close pin))))))))
    pin))


(comment
  (future
    (let [in  (java.io.ByteArrayInputStream. (.getBytes "Hello, world!"))
          in2 (gzip-pipe in)
          in3 (java.util.zip.GZIPInputStream. in2)]
      (println (format "DATA: [%s]" (slurp in3))))))