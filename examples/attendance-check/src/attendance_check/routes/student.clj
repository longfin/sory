(ns attendance-check.routes.student
  (:require [attendance-check.db.core :as d]
            [attendance-check.layout :as layout]
            [attendance-check.middleware :refer [wrap-formats]]
            [crypto.password.bcrypt :as password]
            [compojure.core :refer [GET POST defroutes routes wrap-routes]]
            [langohr.basic :as lb]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [monger.operators :refer [$elemMatch]]
            [ring.util.response :refer [redirect]])
  (:import org.bson.types.ObjectId))


(defn wrap-auth [handler]
  (fn [request]
    (if-let [student (-> request :session :student)]
      (handler request)
      (redirect "/student/login"))))


(defn index [{session :session}]
  (println session)
  (layout/render "index.html" (select-keys session [:student])))


(defn get-login [{:keys [flash]}]
  (layout/render "login.html"
                 (select-keys flash [:errors])))


(defn post-login [{:keys [session form-params]}]
  (let [{:strs [email password]} form-params
        student (first (d/get-student {:email email}))]
    (if (and student
             (password/check password (:password student)))
      (-> (redirect "/student/")
          (assoc :session (assoc session :student
                                 (d/serialize student))))
      (-> (redirect "/student/login")
          (assoc :flash {:errors "해당하는 사용자가 없습니다."})))))


(defn get-courses [{:keys [session]}]
  (let [student-id (-> session :student :_id ObjectId.)
        courses (d/get-course {:students {$elemMatch {:_id student-id}}})]
    {:body
     {:courses (d/serialize courses)
      :result :success}}))


(defn post-attendance-check [course-id {:keys [session form-params]}]
  (let [code (form-params "code")
        student (:student session)
        course (d/get-course-by-id course-id)
        check (first (d/get-attendance-check {:code code}))
        queue (format "courses/%s/check/%s" course-id (str (:_id check)))
        amqp-conn (rmq/connect)
        chan (lch/open amqp-conn)]
    (lb/publish chan "" queue (prn-str {:student student}))
    (rmq/close chan)
    (rmq/close amqp-conn)
    {:body
     {:check (d/serialize check)
      :result :success}}))


(def rest-api-routes
  (wrap-routes
   (routes
    (GET "/courses/" request (get-courses request))
    (POST "/courses/:course/attendance-checks/"
          [course :as request]
          (post-attendance-check course request)))
   wrap-formats))


(defroutes secured-routes
  rest-api-routes
  (GET "/" request (index request)))


(defroutes student-routes
  (wrap-routes secured-routes wrap-auth)
  (GET "/login" request (get-login request))
  (POST "/login" request (post-login request)))
