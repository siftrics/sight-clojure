(ns sight.utils
  (:require [clojure.string :as s]
            [clojure.string :as s])
  (:import
   org.apache.commons.codec.binary.Base64
   (java.io ByteArrayOutputStream)))

(def ^:private extension->mime-type
  {:pdf  "application/pdf"
   :bmp  "image/bmp"
   :gif  "image/gif"
   :jpeg "image/jpeg"
   :jpg  "image/jpg"
   :png  "image/png"})

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing.
  https://stackoverflow.com/questions/23018870/how-to-read-a-whole-binary-file-nippy-into-byte-array-in-clojure"
  [x]
  (with-open [out (ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (.toByteArray out)))

(defn file-path->base64file
  "https://stackoverflow.com/questions/42523024/why-is-image-corrupted-when-converted-to-base64
  https://stackoverflow.com/questions/23018870/how-to-read-a-whole-binary-file-nippy-into-byte-array-in-clojure"
  [file-path]
  (-> file-path
      slurp-bytes
      Base64/encodeBase64
      String.))

(defn file-extension [file-name]
  (-> (s/split file-name #"\.")
      last
      keyword))

(defn file-path->mime-type
  [file-path]
  (-> file-path
      file-extension
      extension->mime-type))

