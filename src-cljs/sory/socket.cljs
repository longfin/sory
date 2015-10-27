;; Copyright (C) 2015 longfin
;;
;; This software may be modified and distributed under the terms
;; of the MIT license.  See the LICENSE file for details.


(ns sory.socket
  (:require [sory.sound :refer [initialize-audio-context]]
            [sory.codec :refer [encode <decode]]))


(deftype SorySocket [audio-context]
  Object

  (broadcast! [_ message]
    (let [encoded-message (encode message)]
      (.emit-sounds audio-context encoded-message)))

  (<listen [_]
    (<decode (.<listen audio-context)))

  (stop! [_]
    (.stop! audio-context)))


(defn initialize-socket []
  (let [audio-context (initialize-audio-context)]
    (SorySocket. audio-context)))
