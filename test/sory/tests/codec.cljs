(ns sory.tests.codec
  "Unit test suite for `sory.codec`"
  {:doc/format :markdown}

  (:require [cljs.test :refer-macros [deftest is]]
            [sory.codec :refer [encode
                                normalize-freq
                                validate-and-decode-bits]]))


(deftest test-encode
  (is (= [] (encode [])))
  (is (= [;; a
          18800 17550
          ;; b
          18800 17600
          ;; c
          18800 17650] (encode "abc"))))


(deftest test-validate-and-decode-bits
  (is (nil? (validate-and-decode-bits 0 true)))
  (is (nil? (validate-and-decode-bits 0 false)))

  (is (nil? (validate-and-decode-bits 17550 true)))
  (is (nil? (validate-and-decode-bits 18800 false)))

  (is (= 6 (validate-and-decode-bits 18800 true)))
  (is (= 1 (validate-and-decode-bits 17550 false))))


(deftest test-normalize-freq
  (is (= 18800 (normalize-freq 18799)))
  (is (= 18800 (normalize-freq 18801)))
  (is (= 18800 (normalize-freq 18824)))
  (is (= 18800 (normalize-freq 18776))))
