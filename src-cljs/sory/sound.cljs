(ns sory.sound
  (:require [cljs.core.async :refer [<! put! chan]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def audio-context-constructor (or js/window.AudioContext
                                   js/window.webkitAudiocontext))

(defn- <stream []
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

(defn- peak-freq [arr nyquist]
  (let [buffer-length (.-length arr)
        freq-to-index #(js/Math.round (* (/ % nyquist) buffer-length))
        index-to-freq #(* % (/ nyquist buffer-length))]
    (loop [i (freq-to-index 18500)
           index -1
           max (- (.-Infinity js/window))]
      (if (< i buffer-length)
        (let [val (aget arr i)]
          (if (< max val)
            (recur (inc i) i val)
            (recur (inc i) index max)))
        (when (< -80 max)
          (index-to-freq index))))))

(deftype AudioContext [js-context]
  Object

  (emit-sound [_ freq started-at]
     (let [oscillator (.createOscillator js-context)
           gain-node (.createGain js-context)
           gain (.-gain gain-node)
           duration 0.2
           ramp-duration 0.001]
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
        (recur (inc i) (+ started-at 0.4)))))

  (<listen [_]
    (let [c (chan)]
      (go
        (let [stream (<! (<stream))
              analyser (.createAnalyser js-context)
              mic (.createMediaStreamSource js-context stream)
              buffer-length (.-frequencyBinCount analyser)
              buffer (js/Float32Array. buffer-length)
              nyquist (/ (.-sampleRate js-context) 2)
              process (fn process [buf]
                        (.getFloatFrequencyData analyser buffer)
                        (if-let [freq (peak-freq buffer nyquist)]
                          (.setTimeout js/window
                                       (partial process (conj buf freq))
                                       5)
                          (do
                            (when (not (empty? buf))
                              (let [counted-freqs (map #(vector (count %)
                                                                (first %))
                                                       (partition-by identity
                                                                     buf))]
                                (when-let [freq-pairs (filter #(>= (key %) 20)
                                                              counted-freqs)]
                                  (doseq [freq-pair freq-pairs]
                                    (put! c (val freq-pair))))))
                            (.setTimeout js/window (partial process [] 10)))))]
              (.connect mic analyser)
              (process [])))
      c)))

(defn initialize-audio-context []
  (let [js-context (audio-context-constructor.)]
    (AudioContext. js-context)))
