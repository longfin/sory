(ns chat.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! chan]]
            [dommy.core :refer [set-text!] :refer-macros [sel1]]
            [chat.sound :refer [initialize-audio-context <media-stream]]))

(enable-console-print!)

(defn freq-to-char [freq]
  (cond
    (nil? freq) nil
    (< freq 18880) nil
    (< freq 18980) :a
    (< freq 19180) :b
    (< freq 19380) :c
    (< freq 19580) :d))

(defn setup-mic []
  (go
    (let [[get-freqs get-freq] (<! (<media-stream))
          draw-int #(let [canvas (sel1 :#canvas-int)
                          canvas-context (.getContext canvas "2d")
                          width (.-width canvas)
                          height (.-height canvas)
                          buffer-length (.-length %)]
                      (set! (.-fillStyle canvas-context) "rgb(200, 200, 200)")
                      (.fillRect canvas-context 0 0 width height)
                      (set! (.-lineWidth canvas-context) 2)
                      (set! (.-strokeStyle canvas-context) "rgb(0,0,0)")
                      (.beginPath canvas-context)
                      (doseq [i (range buffer-length)]
                        (let [freq (aget % i)
                              x (* i (/ (* width 1.0) buffer-length))
                              v (/ freq 128.0)
                              y (* v (/ height 2))]
                          (if (= i 0)
                            (.moveTo canvas-context x y)
                            (.lineTo canvas-context x y))))
                      (.lineTo canvas-context (.-width canvas) (/ (.-height canvas) 2))
                      (.stroke canvas-context))
          process (fn process []
                    (let [freq (get-freq)
                          freqs (get-freqs)
                          freq-indicator (sel1 :#frequency)
                          char-indicator (sel1 :#char)]
                      (draw-int freqs)
                      (set-text! freq-indicator freq)
                      (set-text! char-indicator (freq-to-char freq))
                      (.requestAnimationFrame js/window process)))]
      (process))))

(defn setup-play []
  (let [audio-context (initialize-audio-context)]
    (set! (.-onclick (sel1 :#a)) #(.emit-sound audio-context 19000))
    (set! (.-onclick (sel1 :#b)) #(.emit-sound audio-context 19200))
    (set! (.-onclick (sel1 :#c)) #(.emit-sound audio-context 19400))
    (set! (.-onclick (sel1 :#d)) #(.emit-sound audio-context 19600))))


(set! (.-onload js/window) (fn []
                             (setup-play)
                             (setup-mic)))
