(ns attendance-check.student
  "Stduent side sample app
  it only receives sound signal from dashboard app. (no emit logic)"

  (:require [attendance-check.codec :refer [decode]]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [chan put! <! close!]]
            [reagent.core :as reagent]
            [sory.socket :refer [initialize-socket]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


;; initialize sory-socket only once.
(defonce sory-socket (initialize-socket))


(defn <get-courses []
  (let [c (chan)]
    (go
      (GET "/student/courses/"
           {:handler #(put! c %)
            :response-format :json
            :keywords? true}))
    c))


(defn <attendance [course-id code]
  (let [c (chan)]
    (go
      (let [url (str "/student/courses/" course-id "/attendance-checks/")]
        (POST url
              {:handler #(put! c {:result :success
                                  :response %})
               :error-handler #(put! c {:result :error
                                        :response %})
               :params {:code code}
               :format :raw})))
    c))


(defn course-table []
  (let [courses-chan (<get-courses)
        courses (reagent/atom [])
        listening-session (reagent/atom {})]
    (go
      (let [fetched (-> courses-chan <! :courses)]
        (reset! courses fetched)))
    (letfn [(start-attendance [course-id]
              (let [chan (.<listen sory-socket)]
                (go-loop [backlog []]
                  (if (= (count backlog) 2)
                    (when-let [code (decode backlog)]
                      (.debug js/console (str "posted:" code))
                      (go
                        (let [r (<! (<attendance course-id code))]
                         (if (= (:result r) :success)
                           (do
                             (.alert js/window "처리되었습니다.")
                             (stop))
                           (do
                             ;; retry...
                             (.error js/console r)))))
                      (recur []))
                    (let [char (<! chan)]
                      (.debug js/console (str "recevied: " char))
                      (recur (conj backlog char)))))
                chan))
            (start [course]
              (swap! listening-session assoc
                     :course course
                     :chan (start-attendance (:_id course))))
            (stop []
              (.stop! sory-socket)
              (close! (:chan @listening-session))
              (reset! listening-session {}))]
     (fn []
       [:div
        (when-let [course (:course @listening-session)]
          [:div
           [:span (str (:name course) " 출석 대기 중...")]
           [:button
            {:type "button"
             :class "btn btn-default btn-sm"
             :on-click stop}
            "취소"]])
        [:table
         {:class "table"}
         [:thead
          [:tr
           [:th "강의명"]
           [:th "강사명"]
           [:th "출석"]]]
         [:tbody
          (doall
           (for [c @courses]
             ^{:key c}
             [:tr
              [:td (-> c :name)]
              [:td (-> c :tutor :name)]
              [:td
               (when (not (:course @listening-session))
                 [:button
                  {:type "button"
                   :class "btn btn-default btn-sm"
                   :on-click #(start c)}
                  "입실"])]]))]]]))))


(defn app
  "Root component of student app."
  []
  (fn []
    [:div
     [course-table]]))


(defn mount-components []
  (reagent/render [#'app] (.getElementById js/document "app")))


(defn start
  "Entry point of student app."
  []
  (mount-components)
  nil)
