(ns attendance-check.routes.admin.course
  (:require [attendance-check.db.core :as d]
            [attendance-check.layout :as layout]
            [compojure.core :refer [defroutes GET POST DELETE]]
            [formidable.core :refer [render-form]]
            [formidable.parse :refer [parse-params]]
            [hiccup.core :refer [html]]
            [ring.util.response :refer [redirect]]))


(def course-form
  {:fields [{:name :name}
            {:name :tutor :type :select}]
   :validations [[:required [:name :tutor]]]})


(defn get-courses [request]
  (layout/render "course/list.html" {:courses (d/get-course {})}))


(defn get-course-new []
  (let [form (assoc-in
              (assoc course-form
                     :action "/admin/courses/"
                     :method "POST")
              [:fields 1 :options]
              (map #(hash-map :value (:_id %) :label (:name %)) (d/get-tutor {})))]
    (layout/render "course/form.html" {:form (html (render-form form))})))

(defn post-course [request]
  (try
    (let [values (parse-params course-form (:form-params request))
          tutor (d/get-tutor-by-id (:tutor values))
          course (assoc values :tutor tutor)]
      (d/create-course course)
      (redirect "/admin/courses/"))
    (catch Exception e
      (redirect "/admin/courses/new/"))))


(defn delete-course [id]
  (d/delete-course-by-id id)
  (redirect "/admin/courses/"))


(defroutes course-routes
  (GET "/courses/" request (get-courses request))
  (GET "/courses/new/" [] (get-course-new))
  (POST "/courses/" request (post-course request))
  (DELETE "/courses/:id/" [id] (delete-course id)))
