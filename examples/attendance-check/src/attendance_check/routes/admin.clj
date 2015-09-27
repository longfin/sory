(ns attendance-check.routes.admin
  (:require [compojure.core :refer [routes wrap-routes]]
            [attendance-check.middleware :as middleware]
            [attendance-check.routes.admin.course :refer [course-routes]]
            [attendance-check.routes.admin.tutor :refer [tutor-routes]]
            [attendance-check.routes.admin.student :refer [student-routes]]))

(def admin-routes
  (routes
   (wrap-routes #'course-routes middleware/wrap-csrf)
   (wrap-routes #'tutor-routes middleware/wrap-csrf)
   (wrap-routes #'student-routes middleware/wrap-csrf)))

