(ns sory.codec
  (:require [cljs.core.async :refer [<! put! chan]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


(defn normalize-freq [freq]
  (if (> freq 18500)
    (let [ifreq (js/Math.round freq)
          m (mod ifreq 50)]
      (/ (- (if (< m 25)
              (- ifreq m)
              (+ ifreq (- 50 m)))
            19000)
         50))))

(defn encode [s]
  (let [bs (map #(.charCodeAt %) s)]
    (map
     #(+ 19000 (* 50 %))
     (interleave
      (map #(bit-shift-right (bit-and % 0xF0) 4) bs)
      (map #(bit-and % 0x0F) bs)))))


(defn <decode [channel]
  (let [c (chan)]
    (go-loop [prev nil]
      (when-let [freq (normalize-freq (<! channel))]
        (if (nil? prev)
          (recur freq)
          (let [decoded (js/String.fromCharCode
                         (bit-or (bit-shift-left prev 4)
                                 freq))]
            (put! c decoded)
            (recur nil)))))
    c))
