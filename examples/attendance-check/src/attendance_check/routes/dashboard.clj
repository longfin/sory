(ns attendance-check.routes.dashboard
  (:require [attendance-check.db.core :as d]
            [attendance-check.layout :as layout]
            [attendance-check.middleware :refer [wrap-formats]]
            [compojure.core :refer [GET POST defroutes routes wrap-routes]]
            [crypto.password.bcrypt :as password]
            [ring.util.response :refer [redirect]]
            [manifold.stream :as ms]
            [manifold.deferred :as md]
            [clojure.core.async :as a]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [langohr.basic :as lb]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lc]
            [langohr.queue :as lq]))


(defn index []
  (layout/render "index.html"))


(defn get-login [{:keys [flash]}]
  (layout/render "login.html"
                 (select-keys flash [:errors])))


(defn post-login [{:keys [form-params session]}]
  (let [{:strs [email password]} form-params
        tutor (first (d/get-tutor {:email email}))]
    (if (and tutor
             (password/check password (:password tutor)))
      (-> (redirect "/dashboard/")
          (assoc :session (assoc session :tutor tutor)))
      (-> (redirect "/dashboard/login")
          (assoc :flash {:errors "해당하는 사용자가 없습니다."})))))


(defn get-courses [{:keys [session]}]
  (let [tutor-id (-> session :tutor :_id)
        courses (d/get-course {"tutor._id" tutor-id})]
    {:body
     {:courses (d/serialize courses)
      :result :success}}))


(defn format-sse [obj]
  (format "data: %s\n\n" (json/write-str obj)))


(defn post-attendance-check [course-id]
  (if-let [course (d/get-course-by-id course-id)]
    (let [code (apply str (take 6 (repeatedly #(rand-int 9))))
          check (->
                 {:course course
                  :code code}
                 d/create-attendance-check)]
      {:body
       {:check (d/serialize check)
        :result :success}})
    {:status 400
     :body {:result :error}}))


(defn course-stream [course-id check-id]
  (let [course (d/get-course-by-id course-id)
        body (a/chan)
        amqp-conn (rmq/connect)
        chan (lch/open amqp-conn)
        queue (format "courses/%s/check/%s" course-id check-id)]
    (lq/declare chan queue {:auto-delete true :exclusive false})
    (lc/subscribe chan queue (fn [ch meta payload]
                               (let [sse-payload (-> payload
                                                     (String. "UTF-8")
                                                     edn/read-string
                                                     format-sse)]
                                 (a/go (a/>! body sse-payload))))
                  {:auto-ack true})
    {:status 200
     :headers {"content-type" "text/event-stream"}
     :body (ms/->source body)}))


(defn wrap-auth [handler]
  (fn [request]
    (if-let [tutor (-> request :session :tutor)]
      (handler request)
      (redirect "/dashboard/login"))))


(def rest-api-routes
  (wrap-routes
   (routes
    (GET "/courses/" request (get-courses request))
    (POST "/courses/:course/attendance-checks/"
          [course]
          (post-attendance-check course)))
   wrap-formats))


(defroutes secured-routes
  rest-api-routes
  (GET "/" [] (index))
  (GET "/courses/:course/attendance-checks/:check/"
       [course check]
       (course-stream course check)))


(defroutes dashboard-routes
  (wrap-routes secured-routes wrap-auth)
  (GET "/login" request (get-login request))
  (POST "/login" request (post-login request)))
