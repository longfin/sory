(ns chat.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! chan]]
            [dommy.core :refer [set-text! text attr] :refer-macros [sel sel1]]
            [sory.sound :refer [initialize-audio-context]]
            [sory.codec :refer [encode <decode]]))


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
  (let [audio-context (initialize-audio-context)
        form (sel1 :#chat)]
    (set! (.-onsubmit form)
          (fn [e]
            (.preventDefault e)
            (let [form (.-currentTarget e)
                  message (.-value (aget (.-elements form) "message"))]
              (.emit-sounds audio-context (encode message)))))))


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
