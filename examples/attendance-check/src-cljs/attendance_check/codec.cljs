(ns attendance-check.codec
  (:require [cuerdas.core :refer [pad]]))


(defn int-to-bytes [n]
  [(bit-or (bit-and (bit-shift-right n 7) 0x7F) 0x80)
   (bit-and n 0x7F)])


(defn bytes-to-int [bytes]
  (let [high (->> bytes
                  (filter #(= 0x80 (bit-and % 0x80)))
                  first
                  (bit-and 0x7F))
        low (->> bytes
                 (filter #(= 0 (bit-and % 0x80)))
                 first)]
    (bit-or (bit-shift-left high 7)
            low)))


(defn encode [code]
  (->> code
       int
       int-to-bytes
       (map js/String.fromCharCode)
       (apply str)))


(defn decode [bs]
  (let [bytes (map #(.charCodeAt %) bs)]
    (-> bytes
        bytes-to-int
        str
        (pad {:length 4 :padding "0"}))))
