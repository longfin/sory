(ns chat.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! chan]]
            [dommy.core :refer [set-text! text attr] :refer-macros [sel sel1]]
            [chat.sound :refer [initialize-audio-context]]
            [chat.codec :refer [encode <decode]]))


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
    (doseq [el (sel :button)]
      (set! (.-onclick el) #(.emit-sounds audio-context (encode (attr el :data-str)))))))


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
