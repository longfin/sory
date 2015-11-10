(ns sory.tests.socket
  "Unit test suite for `sory.socket`"
  {:doc/format :markdown}


  (:require [cljs.test :refer-macros [deftest is]]
            [sory.socket :refer [SorySocket
                                 initialize-socket]]))


(deftest test-initialize-socket
  (is (instance? SorySocket (initialize-socket))))
