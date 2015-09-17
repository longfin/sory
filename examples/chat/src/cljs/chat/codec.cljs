(ns chat.codec)

(def codes
  (sorted-map :a 19000
              :b 19200
              :c 19400
              :d 19600
              :e 19800
              :f 20000
              :g 20200
              :h 20400
              :i 20600
              :j 20800
              :k 21000))

(defn freq-to-char [freq]
  (if (> freq 18500)
    (let [s (seq codes)
          filtered (filter #(and (< (- (last %) 30) freq)
                                 (< freq (+ (last %) 30)))
                           s)]
      (first (first filtered)))))


(defn char-to-freq [char]
  (get codes char))
