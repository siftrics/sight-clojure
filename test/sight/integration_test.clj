(ns sight.integration-test
  (:require [clojure.test :refer :all]
            [sight.core :as core]
            [clojure.java.io :as io]))

(deftest test-sanity
  (testing "Should recognize all the words"
    (let [client   (core/->Client (System/getenv "API_KEY"))
          resource (io/resource "dummy.pdf")
          files    (list (-> resource
                             .getPath))]
      (is (= "Dummy PDF file"
             (-> (core/recognize client files)
                 :pages
                 first
                 :recognized-text
                 first
                 :text)))
      (is (= "Dummy PDF file"
             (->> (core/recognize-stream client files)
                  (mapcat identity)
                  first
                  :recognized-text
                  first
                  :text))))))
