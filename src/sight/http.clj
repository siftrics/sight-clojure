(ns sight.http
  (:require [camel-snake-kebab.core :as csk]
            [clojure.data.json :as json]
            [failjure.core :as f]
            [clj-http.client]))

(defn get [{api-key :api-key} polling-url throw-exception?]
  (let [{:keys [status body]} (clj-http.client/get polling-url
                                                   {:headers {"Authorization" (str "Basic " api-key)}})]
    (if (= status 200)
      (-> body
          (json/read-str :key-fn csk/->kebab-case-keyword)
          :pages)
      (if throw-exception?
        (throw (Exception. (str "Non-200 response: " status "\n" body)))
        (f/fail (str "Non-200 response: " status "\n" body))))))

(defn post [{api-key :api-key} payload throw-exception?]
  (let [{:keys [status body]} (clj-http.client/post
                               "https://siftrics.com/api/sight/"
                               {:headers            {"Authorization" (str "Basic " api-key)}
                                :body               (json/write-str payload)
                                :content-type       :json
                                :socket-timeout     10000  ;; in milliseconds
                                :connection-timeout 10000  ;; in milliseconds
                                :accept             :json})]
    (if (= 200 status)
      (-> body
          (json/read-str :key-fn csk/->kebab-case-keyword))
      (if throw-exception?
        (throw (Exception. (str "Non-200 response: " status "\n" body)))
        (f/fail (format "Non-200 response: status %d \n body: %s" status body))))))