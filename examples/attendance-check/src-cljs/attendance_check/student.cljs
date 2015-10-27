(ns attendance-check.student
  (:require [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [chan put! <! close!]]
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


(defn <attendance [course-id code]
  (let [c (chan)]
    (go
      (let [url (str "/student/courses/" course-id "/attendance-checks/")]
        (POST url
              {:handler #(put! c %)
               :params {:code code}
               :format :raw})))
    c))


(defn rotate [coll start end]
  (let [tail (take-while #(not (= % start)) coll)
        body (->> coll
                  (drop-while #(not (= % start)))
                  (take-while #(not (= % (first tail)))))]
    (concat body tail)))


(defn course-table []
  (let [courses-chan (<get-courses)
        courses (reagent/atom [])
        listening-session (reagent/atom {})]
    (go
      (let [fetched (-> courses-chan <! :courses)]
        (reset! courses fetched)))
    (letfn [(start-attendance [course-id]
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
                        (let [_ (<attendance course-id code)]
                          (.alert js/window "처리되었습니다.")
                          (stop)
                          (.debug js/console (str "posted:" code))))
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


(defn app []
  (fn []
    [:div
     [course-table]]))


(defn mount-components []
  (reagent/render [#'app] (.getElementById js/document "app")))


(defn start []
  (mount-components)
  nil)
