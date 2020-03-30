[![Clojars Project](https://img.shields.io/clojars/v/sight.svg)](https://clojars.org/sight) ![Clojure CI](https://github.com/ashwinbhaskar/sight-clojure/workflows/Clojure%20CI/badge.svg)

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

```

`:file-index` is the index of this file in the original request's "files" array.

## Word-Level Bounding Boxes

`recognize` has an additional signature with a third parameter, `word-level-bounding-boxes`. If it's `true` then word-level bounding boxes are returned instead of sentence-level bounding boxes. E.g.,

```
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
