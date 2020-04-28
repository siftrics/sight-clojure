[![Clojars Project](https://img.shields.io/clojars/v/sight.svg)](https://clojars.org/sight) ![Clojure CI](https://github.com/ashwinbhaskar/sight-clojure/workflows/Clojure%20CI/badge.svg) [![codecov](https://codecov.io/gh/ashwinbhaskar/sight-clojure/branch/master/graph/badge.svg)](https://codecov.io/gh/ashwinbhaskar/sight-clojure)

This repository contains the official [Sight API](https://siftrics.com/) Clojure client. The Sight API is a text recognition service.

# Quickstart

1. Add this project as a dependency.

### Leiningen/Boot:

```
[sight "1.1.0"]
```

### Clojure CLI/deps.edn:

```
sight {:mvn/version "1.1.0"}
```

### Gradle

```
compile 'sight:sight:1.1.0
```

### Maven

```
<dependency>
  <groupId>sight</groupId>
  <artifactId>sight</artifactId>
  <version>1.1.0</version>
</dependency>
```

2. Require or import the package. For example, add it to `:require`:

```
(ns my-namespace
  ...
  (:require [sight.core :as sight]))
```

3. Grab an API key from the [Sight dashboard](https://siftrics.com/).
4. Create a client, passing your API key into the constructor, and recognize text:
   - One shot response
        ```
        (let [client (sight/->Client "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
              files  ["/user/foos/dummy.pdf"]]
          (sight/recognize client files))
        ```
        
        Response would look something like this
        ```
        {:pages [{:error "",
                  :file-index 0,
                  :page-number 1,
                  :number-of-pages-in-file 1,
                  :recognized-text [{:top-left-y 193,
                                     :bottom-right-y 243,
                                     :bottom-left-x 152,
                                     :top-right-x 500,
                                     :bottom-left-y 248,
                                     :top-right-y 188,
                                     :top-left-x 151,
                                     :bottom-right-x 501,
                                     :confidence 0.10092532855610954,
                                     :text "Dummy PDF file"}]}]}
   - Stream response
    
        You can also stream the response as they are returned. 
        ```
     (let [client (->Client "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")]
       (->> (recognize-stream client (list "/Users/ashwinbhaskar/Downloads/flight-euro.pdf" "/Users/ashwinbhaskar/Downloads/dummy.pdf" "/Users/ashwinbhaskar/Downloads/flight-euro.pdf"))
            (run! process)))
        ```
     **Caveat** : Exceptions are not thrown if there is a an error fetching some pages. Instead, a `failure`
     is returned [this](https://github.com/adambard/failjure). So your `process` function will have to check for failure
     . As an example
     ```
     (:require [failjure.core :as f])
     
     (defn process [pages]
       (->> pages
            (map (fn [p]
                   (if (f/failed? p)
                     (failure-func (f/message p))
                     (:recognized-text p)))))
     ```
     `pages` looks like this
     ```
      [{:error                   "",
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
     ```

```

`:file-index` is the index of this file in the original request's "files" array.
```

## Word-Level Bounding Boxes

`recognize` has an additional signature with a third parameter, `word-level-bounding-boxes`. If it's `true` then word-level bounding boxes are returned instead of sentence-level bounding boxes. E.g.,

```
(sight/recognize client (list "invoice.pdf" "receipt.png") true)
(sight/recognize client (list "invoice.pdf" "receipt.png") true)
```

## Official API Documentation

Here is the [official documentation for the Sight API](https://siftrics.com/docs/sight.html).

# Apache V2 License

This code is licensed under Apache V2.0. The full text of the license can be found in the "LICENSE" file.

# Lead Maintainer

* [Ashwin Bhaskar](https://github.com/ashwinbhaskar)

# Contributors

* [Siftrics Founder](https://github.com/siftrics/)
* [Ashwin Bhaskar](https://github.com/ashwinbhaskar) 
