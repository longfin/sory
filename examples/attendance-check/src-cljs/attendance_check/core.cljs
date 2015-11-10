(ns attendance-check.core
  "Sample web application client code using sory socket."

  (:require [attendance-check.dashboard :as dashboard]
            [attendance-check.student :as student]))


(defn init! []
  (aset js/window "startDashboardApp" dashboard/start)
  (aset js/window "startStudentApp" student/start))
