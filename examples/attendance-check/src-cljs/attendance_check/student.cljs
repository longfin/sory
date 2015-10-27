(ns attendance-check.student
  (:require [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [<!]]
            [cljs.core.async :refer [chan put! <!]]
            [reagent.core :as reagent]
            [sory.socket :refer [initialize-socket]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(defonce sory-socket (initialize-socket))


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


(defn rotate [coll start end]
  (let [tail (take-while #(not (= % start)) coll)
        body (->> coll
                  (drop-while #(not (= % start)))
                  (take-while #(not (= % (first tail)))))]
    (concat body tail)))


(defn start-attendance [course-id]
  (let [chan (.<listen sory-socket)
        start-char \^
        end-char \$]
    (go-loop [backlog []]
      (if (= (count backlog) 6)
        (when (and (some #(= % start-char) backlog)
                 (some #(= % end-char) backlog))
          (let [code (->>
                      (rotate backlog start-char end-char)
                      (drop 1)
                      (drop-last 1)
                      (apply str))]
            (attendance course-id code)
            (.debug js/console (str "posted:" code)))
          (recur []))
        (let [char (<! chan)]
          (.debug js/console (str "recevied: " char))
          (recur (conj backlog char)))))))


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
  nil)
