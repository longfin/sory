(ns attendance-check.student
  (:require [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [chan put! <!]]
            [reagent.core :as reagent])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn <get-courses []
  (let [c (chan)]
    (go
      (GET "/student/courses/"
           {:handler #(put! c %)
            :response-format :json
            :keywords? true}))
    c))


(defn attendance [course-id code]
  (let [url (str "/student/courses/" course-id "/attendance-checks/")]
    (POST url
          {:params {:code code}
           :format :raw})))


(defn start-attendance [course-id]
  ;; TODO implement with sory lib
  (.alert js/window course-id))


(defn course-table []
  (let [courses-chan (<get-courses)
        state (reagent/atom {:courses []})]
    (go
      (let [courses (-> courses-chan <! :courses)]
        (swap! state assoc :courses courses)))
    (fn []
      [:table
       [:thead
        [:tr
         [:th "강의명"]
         [:th "강사명"]
         [:th ""]]]
       [:tbody
        (for [c (:courses @state)]
          ^{:key c}
          [:tr
           [:td (-> c :name)]
           [:td (-> c :tutor :name)]
           [:td
            [:button {:type "butotn"
                      :on-click #(start-attendance (:_id c))}
             "출석 체크"]]])]])))


(defn app []
  (fn []
    [:div
     [course-table]]))


(defn mount-components []
  (reagent/render [#'app] (.getElementById js/document "app")))


(defn start []
  (mount-components)
  (aset js/window "attendance" attendance)
  nil)
