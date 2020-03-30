(ns sight.core-test
  (:require [clojure.test :refer :all]
            [sight.core :as core]
            [mock-clj.core :as m]
            [clojure.data.json :as json]
            [sight.utils :as u]
            [camel-snake-kebab.core :as csk]
            [failjure.core :as f]))

(deftest sight-api-200-response-test-one-shot
  (testing "Should return the result when http call gives 200 status code without polling-url"
    (let [client (core/->Client "12345678-1234-1234-1234-123456781234")]
      (m/with-mock [clj-http.client/post    {:status 200
                                             :body   (json/write-str {"RecognizedText" [{"Text"         "Invoice"
                                                                                         "Confidence"   0.22863210084975458
                                                                                         "TopLeftX"     395
                                                                                         "TopLeftY"     35
                                                                                         "TopRightX"    449
                                                                                         "TopRightY"    35
                                                                                         "BottomLeftX"  395
                                                                                         "BottomLeftY"  47
                                                                                         "BottomRightX" 449
                                                                                         "BottomRightY" 47
                                                                                         }]})}
                    u/file-path->base64file "YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA=="]
        (let [result (core/recognize client
                                     (list "/Users/johndoe/Downloads/baz.jpg"))]
          (is (= {:pages [{:error                   ""
                           :file-index              0
                           :page-number             1
                           :number-of-pages-in-file 1
                           :recognized-text         [{:top-left-y     35,
                                                      :bottom-right-y 47,
                                                      :bottom-left-x  395,
                                                      :top-right-x    449,
                                                      :bottom-left-y  47,
                                                      :top-right-y    35,
                                                      :top-left-x     395,
                                                      :bottom-right-x 449,
                                                      :confidence     0.22863210084975458,
                                                      :text           "Invoice"}]}]}
                 result))
          (is (= 1
                 (m/call-count #'u/file-path->base64file)))
          (is (= ["/Users/johndoe/Downloads/baz.jpg"]
                 (m/last-call #'u/file-path->base64file)))
          (is (= 1
                 (m/call-count #'clj-http.client/post)))
          (is (= ["https://siftrics.com/api/sight/" {:headers            {"Authorization" "Basic 12345678-1234-1234-1234-123456781234"}
                                                     :body               "{\"makeSentences\":true,\"files\":[{\"mimeType\":\"image\\/jpg\",\"base64File\":\"YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA==\"}]}"
                                                     :content-type       :json
                                                     :socket-timeout     10000
                                                     :connection-timeout 10000
                                                     :accept             :json}]
                 (m/last-call #'clj-http.client/post)))))))
  (testing "Should return the result when response is 200 and polling url is not null"
    (let [client (core/->Client "12345678-1234-1234-1234-123456781234")]
      (m/with-mock [clj-http.client/post    {:status 200
                                             :body   (json/write-str {"PollingURL" "https://siftrics.com/api/sight/12345678-1234-1234-1234-123456781234"})}
                    clj-http.client/get     {:status 200
                                             :body   (json/write-str {"Pages" [{"Error"               "",
                                                                                "FileIndex"           0,
                                                                                "PageNumber"          1,
                                                                                "NumberOfPagesInFile" 1,
                                                                                "RecognizedText"      [{"Text"         "Invoice"
                                                                                                        "Confidence"   0.22863210084975458
                                                                                                        "TopLeftX"     395
                                                                                                        "TopLeftY"     35
                                                                                                        "TopRightX"    449
                                                                                                        "TopRightY"    35
                                                                                                        "BottomLeftX"  395
                                                                                                        "BottomLeftY"  47
                                                                                                        "BottomRightX" 449
                                                                                                        "BottomRightY" 47
                                                                                                        }]}]})}
                    u/file-path->base64file "YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA=="]
        (let [result (core/recognize client
                                     (list "/Users/johndoe/Downloads/baz.jpg"))]
          (is (= {:pages [{:error                   "",
                           :file-index              0,
                           :page-number             1,
                           :number-of-pages-in-file 1,
                           :recognized-text         [{:top-left-y     35,
                                                      :bottom-right-y 47,
                                                      :bottom-left-x  395,
                                                      :top-right-x    449,
                                                      :bottom-left-y  47,
                                                      :top-right-y    35,
                                                      :top-left-x     395,
                                                      :bottom-right-x 449,
                                                      :confidence     0.22863210084975458,
                                                      :text           "Invoice"}]}]}
                 result))
          (is (= 1
                 (m/call-count #'u/file-path->base64file)))
          (is (= ["/Users/johndoe/Downloads/baz.jpg"]
                 (m/last-call #'u/file-path->base64file)))
          (is (= 1
                 (m/call-count #'clj-http.client/post)))
          (is (= ["https://siftrics.com/api/sight/" {:headers            {"Authorization" "Basic 12345678-1234-1234-1234-123456781234"}
                                                     :body               "{\"makeSentences\":true,\"files\":[{\"mimeType\":\"image\\/jpg\",\"base64File\":\"YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA==\"}]}"
                                                     :content-type       :json
                                                     :socket-timeout     10000
                                                     :connection-timeout 10000
                                                     :accept             :json}]
                 (m/last-call #'clj-http.client/post)))
          (is (= 1
                 (m/call-count #'clj-http.client/get)))
          (is (= ["https://siftrics.com/api/sight/12345678-1234-1234-1234-123456781234"
                  {:headers {"Authorization" "Basic 12345678-1234-1234-1234-123456781234"}}]
                 (m/last-call #'clj-http.client/get))))))))

(deftest sight-api-200-response-test-stream
  (testing "Should return the result when http call gives 200 status code without polling-url when streaming is true"
    (let [client (core/->Client "12345678-1234-1234-1234-123456781234")]
      (m/with-mock [clj-http.client/post    {:status 200
                                             :body   (json/write-str {"RecognizedText" [{"Text"         "Invoice"
                                                                                         "Confidence"   0.22863210084975458
                                                                                         "TopLeftX"     395
                                                                                         "TopLeftY"     35
                                                                                         "TopRightX"    449
                                                                                         "TopRightY"    35
                                                                                         "BottomLeftX"  395
                                                                                         "BottomLeftY"  47
                                                                                         "BottomRightX" 449
                                                                                         "BottomRightY" 47
                                                                                         }]})}
                    u/file-path->base64file "YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA=="]
        (let [result (core/recognize-stream client
                                     (list "/Users/johndoe/Downloads/baz.jpg"))]
          (is (= {:pages [{:error                   ""
                           :file-index              0
                           :page-number             1
                           :number-of-pages-in-file 1
                           :recognized-text         [{:top-left-y     35,
                                                      :bottom-right-y 47,
                                                      :bottom-left-x  395,
                                                      :top-right-x    449,
                                                      :bottom-left-y  47,
                                                      :top-right-y    35,
                                                      :top-left-x     395,
                                                      :bottom-right-x 449,
                                                      :confidence     0.22863210084975458,
                                                      :text           "Invoice"}]}]}
                 result))
          (is (= 1
                 (m/call-count #'u/file-path->base64file)))
          (is (= ["/Users/johndoe/Downloads/baz.jpg"]
                 (m/last-call #'u/file-path->base64file)))
          (is (= 1
                 (m/call-count #'clj-http.client/post)))
          (is (= ["https://siftrics.com/api/sight/" {:headers            {"Authorization" "Basic 12345678-1234-1234-1234-123456781234"}
                                                     :body               "{\"makeSentences\":true,\"files\":[{\"mimeType\":\"image\\/jpg\",\"base64File\":\"YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA==\"}]}"
                                                     :content-type       :json
                                                     :socket-timeout     10000
                                                     :connection-timeout 10000
                                                     :accept             :json}]
                 (m/last-call #'clj-http.client/post)))))))
  (testing "Should return the result when response is 200 and polling url is not null and streaming is true"
    (let [client                   (core/->Client "12345678-1234-1234-1234-123456781234")
          polling-url-call-counter (atom 0)]
      (m/with-mock [clj-http.client/post    {:status 200
                                             :body   (json/write-str {"PollingURL" "https://siftrics.com/api/sight/12345678-1234-1234-1234-123456781234"})}
                    clj-http.client/get     (fn [& _]
                                              (if (= 1
                                                     (swap! polling-url-call-counter inc))
                                                {:status 200
                                                 :body   (json/write-str {"Pages" [{"Error"               "",
                                                                                    "FileIndex"           0,
                                                                                    "PageNumber"          1,
                                                                                    "NumberOfPagesInFile" 2,
                                                                                    "RecognizedText"      [{"Text"         "Invoice"
                                                                                                            "Confidence"   0.22863210084975458
                                                                                                            "TopLeftX"     395
                                                                                                            "TopLeftY"     35
                                                                                                            "TopRightX"    449
                                                                                                            "TopRightY"    35
                                                                                                            "BottomLeftX"  395
                                                                                                            "BottomLeftY"  47
                                                                                                            "BottomRightX" 449
                                                                                                            "BottomRightY" 47
                                                                                                            }]}]})}
                                                {:status 200
                                                 :body   (json/write-str {"Pages" [{"Error"               "",
                                                                                    "FileIndex"           0,
                                                                                    "PageNumber"          2,
                                                                                    "NumberOfPagesInFile" 2,
                                                                                    "RecognizedText"      [{"Text"         "Hi, Hello"
                                                                                                            "Confidence"   0.22863210084975458
                                                                                                            "TopLeftX"     395
                                                                                                            "TopLeftY"     35
                                                                                                            "TopRightX"    449
                                                                                                            "TopRightY"    35
                                                                                                            "BottomLeftX"  395
                                                                                                            "BottomLeftY"  47
                                                                                                            "BottomRightX" 449
                                                                                                            "BottomRightY" 47
                                                                                                            }]}]})}))
                    u/file-path->base64file "YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA=="]
        (let [result (->> (core/recognize-stream client
                                             (list "/Users/johndoe/Downloads/baz.jpg"))
                          (mapcat identity))]
          (is (= [{:error                   "",
                   :file-index              0,
                   :page-number             1,
                   :number-of-pages-in-file 2,
                   :recognized-text         [{:top-left-y     35,
                                              :bottom-right-y 47,
                                              :bottom-left-x  395,
                                              :top-right-x    449,
                                              :bottom-left-y  47,
                                              :top-right-y    35,
                                              :top-left-x     395,
                                              :bottom-right-x 449,
                                              :confidence     0.22863210084975458,
                                              :text           "Invoice"}]}
                  {:error                   "",
                   :file-index              0,
                   :page-number             2,
                   :number-of-pages-in-file 2,
                   :recognized-text         [{:top-left-y     35,
                                              :bottom-right-y 47,
                                              :bottom-left-x  395,
                                              :top-right-x    449,
                                              :bottom-left-y  47,
                                              :top-right-y    35,
                                              :top-left-x     395,
                                              :bottom-right-x 449,
                                              :confidence     0.22863210084975458,
                                              :text           "Hi, Hello"}]}]
                 result))
          (is (= 1
                 (m/call-count #'u/file-path->base64file)))
          (is (= ["/Users/johndoe/Downloads/baz.jpg"]
                 (m/last-call #'u/file-path->base64file)))
          (is (= 1
                 (m/call-count #'clj-http.client/post)))
          (is (= ["https://siftrics.com/api/sight/" {:headers            {"Authorization" "Basic 12345678-1234-1234-1234-123456781234"}
                                                     :body               "{\"makeSentences\":true,\"files\":[{\"mimeType\":\"image\\/jpg\",\"base64File\":\"YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA==\"}]}"
                                                     :content-type       :json
                                                     :socket-timeout     10000
                                                     :connection-timeout 10000
                                                     :accept             :json}]
                 (m/last-call #'clj-http.client/post)))
          (is (= 2
                 (m/call-count #'clj-http.client/get)))
          (is (= ["https://siftrics.com/api/sight/12345678-1234-1234-1234-123456781234"
                  {:headers {"Authorization" "Basic 12345678-1234-1234-1234-123456781234"}}]
                 (m/last-call #'clj-http.client/get)))))))
  (testing "Should get a failure when GET PollingUrl gives a non 200 response with streaming true"
    (let [client                   (core/->Client "12345678-1234-1234-1234-123456781234")
          polling-url-call-counter (atom 0)]
      (m/with-mock [clj-http.client/post    {:status 200
                                             :body   (json/write-str {"PollingURL" "https://siftrics.com/api/sight/12345678-1234-1234-1234-123456781234"})}
                    clj-http.client/get     (fn [& _]
                                              (if (= 1
                                                     (swap! polling-url-call-counter inc))
                                                {:status 200
                                                 :body   (json/write-str {"Pages" [{"Error"               "",
                                                                                    "FileIndex"           0,
                                                                                    "PageNumber"          1,
                                                                                    "NumberOfPagesInFile" 2,
                                                                                    "RecognizedText"      [{"Text"         "Invoice"
                                                                                                            "Confidence"   0.22863210084975458
                                                                                                            "TopLeftX"     395
                                                                                                            "TopLeftY"     35
                                                                                                            "TopRightX"    449
                                                                                                            "TopRightY"    35
                                                                                                            "BottomLeftX"  395
                                                                                                            "BottomLeftY"  47
                                                                                                            "BottomRightX" 449
                                                                                                            "BottomRightY" 47
                                                                                                            }]}]})}
                                                {:status 500
                                                 :body   "Internal Server Error"}))
                    u/file-path->base64file "YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA=="]
        (let [result (core/recognize-stream client
                                            (list "/Users/johndoe/Downloads/baz.jpg"))]
          (is (= [{:error                   "",
                   :file-index              0,
                   :page-number             1,
                   :number-of-pages-in-file 2,
                   :recognized-text         [{:top-left-y     35,
                                              :bottom-right-y 47,
                                              :bottom-left-x  395,
                                              :top-right-x    449,
                                              :bottom-left-y  47,
                                              :top-right-y    35,
                                              :top-left-x     395,
                                              :bottom-right-x 449,
                                              :confidence     0.22863210084975458,
                                              :text           "Invoice"}]}]
                 (first result)))
          (is (= "Non-200 response: 500\nInternal Server Error"
                 (-> result
                     second
                     f/message))))))))


(deftest payload-test
  (testing "Payload when word bounding boxes is false"
    (let [client (core/->Client "12345678-1234-1234-1234-123456781234")]
      (m/with-mock [u/file-path->base64file "YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA=="
                    clj-http.client/post    {:status 200
                                             :body   (-> {:recognized-text [{:top-left-y     35,
                                                                             :bottom-right-y 47,
                                                                             :bottom-left-x  395,
                                                                             :top-right-x    449,
                                                                             :bottom-left-y  47,
                                                                             :top-right-y    35,
                                                                             :top-left-x     395,
                                                                             :bottom-right-x 449,
                                                                             :confidence     0.22863210084975458,
                                                                             :text           "Invoice"}]}
                                                         (json/write-str :key-fn csk/->PascalCaseString))}]
        (is (= {:pages [{:error                   "",
                         :file-index              0,
                         :page-number             1,
                         :number-of-pages-in-file 1,
                         :recognized-text         [{:top-left-y     35,
                                                    :bottom-right-y 47,
                                                    :bottom-left-x  395,
                                                    :top-right-x    449,
                                                    :bottom-left-y  47,
                                                    :top-right-y    35,
                                                    :top-left-x     395,
                                                    :bottom-right-x 449,
                                                    :confidence     0.22863210084975458,
                                                    :text           "Invoice"}]}]}
               (core/recognize client (list "/Users/johndoe/Downloads/baz.jpg"))))
        (is (= 1
               (m/call-count #'clj-http.client/post)))
        (is (= ["https://siftrics.com/api/sight/" {:headers            {"Authorization" "Basic 12345678-1234-1234-1234-123456781234"}
                                                   :body               (json/write-str (core/make-payload (list "/Users/johndoe/Downloads/baz.jpg")
                                                                                                          false))
                                                   :content-type       :json
                                                   :socket-timeout     10000 ;; in milliseconds
                                                   :connection-timeout 10000 ;; in milliseconds
                                                   :accept             :json}]
               (m/last-call #'clj-http.client/post))))))
  (testing "Payload when word bounding boxes is true"
    (let [client (core/->Client "12345678-1234-1234-1234-123456781234")]
      (m/with-mock [u/file-path->base64file "YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA=="
                    clj-http.client/post    {:status 200
                                             :body   (-> {:recognized-text [{:top-left-y     35,
                                                                             :bottom-right-y 47,
                                                                             :bottom-left-x  395,
                                                                             :top-right-x    449,
                                                                             :bottom-left-y  47,
                                                                             :top-right-y    35,
                                                                             :top-left-x     395,
                                                                             :bottom-right-x 449,
                                                                             :confidence     0.22863210084975458,
                                                                             :text           "Invoice"}]}
                                                         (json/write-str :key-fn csk/->PascalCaseString))}]
        (is (= {:pages [{:error                   "",
                         :file-index              0,
                         :page-number             1,
                         :number-of-pages-in-file 1,
                         :recognized-text         [{:top-left-y     35,
                                                    :bottom-right-y 47,
                                                    :bottom-left-x  395,
                                                    :top-right-x    449,
                                                    :bottom-left-y  47,
                                                    :top-right-y    35,
                                                    :top-left-x     395,
                                                    :bottom-right-x 449,
                                                    :confidence     0.22863210084975458,
                                                    :text           "Invoice"}]}]}
               (core/recognize client (list "/Users/johndoe/Downloads/baz.jpg") {:word-level-bounding-boxes? true})))
        (is (= 1
               (m/call-count #'clj-http.client/post)))
        (is (= ["https://siftrics.com/api/sight/" {:headers            {"Authorization" "Basic 12345678-1234-1234-1234-123456781234"}
                                                   :body               (json/write-str (core/make-payload (list "/Users/johndoe/Downloads/baz.jpg")
                                                                                                          true))
                                                   :content-type       :json
                                                   :socket-timeout     10000 ;; in milliseconds
                                                   :connection-timeout 10000 ;; in milliseconds
                                                   :accept             :json}]
               (m/last-call #'clj-http.client/post)))))))

(deftest unsupported-file-extension-test
  (testing "Should throw an exception when the given file-paths have an unsupported extension"
    (let [client (core/->Client "12345678-1234-1234-1234-123456781234")]
      (m/with-mock [clj-http.client/post {:status 200
                                          :body   {:message "success"}}]
        (try
          (core/recognize client
                          (list "/Users/johndoe/Downloads/baz.qux" "/Users/johndoe/Downloads/bax.jpg"))
          (is false "should not reach this line")
          (catch Exception e
            (is (= "invalid file extension; must be one of \".pdf\", \".bmp\", \".gif\", \".jpeg\", \".jpg\", or \".png\""
                   (.getMessage e)))
            (is (not (m/called? #'clj-http.client/post))))))
      (m/with-mock [clj-http.client/post {:status 200
                                          :body   {:message "success"}}]
        (try
          (core/recognize client
                          (list "/Users/johndoe/Downloads/baz.mp4" "/Users/johndoe/Downloads/bax.mp3"))
          (is false "should not reach this line")
          (catch Exception e
            (is (= "invalid file extension; must be one of \".pdf\", \".bmp\", \".gif\", \".jpeg\", \".jpg\", or \".png\""
                   (.getMessage e)))
            (is (not (m/called? #'clj-http.client/post)))))))))

(deftest sight-api-non-200-response-test
  (testing "Should throw an exception when the response status code is non 200"
    (let [client (core/->Client "12345678-1234-1234-1234-123456781234")]
      (m/with-mock [clj-http.client/post {:status 401
                                          :body   {:message "you are not authorized to use this api"}}
                    u/file-path->base64file "YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA=="]
        (try
          (core/recognize client
                          (list "/Users/johndoe/Downloads/baz.jpg" "/Users/johndoe/Downloads/bax.jpg")
                          false)
          (is false "should not reach this line")
          (catch Exception e
            (is (= "Non-200 response: 401\n{:message \"you are not authorized to use this api\"}"
                   (.getMessage e)))
            (is (= 1
                   (m/call-count #'clj-http.client/post)))
            (is (= ["https://siftrics.com/api/sight/" {:accept             :json
                                                       :body               "{\"makeSentences\":true,\"files\":[{\"mimeType\":\"image\\/jpg\",\"base64File\":\"YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA==\"},{\"mimeType\":\"image\\/jpg\",\"base64File\":\"YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA==\"}]}"
                                                       :connection-timeout 10000
                                                       :content-type       :json
                                                       :headers            {"Authorization" "Basic 12345678-1234-1234-1234-123456781234"}
                                                       :socket-timeout     10000}]
                   (m/last-call #'clj-http.client/post))))))
      (m/with-mock [clj-http.client/post {:status 500
                                          :body   {:message "Internal server error"}}
                    u/file-path->base64file "YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA=="]
        (try
          (core/recognize client
                          (list "/Users/johndoe/Downloads/baz.jpg" "/Users/johndoe/Downloads/bax.jpg"))
          (is false "should not reach this line")
          (catch Exception e
            (is (= "Non-200 response: 500\n{:message \"Internal server error\"}"
                   (.getMessage e)))
            (is (= 1
                   (m/call-count #'clj-http.client/post)))
            (is (= ["https://siftrics.com/api/sight/" {:accept             :json
                                                       :body               "{\"makeSentences\":true,\"files\":[{\"mimeType\":\"image\\/jpg\",\"base64File\":\"YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA==\"},{\"mimeType\":\"image\\/jpg\",\"base64File\":\"YWxzZGtmanNhbGtkZmogYWxzZGtmamFzbGtkZmphc2xka2ZqYXNkbGtmaiBhbHNrZGZqbHNhZA==\"}]}"
                                                       :connection-timeout 10000
                                                       :content-type       :json
                                                       :headers            {"Authorization" "Basic 12345678-1234-1234-1234-123456781234"}
                                                       :socket-timeout     10000}]
                   (m/last-call #'clj-http.client/post)))))))))
