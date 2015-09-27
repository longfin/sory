(ns attendance-check.routes.admin.tutor
  (:require [attendance-check.layout :as layout]
            [crypto.password.bcrypt :as password]
            [formidable.core :refer [render-form]]
            [formidable.parse :refer [parse-params]]
            [hiccup.core :refer [html]]
            [ring.util.response :refer [redirect]]
            [compojure.core :refer [defroutes GET DELETE POST]]
            [attendance-check.db.core :as d]))


(def tutor-form
  {:fields [{:name :email, :type "email"}
            {:name :password}
            {:name :name}]
   :validations [[:required [:email :password]]]})


(defn get-tutors []
  (layout/render "tutor/list.html" {:tutors (d/get-tutor {})}))


(defn delete-tutor [id]
  (d/delete-tutor-by-id id)
  (redirect "/admin/tutors/"))


(defn get-tutor-form [{:keys [flash]}]
  (let [form (assoc tutor-form
                    :action "/admin/tutors/"
                    :method "POST")]
    (layout/render
     "tutor/new.html"
     (merge {:form (html (render-form form))}
            (select-keys flash [:errors])))))


(defn post-tutor [request]
  (try
    (let [values (parse-params tutor-form (:form-params request))
          tutor (update values :password password/encrypt)]
      (d/create-tutor tutor)
      (redirect "/admin/tutors/"))
    (catch Exception e
      (->
       (redirect "/admin/tutors/new/")
       (assoc :flash {:errors (str e)})))))


(defroutes tutor-routes
  (GET "/tutors/" [] (get-tutors))
  (DELETE "/tutors/:id/" [id] (delete-tutor id))
  (GET "/tutors/new/" request (get-tutor-form request))
  (POST "/tutors/" request (post-tutor request)))
