(ns chat.sound
  (:require [cljs.core.async :refer [<! put! chan]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def audio-context-constructor (or js/window.AudioContext
                                   js/window.webkitAudiocontext))

(deftype AudioContext [js-context oscillator gain]
  Object
  (emit-sound [_ freq]
    (let [curr-time (.-currentTime js-context)
          duration 1
          ramp-duration 0.001
          gain (.-gain gain)]
      (set! (.-value (.-frequency oscillator)) freq)
      (.setValueAtTime gain 0 curr-time)
      (.linearRampToValueAtTime gain 1 (+ curr-time ramp-duration))
      (.linearRampToValueAtTime gain 0 (+ curr-time duration)))))

(defn initialize-audio-context []
  (let [js-context (audio-context-constructor.)
        oscillator (.createOscillator js-context)
        gain (.createGain js-context)]
    (set! (.-value (.-gain gain)) 0)
    (.connect gain (.-destination js-context))
    (.connect oscillator gain)
    (.start oscillator)
    (AudioContext. js-context oscillator gain)))

(defn <stream []
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
         (fn []
           (put! c nil)))))
    c))

(defn <media-stream []
  (let [c (chan)]
    (go
      (let [stream (<! (<stream))
            listen-context (audio-context-constructor.)
            nyquist (/ (.-sampleRate listen-context) 2)
            analyser (.createAnalyser listen-context)
            mic (.createMediaStreamSource listen-context stream)
            buffer-length (.-frequencyBinCount analyser)

            uint-freqs (js/Uint8Array. buffer-length)
            float-freqs (js/Float32Array. buffer-length)

            freq-to-index #(js/Math.round (* (/ % nyquist) buffer-length))
            index-to-freq #(* % (/ nyquist buffer-length))
            peak-freq #(loop [i (freq-to-index 19000)
                              index -1
                              max (- (.-Infinity js/window))]
                         (if (< i buffer-length)
                           (let [val (aget % i)]
                             (if (< max val)
                               (recur (inc i) i val)
                               (recur (inc i) index max)))
                           (if (< -80 max)
                             (index-to-freq index))))]
        (.connect mic analyser)
        (put! c [(fn []
                   (.getByteTimeDomainData analyser uint-freqs)
                   uint-freqs)
                 (fn []
                   (.getFloatFrequencyData analyser float-freqs)
                   (peak-freq float-freqs))])))
    c))
