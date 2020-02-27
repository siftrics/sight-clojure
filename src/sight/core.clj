(ns sight.core
  (:import org.apache.commons.codec.binary.Base64)
  (:require [clojure.core.async]
            [clojure.data.json :as json]
            [clojure.java.io]
            [clojure.string]
            [clj-http.client]))

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

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing.
  https://stackoverflow.com/questions/23018870/how-to-read-a-whole-binary-file-nippy-into-byte-array-in-clojure"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (.toByteArray out)))

(defn filepath->base64file
  "https://stackoverflow.com/questions/42523024/why-is-image-corrupted-when-converted-to-base64
  https://stackoverflow.com/questions/23018870/how-to-read-a-whole-binary-file-nippy-into-byte-array-in-clojure"
  [filepath]
  (String. 
   (Base64/encodeBase64
    (slurp-bytes filepath))))

(defn filepath->mimetype
  [filepath]
  (if (clojure.string/ends-with? filepath ".pdf") "application/pdf"
      (if (clojure.string/ends-with? filepath ".bmp") "image/bmp"
          (if (clojure.string/ends-with? filepath ".gif") "image/gif"
              (if (clojure.string/ends-with? filepath ".jpeg") "image/jpeg"
                  (if (clojure.string/ends-with? filepath ".jpg") "image/jpg"
                      (if (clojure.string/ends-with? filepath ".png") "image/png"
                          (throw (Exception. "invalid file extension; must be one of \".pdf\", \".bmp\", \".gif\", \".jpeg\", \".jpg\", or \".png\"")))))))))

(defn filepath->file-entry
  [filepath]
  (->FileEntry (filepath->mimetype filepath) (filepath->base64file filepath)))

(defn filepaths->file-entries
  [filepaths]
  (if (empty? filepaths) '()
      (apply list (filepath->file-entry (first filepaths)) (filepaths->file-entries (rest filepaths)))))

(defn make-payload
  [filepaths word-level-bounding-boxes]
  (->Payload (not word-level-bounding-boxes) (filepaths->file-entries filepaths)))

(defn handle-poll-page
  [page file-index-2-seen-pages results]
  (let [error (get page "Error")
        file-index (get page "FileIndex")
        page-number (get page "PageNumber")
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
        results (transient {"Pages" []})]
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
    {"Pages" [{"Error" "" "FileIndex" 0 "PageNumber" 1 "NumberOfPagesInFile" 1
               "RecognizedText" (get response "RecognizedText")}]}))

(defn recognize-payload
  [client payload]
  (let [response (clj-http.client/post
                  "https://siftrics.com/api/sight/"
                  {:headers {"Authorization" (str "Basic " (:apikey client))}
                   :body (json/write-str payload)
                   :content-type :json
                   :socket-timeout 10000      ;; in milliseconds
                   :connection-timeout 10000  ;; in milliseconds
                   :accept :json})]
    (if (not= (:status response) 200) (throw (Exception. (str "Non-200 response: " (:status response) "\n" (:body response))))
        (finalize-recognize-response
         client (json/read-str (:body response)) (count (:files payload))))))

(defn recognize
  "Recognize text in the given files"
  ([client filepaths] (recognize client filepaths false))
  ([client filepaths word-level-bounding-boxes]
   (recognize-payload
    client (make-payload filepaths word-level-bounding-boxes))))
