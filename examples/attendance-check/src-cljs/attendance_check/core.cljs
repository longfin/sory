(ns attendance-check.core
  (:require [attendance-check.dashboard :as dashboard]
            [attendance-check.student :as student]))


(defn init! []
  (aset js/window "startDashboardApp" dashboard/start)
  (aset js/window "startStudentApp" student/start))
