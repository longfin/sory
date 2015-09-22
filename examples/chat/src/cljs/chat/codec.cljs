(ns chat.codec
  (:require [cljs.core.async :refer [<! put! chan]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def codes
  (sorted-map "a" 19000
              "b" 19050
              "c" 19100
              "d" 19150
              "e" 19200
              "f" 19250
              "g" 19300
              "h" 19350
              "i" 19400
              "j" 19450
              "k" 19500))

(defn freq-to-char [freq]
  (if (> freq 18500)
    (let [s (seq codes)
          filtered (filter #(and (< (- (last %) 20) freq)
                                 (< freq (+ (last %) 20)))
                           s)]
      (first (first filtered)))))


(defn char-to-freq [char]
  (get codes char))


(defn encode [s]
  (map char-to-freq s))


(defn <decode [channel]
  (let [c (chan)]
    (go-loop []
      (let [freqs (<! channel)
            counted-freqs (map #(vector (count %) (first %))
                               (partition-by #(< (js/Math.abs (- %1 %2)) 10) freqs))]
        (.debug js/console (str counted-freqs))
        (when-let [freq-pair (first
                              (filter #(> (key %) 9) counted-freqs))]
          (when-let [char (freq-to-char (val freq-pair))]
            (.log js/console (str freq-pair "@" freqs))
            (put! c char))))
      (recur))
    c))
