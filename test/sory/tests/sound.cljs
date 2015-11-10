(ns sory.tests.sound
  "Unit test suite for `sory.sound`"
  {:doc/format :markdown}

  (:require [cljs.test :refer-macros [deftest is]]
            [sory.sound :refer [AudioContext
                                initialize-audio-context
                                select-freqs]]))


(deftest test-initialize-audio-context
  (is (instance? AudioContext (initialize-audio-context)))
  (is (instance? AudioContext (initialize-audio-context {})))
  (is (instance? AudioContext (initialize-audio-context {:ramp-duration 0.1}))))


(deftest test-select-freqs
  (is (= [18880] (select-freqs [18880] 1)))
  (is (= [] (select-freqs [18880] 2)))
  (is (= [18880] (select-freqs [18880 18880 13000] 2)))
  (is (= [] (select-freqs [18880 13000 18880] 2)))
  (is (= [18880 13000] (select-freqs [18880 18880 13000 13000] 2)))
  (is (= [18880] (select-freqs [18880 18880 13000 18880 13000] 2))))
