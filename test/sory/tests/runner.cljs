(ns sory.tests.runner
  (:require [cljs.test :as test]
            [cljs.test :refer-macros [deftest is testing run-tests]]
            [doo.runner :refer-macros [doo-all-tests doo-tests]]
            [sory.tests.sound]
            [sory.tests.codec]
            [sory.tests.socket]))

(doo-tests 'sory.tests.sound
           'sory.tests.socket
           'sory.tests.codec)
