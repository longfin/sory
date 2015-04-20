(ns chat.core
  (:require [dommy.core :refer [set-text!] :refer-macros [sel1]]))

(enable-console-print!)

(def audio-context-constructor (or js/window.AudioContext
                                   js/window.webkitAudiocontext))


(defn freq-to-char [freq]
  (cond
    (nil? freq) nil
    (< freq 18480) :a
    (< freq 18980) :b
    (< freq 19480) :c
    (< freq 19980) :d))

(defn setup-mic []
  (let [navigator (.-navigator js/window)
        get-user-media (or (.-getUserMedia navigator)
                           (.-webkitGetUserMedia navigator)
                           (.-mozGetUserMedia navigator))
        freq-indicator (sel1 :#frequency)
        char-indicator (sel1 :#char)]
    ((.bind get-user-media navigator)
     #js {:audio #js {:optional #js [#js {:echoCancellation false}]}},
     (fn [stream]
       (let [listen-context (audio-context-constructor.)
             nyquist (/ (.-sampleRate listen-context) 2)
             analyser (.createAnalyser listen-context)
             mic (.createMediaStreamSource listen-context stream)
             buffer-length (.-frequencyBinCount analyser)
             freqs (js/Uint8Array. buffer-length)
             ffreqs (js/Float32Array. buffer-length)]
         (letfn [(freq-to-index [freq]
                   (js/Math.round (* (/ freq nyquist) buffer-length)))
                 (index-to-freq [index]
                   (println index)
                   (* index (/ nyquist buffer-length)))

                 (peak-freq [freqs]
                   (loop [i (freq-to-index 18000)
                          index -1
                          max (- (.-Infinity js/window))]
                     (if (< i buffer-length)
                       (let [val (aget freqs i)]
                         (if (< max val)
                           (recur (inc i) i val)
                           (recur (inc i) index max)))
                       (if (< -80 max)
                         (index-to-freq index)
                         ))
                     ))]
           (.connect mic analyser)
           (letfn [(draw-int []
                     (let [canvas (sel1 :#canvas-int)
                           canvas-context (.getContext canvas "2d")
                           width (.-width canvas)
                           height (.-height canvas)]
                       (.getByteTimeDomainData analyser freqs)
                       (set! (.-fillStyle canvas-context) "rgb(200, 200, 200)")
                       (.fillRect canvas-context 0 0 width height)
                       (set! (.-lineWidth canvas-context) 2)
                       (set! (.-strokeStyle canvas-context) "rgb(0,0,0)")
                       (.beginPath canvas-context)

                       (doseq [i (range buffer-length)]
                         (let [freq (aget freqs i)
                               x (* i (/ (* width 1.0) buffer-length))
                               v (/ freq 128.0)
                               y (* v (/ height 2))]
                           (if (= i 0)
                             (.moveTo canvas-context x y)
                             (.lineTo canvas-context x y))))

                       (.lineTo canvas-context (.-width canvas) (/ (.-height canvas) 2))
                       (.stroke canvas-context)))

                   (set-frequency []
                     (.getFloatFrequencyData analyser ffreqs)
                     (let [freq (peak-freq ffreqs)]
                       (set-text! freq-indicator freq)
                       (set-text! char-indicator (freq-to-char freq))))

                   (loop []
                     (draw-int)
                     (set-frequency)
                     (.requestAnimationFrame js/window loop))]
             (loop)))))

     (fn [] (println 'failed.')))))

(defn setup-play []
  (let [context (audio-context-constructor.)
        gain-node (.createGain context)
        osc (.createOscillator context)
        gain (.-gain gain-node)]
    (set! (.-value gain) 0)
    (.connect gain-node (.-destination context))
    (.connect osc gain-node)
    (.start osc 0)

    (letfn [(emit-sound [freq]
              #(let [curr-time (.-currentTime context)
                     duration 1
                     ramp-duration 0.001]
                 (set! (.-value (.-frequency osc)) freq)
                 (.setValueAtTime gain 0, curr-time)
                 (.linearRampToValueAtTime gain 1 (+ curr-time ramp-duration))
                 (.linearRampToValueAtTime gain 0 (+ curr-time duration))))]
      (set! (.-onclick (sel1 :#a)) (emit-sound 18000))
      (set! (.-onclick (sel1 :#b)) (emit-sound 18500))
      (set! (.-onclick (sel1 :#c)) (emit-sound 19000))
      (set! (.-onclick (sel1 :#d)) (emit-sound 19500))
      )))


(set! (.-onload js/window) (fn []
                             (setup-play)
                             (setup-mic)))
