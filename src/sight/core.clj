;; Copyright © 2020 Siftrics
;;
;; Permission is hereby granted, free of charge, to any person obtaining a copy
;; of this software and associated documentation files (the "Software"), to deal
;; in the Software without restriction, including without limitation the rights
;; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
;; copies of the Software, and to permit persons to whom the Software is
;; furnished to do so, subject to the following conditions:
;;
;; The above copyright notice and this permission notice shall be included in
;; all copies or substantial portions of the Software.
;;
;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
;; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
;; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
;; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
;; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
;; THE SOFTWARE.

(ns sight.core
  (:require [clojure.java.io]
            [clojure.string]
            [sight.utils :as u]
            [failjure.core :as f]
            [sight.http :as http]))

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

(defn- file-path->file-entry
  [file-path throw-exception?]
  (let [mime-type (u/file-path->mime-type file-path)]
    (if mime-type
      (->FileEntry mime-type (u/file-path->base64file file-path))
      (if throw-exception?
        (throw (Exception. "invalid file extension; must be one of \".pdf\", \".bmp\", \".gif\", \".jpeg\", \".jpg\", or \".png\""))
        (f/fail "invalid file extension; must be one of \".pdf\", \".bmp\", \".gif\", \".jpeg\", \".jpg\", or \".png\"")))))

(defn- file-paths->file-entries
  [file-paths throw-exception?]
  (let [result (map #(file-path->file-entry % throw-exception?) file-paths)]
    (f/if-let-failed? [failure (some (fn [r] (when (f/failed? r) r)) result)]
      failure
      result)))

(defn make-payload
  [file-paths word-level-bounding-boxes throw-exception?]
  (f/if-let-ok? [file-entries (file-paths->file-entries file-paths throw-exception?)]
    (->Payload (not word-level-bounding-boxes) file-entries)))

(defn- mark-page-as-seen!
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

(defn- mark-pages-as-seen!
  [pages file-index->seen-pages results]
  (doseq [page pages]
    (mark-page-as-seen! page file-index->seen-pages results)))

(defn- seen-all-pages?
  [file-index->pages]
  (every? #(and (seq %)
                (every? true? %))
          file-index->pages))

(defn- poll
  [client polling-url num-files stream?]
  (let [file-index->seen-pages (make-array Boolean/TYPE num-files 0)
        results (transient {:pages []})
        failure-count (atom 0)
        fetch (fn []
                (Thread/sleep 500)
                (http/get client polling-url false))]
    (if stream?
      (->> (repeatedly fetch)
           (take-while (fn [pages]
                         (if (f/failed? pages)
                           (= 1 (swap! failure-count inc))
                           (if (seen-all-pages? file-index->seen-pages)
                             false
                             (do
                               (mark-pages-as-seen! pages file-index->seen-pages results)
                               true)))))
           (filter (comp not empty?)))
      (do
        (while (not (seen-all-pages? file-index->seen-pages))
          (let [pages (http/get client polling-url true)]
            (mark-pages-as-seen! pages file-index->seen-pages results))
          (Thread/sleep 500))
        (persistent! results)))))

(defn- recognize-payload
  [client {:keys [polling-url recognized-text]} num-files stream?]
  (if polling-url
    (poll client polling-url num-files stream?)
    (if stream?
      (lazy-seq [[{:error                   ""
                   :file-index              0
                   :page-number             1
                   :number-of-pages-in-file 1
                   :recognized-text         recognized-text}]])
      {:pages [{:error                   ""
                :file-index              0
                :page-number             1
                :number-of-pages-in-file 1
                :recognized-text         recognized-text}]})))

(defn recognize
  "Recognize text in the given files"
  ([client file-paths] (recognize client file-paths {}))
  ([client file-paths {:keys [stream? word-level-bounding-boxes?] :as opts}]
   (let [payload (make-payload file-paths word-level-bounding-boxes? true)
         result (http/post client payload true)]
     (recognize-payload
      client
      result
      (count file-paths)
      false))))

(defn recognize-stream
  "Recognize text in the given files and return it as a lazy sequence"
  ([client file-paths] (recognize-stream client file-paths false))
  ([client file-paths word-level-bounding-boxes?]
   (f/attempt-all [payload (make-payload file-paths word-level-bounding-boxes? false)
                   result (http/post client payload false)]
     (recognize-payload
      client
      result
      (count file-paths)
      true))))
