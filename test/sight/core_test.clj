(ns sight.core-test
  (:require [clojure.test :refer :all]
            [sight.core :as core]
            [mock-clj.core :as m]
            [clojure.data.json :as json]))

(deftest sight-ap-non-200-response-test
  (testing "Should throw an exception when the response status code is non 200"
    (let [client (core/->Client "12345678-1234-1234-1234-123456781234")]
      (m/with-mock [clj-http.client/post {:status 401
                                          :body   {:message "you are not authorized to use this api"}}
                    core/make-payload "dummy-payload"]
        (try
          (core/recognize client
                          (list "/Users/johndoe/Downloads/baz.jpg" "/Users/johndoe/Downloads/bax.jpg"))
          (is false "should not reach this line")
          (catch Exception e
            (is (= (format "Non-200 response: status %d \n body: %s" 401 {:message "you are not authorized to use this api"})
                   (.getMessage e)))
            (is (= 1
                   (m/call-count #'clj-http.client/post)))
            (is (= ["https://siftrics.com/api/sight/" {:headers            {"Authorization" (str "Basic " (:apikey client))}
                                                       :body               (json/write-str "dummy-payload")
                                                       :content-type       :json
                                                       :socket-timeout     10000
                                                       :connection-timeout 10000
                                                       :accept             :json}]
                   (m/last-call #'clj-http.client/post)))
            (is (= 1
                   (m/call-count #'core/make-payload)))
            (is (= [(list "/Users/johndoe/Downloads/baz.jpg" "/Users/johndoe/Downloads/bax.jpg") false]
                   (m/last-call #'core/make-payload))))))
      (m/with-mock [clj-http.client/post {:status 500
                                          :body   {:message "Internal server error"}}
                    core/make-payload "dummy-payload"]
        (try
          (core/recognize client
                          (list "/Users/johndoe/Downloads/baz.jpg" "/Users/johndoe/Downloads/bax.jpg"))
          (is false "should not reach this line")
          (catch Exception e
            (is (= (format "Non-200 response: status %d \n body: %s" 500 {:message "Internal server error"})
                   (.getMessage e)))
            (is (= 1
                   (m/call-count #'clj-http.client/post)))
            (is (= ["https://siftrics.com/api/sight/" {:headers            {"Authorization" (str "Basic " (:apikey client))}
                                                       :body               (json/write-str "dummy-payload")
                                                       :content-type       :json
                                                       :socket-timeout     10000
                                                       :connection-timeout 10000
                                                       :accept             :json}]
                   (m/last-call #'clj-http.client/post)))
            (is (= 1
                   (m/call-count #'core/make-payload)))
            (is (= 1
                   (m/call-count #'core/make-payload)))))))))
