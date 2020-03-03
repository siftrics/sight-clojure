[![Clojars Project](https://img.shields.io/clojars/v/sight.svg)](https://clojars.org/sight)

This repository contains the official [Sight API](https://siftrics.com/) Clojure client. The Sight API is a text recognition service.

# Quickstart

1. Add this project as a dependency.

### Leiningen/Boot:

```
[sight "1.0.0"]
```

### Clojure CLI/deps.edn:

```
sight {:mvn/version "1.0.0"}
```

### Gradle

```
compile 'sight:sight:1.0.0
```

### Maven

```
<dependency>
  <groupId>sight</groupId>
  <artifactId>sight</artifactId>
  <version>1.0.0</version>
</dependency>
```

2. Require or import the package. For example, add it to `:require`:

```
(ns my-namespace
  ...
  (:require ...
            [sight]))
```

3. Grab an API key from the [Sight dashboard](https://siftrics.com/).
4. Create a client, passing your API key into the constructor, and recognize text:

```
(def client (->Client "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"))
(def pages (recognize client (list "invoice.pdf" "receipt.png")))
```

`pages` looks like this:

```
{
      "Pages": [
        {
          "Error": "",
          "FileIndex": 0,
          "PageNumber": 1,
          "NumberOfPagesInFile": 3,
          "RecognizedText": [ ... ]
        },
        ...
      ]
}
```

`FileIndex` is the index of this file in the original request's "files" array.

`RecognizedText` looks like this:

```
    "RecognizedText": [
        {
          "Text": "Invoice",
          "Confidence": 0.22863210084975458
          "TopLeftX": 395,
          "TopLeftY": 35,
          "TopRightX": 449,
          "TopRightY": 35,
          "BottomLeftX": 395,
          "BottomLeftY": 47,
          "BottomRightX": 449,
          "BottomRightY": 47,
        },
        ...
      ]
```

## Word-Level Bounding Boxes

`recognize` has an additional signature with a third parameter, `word-level-bounding-boxes`. If it's `true` then word-level bounding boxes are returned instead of sentence-level bounding boxes. E.g.,

```
(recognize client (list "invoice.pdf" "receipt.png") true)
```

## Official API Documentation

Here is the [official documentation for the Sight API](https://siftrics.com/docs/sight.html).

# Apache V2 License

This code is licensed under Apache V2.0. The full text of the license can be found in the "LICENSE" file.

# Contributors

* [Siftrics Founder](https://github.com/siftrics/)
* [Ashwin Bhaskar](https://github.com/ashwinbhaskar)
