;; Copyright (C) 2015 longfin
;;
;; This software may be modified and distributed under the terms
;; of the MIT license.  See the LICENSE file for details.


(ns sory.codec
  "Define codec interface to en/decode sound relative data.

  TODO: write overall structure.
  "
  {:doc/format :markdown}

  (:require [cljs.core.async :refer [<! put! chan]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [sory.macros :refer [dbg]]))


(def
  ^{:doc "frequency lower boundary."}
  low-bits-offset 17500)
(def
  ^{:doc "frequency higher boundary."}
  high-bits-offset 18500)


(defn- normalize-freq
  "Normalize frequency by specific value."
  {:doc/format :markdown}

  [freq]
  (dbg freq)
  (let [ifreq (js/Math.round freq)
        m (mod ifreq 50)]
    (if (< m 25)
      (- ifreq m)
      (+ ifreq (- 50 m)))))


(defn- encode-bits
  "Encode bit array to frequency value."
  {:doc/format :markdown}

  [bs offset]
  (dbg (-> bs (* 50) (+ offset))))


(defn- decode-bits
  "Decode frequency to bit array."
  {:doc/format :markdown}

  [freq offset]
  (-> freq (- offset) (/ 50)))


(defn- validate-and-decode-bits
  "Decode frequency to bit array if available."
  {:doc/format :markdown}

  [freq high-bits?]
  (if high-bits?
    (when (>= freq high-bits-offset)
      (decode-bits freq high-bits-offset))
    (when (and (<= low-bits-offset freq)
               (<= freq high-bits-offset))
      (decode-bits freq low-bits-offset))))


(defn encode
  "Encode given sequable to frequencies as lazy sequence.

  it returns an interleaved bit having the structure shown below...


  [\\a \\b \\c ...] => [freq(higher 4bit of \\a),
                        freq(lower 4bit of \\a),
                        freq(hihger 4bit of \\b),
                        freq(lower 4bit of \\b),
                        freq(higher 4bit of \\c),
                        freq(lower 4bit of \\c),
                        ...]


  Usages:

  ```
  (encode \"some value\")

  or

  (encode `(\\a \\b \\c))
  ```
  "
  {:doc/format :markdown}

  [s]
  (let [bs (map #(.charCodeAt %) s)]
    (interleave
     (map #(-> %
               (bit-and 0xF0)
               (bit-shift-right 4)
               (encode-bits high-bits-offset))
          bs)
     (map #(-> %
               (bit-and 0x0F)
               (encode-bits low-bits-offset))
          bs))))


(defn <decode
  "Receive frequency stream channel and return channel that stream decoded value.

  Usages:
  ```
  (let [c (<listen ctx)
        decoded-chan (<decoed c)]
    (go-loop []
      (let [decoded-val (<! decoed-chan)]
      (println decoded-val))))
  ```
  "
  {:doc/format :markdown}

  [channel]
  (let [c (chan)]
    (go-loop [prev nil]
      (let [freq (normalize-freq (<! channel))]
        (when-let [bits (dbg (validate-and-decode-bits freq (nil? prev)))]
          (if (nil? (dbg prev))
            (recur bits)
            (let [prev-bits (bit-shift-left prev 4)
                  decoded (js/String.fromCharCode
                           (bit-or prev-bits bits))]
              (put! c (dbg decoded)))))
        (recur nil)))
    c))
