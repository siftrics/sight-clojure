(ns sight.core
  (:require [clojure.core.async]
            [clojure.data.json :as json]
            [clojure.java.io]
            [clojure.string]
            [clj-http.client]
            [sight.utils :as u]))

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
  [file-index->pages index]
  (or (>= index (alength file-index->pages))
      (every? #(every? true? %)
              (drop index file-paths->file-entries))))

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
  (let [{:keys [status body]} (clj-http.client/post
                                "https://siftrics.com/api/sight/"
                                {:headers            {"Authorization" (str "Basic " (:apikey client))}
                                 :body               (json/write-str payload)
                                 :content-type       :json
                                 :socket-timeout     10000  ;; in milliseconds
                                 :connection-timeout 10000  ;; in milliseconds
                                 :accept             :json})]
    (if (= 200 status)
      (finalize-recognize-response client (json/read-str body) (count (:files payload)))
      (throw (Exception. (format "Non-200 response: status %d \n body: %s" status body))))))

(defn recognize
  "Recognize text in the given files"
  ([client file-paths] (recognize client file-paths false))
  ([client file-paths word-level-bounding-boxes?]
   (recognize-payload
     client
     (make-payload file-paths word-level-bounding-boxes?))))
