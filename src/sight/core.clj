(ns sight.core
  (:import org.apache.commons.codec.binary.Base64
           (java.io ByteArrayOutputStream))
  (:require [clojure.core.async]
            [clojure.data.json :as json]
            [clojure.java.io]
            [clojure.string]
            [clj-http.client]
            [clojure.string :as s]))

(defrecord Client [apikey])
(defrecord Result [pages])
(defrecord RecognizedPage
  [error fileIndex pageNumber numberOfPagesInFile recognizedText])
(defrecord RecognizedText
  [text confidence
   topLeftX topLeftY topRightX topRightY
   bottomLeftX bottomLeftY bottomRightX bottomRightY])

(defrecord Payload [makeSentences files])
(defrecord FileEntry [mimeType base64File])

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

(defn- file-extension [file-name]
  (-> (s/split file-name #"\.")
      last
      keyword))

(defn file-path->mimetype
  [file-path]
  (if-let [mime-type (-> file-path
                         file-extension
                         extension->mime-type)]
    mime-type
    (throw (Exception. "invalid file extension; must be one of \".pdf\", \".bmp\", \".gif\", \".jpeg\", \".jpg\", or \".png\""))))

(defn file-path->file-entry
  [filepath]
  (->FileEntry (file-path->mimetype filepath)
               (file-path->base64file filepath)))

(defn file-paths->file-entries
  [file-paths]
  (if (empty? file-paths)
    []
    (map file-path->file-entry file-paths)))

(defn make-payload
  [file-paths word-level-bounding-boxes]
  (->Payload (not word-level-bounding-boxes)
             (file-paths->file-entries file-paths)))

(defn handle-poll-page
  [page file-index-2-seen-pages results]
  (let [error                   (get page "Error")
        file-index              (get page "FileIndex")
        page-number             (get page "PageNumber")
        number-of-pages-in-file (get page "NumberOfPagesInFile")]
    (do
      (assoc! results "Pages" (conj (get results "Pages") page))
      (if (not (clojure.string/blank? error))
        (let [newarr (make-array Boolean/TYPE 1)]
          (do
            (aset newarr 0 true)
            (aset file-index-2-seen-pages file-index newarr)))
        (do
          (if (empty? (aget file-index-2-seen-pages file-index))
            (aset file-index-2-seen-pages file-index (make-array Boolean/TYPE number-of-pages-in-file))
            ())
          (aset file-index-2-seen-pages file-index (dec page-number) true))))))

(defn handle-poll-pages
  [pages file-index-2-seen-pages results]
  (if (empty? pages) ()
                     (do
                       (handle-poll-page (first pages) file-index-2-seen-pages results)
                       (recur (rest pages) file-index-2-seen-pages results))))

(defn seen-all-pages?
  [file-index-2-pages index]
  (if (>= index (alength file-index-2-pages))
    true
    (let [arr (aget file-index-2-pages index)]
      (and (> (alength arr) 0) (every? true? arr) (recur file-index-2-pages (inc index))))))

(defn do-poll
  [client polling-url num-files]
  (let [file-index-2-seen-pages (make-array Boolean/TYPE num-files 0)
        results                 (transient {"Pages" []})]
    (while (not (seen-all-pages? file-index-2-seen-pages 0))
      (let [response (clj-http.client/get polling-url {:headers {"Authorization" (str "Basic " (:apikey client))}})]
        (do
          (if (not= (:status response) 200)
            (throw (Exception. (str "Non-200 response: " (:status response) "\n" (:body response))))
            (handle-poll-pages (get (json/read-str (:body response)) "Pages")
                               file-index-2-seen-pages results))))
      (Thread/sleep 500))
    (persistent! results)))

(defn finalize-recognize-response
  [client response num-files]
  (if (not (nil? (get response "PollingURL")))
    (do-poll client (get response "PollingURL") num-files)
    {"Pages" [{"Error"          "" "FileIndex" 0 "PageNumber" 1 "NumberOfPagesInFile" 1
               "RecognizedText" (get response "RecognizedText")}]}))

(defn recognize-payload
  [client payload]
  (let [response (clj-http.client/post
                   "https://siftrics.com/api/sight/"
                   {:headers            {"Authorization" (str "Basic " (:apikey client))}
                    :body               (json/write-str payload)
                    :content-type       :json
                    :socket-timeout     10000               ;; in milliseconds
                    :connection-timeout 10000               ;; in milliseconds
                    :accept             :json})]
    (if (not= (:status response) 200) (throw (Exception. (str "Non-200 response: " (:status response) "\n" (:body response))))
                                      (finalize-recognize-response
                                        client (json/read-str (:body response)) (count (:files payload))))))

(defn recognize
  "Recognize text in the given files"
  ([client file-paths] (recognize client file-paths false))
  ([client file-paths word-level-bounding-boxes]
   (recognize-payload
     client
     (make-payload file-paths word-level-bounding-boxes))))
