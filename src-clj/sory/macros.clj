;; Copyright (C) 2015 longfin
;;
;; This software may be modified and distributed under the terms
;; of the MIT license.  See the LICENSE file for details.


(ns sory.macros
  "Define some helper macros for sory library")


(defmacro dbg
  "Generate expression that logger and itself.
  print `tag` as prefix if given, else `\"dbg\"` printed."
  {:doc/format :markdown}

  [body & tag]
  (let [tag (if (nil? tag) "dbg" tag)]
    `(let [x# ~body
           t# ~tag]
       (.debug js/console (str t# ":" '~body "=" x#))
       x#)))
