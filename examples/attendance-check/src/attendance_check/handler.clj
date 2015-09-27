(ns attendance-check.handler
  (:require [compojure.core :refer [context defroutes routes wrap-routes]]
            [attendance-check.layout :refer [error-page]]
            [attendance-check.routes.admin :refer [admin-routes]]
            [attendance-check.routes.dashboard :refer [dashboard-routes]]
            [attendance-check.routes.student :refer [student-routes]]
            [attendance-check.middleware :as middleware]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [selmer.parser :as parser]
            [environ.core :refer [env]]))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []

  (timbre/merge-config!
    {:level     (if (env :dev) :trace :info)
     :appenders {:rotor (rotor/rotor-appender
                          {:path "attendance_check.log"
                           :max-size (* 512 1024)
                           :backlog 10})}})

  (if (env :dev) (parser/cache-off!))
  (timbre/info (str
                 "\n-=[attendance-check started successfully"
                 (when (env :dev) " using the development profile")
                 "]=-")))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "attendance-check is shutting down...")
  (timbre/info "shutdown complete!"))


(defmacro blueprint [name & body]
  (let [prefix (str "/" name)
        template-path (str "templates/" name "/")]
  `(middleware/wrap-template-path
    (context ~prefix [] ~@body)
    ~template-path)))

(defroutes app-routes
  (blueprint "admin" admin-routes)
  (blueprint "dashboard" dashboard-routes)
  (blueprint "student" student-routes)
  (route/not-found
   (:body
    (error-page {:status 404
                   :title "page not found"}))))

(def app (middleware/wrap-base #'app-routes))
