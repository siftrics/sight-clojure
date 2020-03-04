(ns sight.core
  (:require [clojure.core.async]
            [clojure.data.json :as json]
            [clojure.java.io]
            [clojure.string]
            [clj-http.client]
            [sight.utils :as u]
            [camel-snake-kebab.core :as csk]))

(defrecord Client [api-key])
(defrecord Result [pages])
(defrecord RecognizedPage
  [error fileIndex pageNumber numberOfPagesInFile recognizedText])
(defrecord RecognizedText
  [text confidence
   topLeftX topLeftY topRightX topRightY
   bottomLeftX bottomLeftY bottomRightX bottomRightY])

(defrecord Payload [makeSentences files])
(defrecord FileEntry [mimeType base64File])

(defn file-path->file-entry
  [filepath]
  (->FileEntry (if-let [mime-type (u/file-path->mimetype filepath)]
                 mime-type
                 (throw (Exception. "invalid file extension; must be one of \".pdf\", \".bmp\", \".gif\", \".jpeg\", \".jpg\", or \".png\"")))
               (u/file-path->base64file filepath)))

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
  [{:keys [error file-index page-number number-of-pages-in-file] :as page} file-index->seen-pages results]
  (assoc! results :pages (conj (:pages results) page))
  (if (not (clojure.string/blank? error))
    (let [new-arr (make-array Boolean/TYPE 1)]
      (aset new-arr 0 true)
      (aset file-index->seen-pages file-index new-arr))
    (do
      (when (empty? (aget file-index->seen-pages file-index))
        (aset file-index->seen-pages file-index (make-array Boolean/TYPE number-of-pages-in-file)))
      (aset file-index->seen-pages file-index (dec page-number) true))))


(defn handle-poll-pages
  [pages file-index->seen-pages results]
  (doseq [page pages]
    (handle-poll-page page file-index->seen-pages results)))

(defn seen-all-pages?
  [file-index->pages]
  (every? #(and (seq %)
                (every? true? %))
          file-index->pages))

(defn do-poll
  [{api-key :api-key} polling-url num-files]
  (let [file-index->seen-pages (make-array Boolean/TYPE num-files 0)
        results                (transient {:pages []})]
    (while (not (seen-all-pages? file-index->seen-pages))
      (let [{:keys [status body]} (clj-http.client/get polling-url
                                                       {:headers {"Authorization" (str "Basic " api-key)}})]
        (if (not= status 200)
          (throw (Exception. (str "Non-200 response: " status "\n" body)))
          (handle-poll-pages (-> body
                                 (json/read-str :key-fn csk/->kebab-case-keyword)
                                 :pages)
                             file-index->seen-pages results)))
      (Thread/sleep 500))
    (persistent! results)))

(defn finalize-recognize-response
  [client {:keys [polling-url recognized-text]} num-files]
  (if (not (nil? polling-url))
    (do-poll client polling-url num-files)
    {:pages [{:error                   ""
              :file-index              0
              :page-number             1
              :number-of-pages-in-file 1
              :recognized-text         recognized-text}]}))

(defn recognize-payload
  [{api-key :api-key :as client} {files :files :as payload}]
  (let [{:keys [status body]} (clj-http.client/post
                                "https://siftrics.com/api/sight/"
                                {:headers            {"Authorization" (str "Basic " api-key)}
                                 :body               (json/write-str payload)
                                 :content-type       :json
                                 :socket-timeout     10000  ;; in milliseconds
                                 :connection-timeout 10000  ;; in milliseconds
                                 :accept             :json})]
    (if (= 200 status)
      (finalize-recognize-response client
                                   (-> body
                                       (json/read-str :key-fn csk/->kebab-case-keyword))
                                   (count files))
      (throw (Exception. (format "Non-200 response: status %d \n body: %s" status body))))))

(defn recognize
  "Recognize text in the given files"
  ([client file-paths] (recognize client file-paths false))
  ([client file-paths word-level-bounding-boxes?]
   (recognize-payload
     client
     (make-payload file-paths word-level-bounding-boxes?))))
