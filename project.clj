(defproject sight "1.0.0"
  :description "Official Clojure client for the Sight API, a text recognition service"
  :url "https://github.com/siftrics/sight-clojure"
  :license {:name "Apache-2.0"
            :url  "https://www.apache.org/licenses/LICENSE-2.0.txt"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "1.0.567"]
                 [org.clojure/data.json "1.0.0"]
                 [clj-http "3.10.0"]
                 [commons-codec/commons-codec "1.4"]
                 [mock-clj "0.2.1"]]
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :repl-options {:init-ns sight.core})
