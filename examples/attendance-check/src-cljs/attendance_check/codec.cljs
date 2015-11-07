(ns attendance-check.codec
  (:require [cuerdas.core :refer [pad]]))


(def start-char \^)
(def end-char \$)


(defn uint16-to-bytes [n]
  [(bit-and n 0xFF)
   (bit-and (bit-shift-right n 8)
                     0xFF)])


(defn bytes-to-uint16 [[low-byte high-byte]]
  (.debug js/console (str "bytes-to-uint16:" low-byte "/" high-byte))
  (bit-or low-byte
          (bit-shift-left high-byte 8)))


(defn- rotate [coll start end]
  (let [tail (take-while #(not (= % start)) coll)
        body (->> coll
                  (drop-while #(not (= % start)))
                  (take-while #(not (= % (first tail)))))]
    (concat body tail)))


(defn encode [code]
  (let [byte-str (->> code
                      int
                      uint16-to-bytes
                      (map js/String.fromCharCode)
                      (apply str))]
    (str start-char
         byte-str
         end-char)))


(defn decode [bs]
  (when (and (some #(= % start-char) bs)
             (some #(= % end-char) bs))
    (let [bytes (->>
                (rotate bs start-char end-char)
                (drop 1)
                (drop-last 1)
                (map #(.charCodeAt %)))]
      (-> bytes
          bytes-to-uint16
          str
          (pad {:length 4 :padding "0"})))))
