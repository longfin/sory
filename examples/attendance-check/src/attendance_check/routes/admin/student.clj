(ns attendance-check.routes.admin.student
  (:require [attendance-check.layout :as layout]
            [attendance-check.db.core :as d]
            [compojure.core :refer [defroutes GET POST DELETE PUT]]
            [crypto.password.bcrypt :as password]
            [formidable.core :refer [render-form]]
            [formidable.parse :refer [parse-params]]
            [hiccup.core :refer [html]]
            [ring.util.response :refer [redirect]]))


(def student-form
  {:fields [{:name :email, :type "email"}
            {:name :password}
            {:name :name}]
   :validations [[:required [:email]]]})


(defn get-students []
  (layout/render "student/list.html" {:students (d/get-student {})}))


(defn get-student [id]
  (try
    (if-let [student (d/get-student-by-id id)]
      (let [form (assoc student-form
                        :action (str "/admin/students/" id "/?__method__=put")
                        :values (dissoc student :password))]
        (layout/render "student/form.html" {:form (html (render-form form))}))
      (layout/error-page {:status 404}))
    (catch IllegalArgumentException e
      (layout/error-page {:status 404}))))


(defn get-student-form [{:keys [flash]}]
  (let [form (assoc student-form
                    :action "/admin/students/"
                    :method "POST")]
    (layout/render "student/form.html"
                   (merge
                    {:form (html (render-form form))}
                    (select-keys flash [:errors])))))


(defn post-student [request]
  (try
    (let [form (update-in student-form
                          [:validations 0 1]
                          #(conj % :password))
          values (parse-params form (:form-params request))
          student (update values :password password/encrypt)]
      (d/create-student student)
      (redirect "/admin/students/"))
    (catch Exception e
      (->
       (redirect "/admin/students/new/")
       (assoc :flash {:errors (str e)})))))


(defn delete-student [id]
  (d/delete-student-by-id id)
  (redirect "/admin/students/"))


(defn put-student [_id request]
  (try
    (let [values (parse-params student-form (:form-params request))
          student (if (empty? (:password values))
                    (dissoc values :password)
                    (update values :password password/encrypt))]
      (d/update-student _id student)
      (redirect "/admin/students/"))
    (catch Exception e
      (->
       (redirect (str "/admin/students/" _id "/"))
       (assoc :flash {:errors (str e)})))))


(defroutes student-routes
  (GET "/students/" [] (get-students))
  (GET "/students/:id{(?!new).+}/" [id] (get-student id))
  (GET "/students/new/" request (get-student-form request))
  (PUT "/students/:id{(?!new).+}/" [id :as request] (put-student id request))
  (POST "/students/" request (post-student request))
  (DELETE "/students/:id/" [id] (delete-student id)))
