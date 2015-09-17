(ns chat.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! chan]]
            [dommy.core :refer [set-text! text] :refer-macros [sel1]]
            [chat.sound :refer [initialize-audio-context <decode]]
            [chat.codec :refer [freq-to-char char-to-freq]]))


(enable-console-print!)

(defn debug-data [data]
  (let [canvas (sel1 :#canvas-int)
        canvas-context (.getContext canvas "2d")
        width (.-width canvas)
        height (.-height canvas)
        buffer-length (.-length data)]
    (set! (.-fillStyle canvas-context) "rgb(200, 200, 200)")
    (.fillRect canvas-context 0 0 width height)
    (set! (.-lineWidth canvas-context) 2)
    (set! (.-strokeStyle canvas-context) "rgb(0,0,0)")
    (.beginPath canvas-context)
    (doseq [i (range buffer-length)]
      (let [freq (aget data i)
            x (* i (/ (* width 1.0) buffer-length))
            v (/ freq 128.0)
            y (* v (/ height 2))]
        (if (= i 0)
          (.moveTo canvas-context x y)
          (.lineTo canvas-context x y))))
    (.lineTo canvas-context (.-width canvas) (/ (.-height canvas) 2))
    (.stroke canvas-context)))


(defn setup-play []
  (let [audio-context (initialize-audio-context)]
    (set! (.-onclick (sel1 :#a)) #(.emit-sound audio-context (char-to-freq :a)))
    (set! (.-onclick (sel1 :#b)) #(.emit-sound audio-context (char-to-freq :b)))
    (set! (.-onclick (sel1 :#c)) #(.emit-sound audio-context (char-to-freq :c)))
    (set! (.-onclick (sel1 :#d)) #(.emit-sound audio-context (char-to-freq :d)))
    (set! (.-onclick (sel1 :#e)) #(.emit-sound audio-context (char-to-freq :e)))
    (set! (.-onclick (sel1 :#f)) #(.emit-sound audio-context (char-to-freq :f)))
    (set! (.-onclick (sel1 :#g)) #(.emit-sound audio-context (char-to-freq :g)))
    (set! (.-onclick (sel1 :#h)) #(.emit-sound audio-context (char-to-freq :h)))
    (set! (.-onclick (sel1 :#i)) #(.emit-sound audio-context (char-to-freq :i)))
    (set! (.-onclick (sel1 :#j)) #(.emit-sound audio-context (char-to-freq :j)))
    (set! (.-onclick (sel1 :#k)) #(.emit-sound audio-context (char-to-freq :k)))
    (set! (.-onclick (sel1 :#abc)) #(.send audio-context [:a :b :c]))
    (set! (.-onclick (sel1 :#bbc)) #(.send audio-context [:b :b :c]))))


(defn setup-mic []
  (let [audio-context (initialize-audio-context)]
    (let [c (<decode (.<listen audio-context))]
      (go-loop []
        (let [char (<! c)]
          (let [text-el (sel1 :#char)]
            (.info js/console (str char))
            (set-text! text-el char)))
        (recur)))))


(set! (.-onload js/window)
      (fn []
        (setup-play)
        (setup-mic)))
