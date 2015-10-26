(ns sory.codec
  (:require [cljs.core.async :refer [<! put! chan]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


(def low-bits-offset 17500)
(def high-bits-offset 18500)


(defn- normalize-freq [freq]
  (.debug js/console (str "pre-normalized: " freq))
  (let [ifreq (js/Math.round freq)
        m (mod ifreq 50)]
    (if (< m 25)
      (- ifreq m)
      (+ ifreq (- 50 m)))))


(defn- encode-bits [bs offset]
  (.debug js/console (str "encoded: " (-> bs (* 50) (+ offset))))
  (-> bs (* 50) (+ offset)))


(defn- decode-bits [freq offset]
  (-> freq (- offset) (/ 50)))


(defn- validate-and-decode-bits [freq high-bits?]
  (if high-bits?
    (when (>= freq high-bits-offset)
      (decode-bits freq high-bits-offset))
    (when (and (<= low-bits-offset freq)
               (<= freq high-bits-offset))
      (decode-bits freq low-bits-offset))))


(defn encode [s]
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


(defn <decode [channel]
  (let [c (chan)]
    (go-loop [prev nil]
      (.debug js/console (str "prev:" prev))
      (let [freq (normalize-freq (<! channel))]
        (when-let [bits (validate-and-decode-bits freq (nil? prev))]
          (if (nil? prev)
            (recur bits)
            (let [prev-bits (bit-shift-left prev 4)
                  decoded (js/String.fromCharCode
                           (bit-or prev-bits bits))]
              (put! c decoded))))
        (recur nil)))
    c))
