;; Copyright (C) 2015 longfin
;;
;; This software may be modified and distributed under the terms
;; of the MIT license.  See the LICENSE file for details.


(ns sory.socket
  "Provides top-level communication interface."
  {:doc/format :markdown}

  (:require [sory.sound :refer [initialize-audio-context]]
            [sory.codec :refer [encode <decode]]))


(deftype SorySocket [audio-context]
  ;; Construct socket interface.
  ;; Don't initialize this by constructor(`.SorySocket`)
  ;; Use `initialize-socket()` instead

  Object

  (broadcast! [_ message]
    (let [encoded-message (encode message)]
      (.emit-sounds audio-context encoded-message)))

  (<listen [_]
    (<decode (.<listen audio-context)))

  (stop! [_]
    (.stop! audio-context)))


;; TODO Add options.
(defn initialize-socket []
  "Initialize sound socket object"
  {:doc/format :markdown}

  (let [audio-context (initialize-audio-context)]
    (SorySocket. audio-context)))
