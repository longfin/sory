(ns sory.sound
  (:require [cljs.core.async :refer [<! put! chan]])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(def audio-context-constructor (or js/window.AudioContext
                                   js/window.webkitAudiocontext))


(defn- <media-stream []
  (let [c (chan)]
    (go
      (let [navigator (.-navigator js/window)
            get-user-media (.bind (or (.-getUserMedia navigator)
                                      (.-webkitGetUserMedia navigator)
                                      (.-mozGetUserMedia navigator))
                                  navigator)]
        (get-user-media
         #js {:audio #js {:optional #js [#js {:echoCancellation false}]}}
         (fn [stream]
           (put! c stream))
         (fn [e]
           (throw (js/Error. (str "Can't initialize media device. [" e "]")))))))
    c))


(defn- peak-freq [arr nyquist start-freq min-db]
  (let [buffer-length (.-length arr)
        freq-to-index #(-> %
                           (/ nyquist)
                           (* buffer-length)
                           js/Math.round)
        index-to-freq #(* % (/ nyquist buffer-length))]
    (loop [i (freq-to-index start-freq)
           index -1
           max (- (.-Infinity js/window))]
      (if (< i buffer-length)
        (let [val (aget arr i)]
          (if (< max val)
            (recur (inc i) i val)
            (recur (inc i) index max)))
        (when (< min-db max)
          (index-to-freq index))))))


(defn- fetch-freqs [analyser]
  (let [buffer-length (.-frequencyBinCount analyser)
        buffer (js/Float32Array. buffer-length)]
    (.getFloatFrequencyData analyser buffer)
    buffer))


(defn- select-freqs [freqs threshold]
  (->> freqs
       (partition-by identity)
       (map #(vector (count %) (first %)))
       (filter #(>= (key %) threshold))
       (map val)))


(deftype AudioContext [js-context
                       ramp-duration
                       duration
                       char-interval
                       process-interval
                       peak-threshold]
  Object

  (emit-sound [_ freq started-at]
     (let [oscillator (.createOscillator js-context)
           gain-node (.createGain js-context)
           gain (.-gain gain-node)]
       (.connect gain-node (.-destination js-context))
       (set! (.-value gain) 0)
       (set! (.-value (.-frequency oscillator)) freq)
       (.setValueAtTime gain 0 started-at)
       (.linearRampToValueAtTime gain 1 (+ started-at ramp-duration))
       (.setValueAtTime gain 1 (- (+ started-at duration) ramp-duration))
       (.linearRampToValueAtTime gain 0 (+ started-at duration))
       (.connect oscillator gain-node)
       (.start oscillator)))

  (emit-sound [this freq]
    (.emit-sound this freq (.-currentTime js-context)))

  (emit-sounds [this freqs]
    (loop [i 0
           started-at (.-currentTime js-context)]
      (when (< i (count freqs))
        (let [freq (nth freqs i)]
          (.emit-sound this freq started-at))
        (recur (inc i) (+ started-at char-interval)))))

  (<listen [_]
    (let [c (chan)]
      (go
        (let [stream (<! (<media-stream))
              analyser (.createAnalyser js-context)
              mic (.createMediaStreamSource js-context stream)
              nyquist (/ (.-sampleRate js-context) 2)
              process (fn process [backlog]
                        (let [raw-freqs (fetch-freqs analyser)]
                          (if-let [freq (peak-freq raw-freqs nyquist 18000 -80)]
                            (.setTimeout js/window
                                         (partial process (conj backlog freq))
                                         process-interval)
                            (let [freqs (select-freqs backlog peak-threshold)]
                              (doseq [freq freqs]
                                (.debug js/console (str "peaked: " freq))
                                (put! c freq))
                              (.setTimeout
                               js/window
                               (partial process [] process-interval))))))]
              (.connect mic analyser)
              (process [])))
      c)))


(defn initialize-audio-context
  [& {:keys [ramp-duration
             duration
             char-interval
             process-interval
             peak-threshold]
      :or {ramp-duration 0.0001
           duration 0.2
           char-interval 0.4
           process-interval 10
           peak-threshold 20}}]
  (let [js-context (audio-context-constructor.)]
    (AudioContext. js-context
                   ramp-duration
                   duration
                   char-interval
                   process-interval
                   peak-threshold)))
