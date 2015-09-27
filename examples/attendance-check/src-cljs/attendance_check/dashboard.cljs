(ns attendance-check.dashboard
  (:require [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [chan put! <!]]
            [reagent.core :as reagent])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(defn <get-courses []
  (let [c (chan)]
    (go
      (GET "/dashboard/courses/"
           {:handler #(put! c %)
            :response-format :json
            :keywords? true}))
    c))


(defn courses-form [props]
  (let [courses-chan (<get-courses)
        state (reagent/atom {:courses []})
        change-fn (:on-change props)]
    (go
      (let [courses (-> courses-chan <! :courses)
            keyed-courses (zipmap (map :_id courses) courses)]
        (change-fn (first courses))
        (swap! state assoc :courses keyed-courses)))

    (fn [props]
      [:div
       [:form
        {:class "form-inline pull-right"}
        [:label
         {:for "course-name"} "과목: "]
        [:div
         {:class "form-group"}
         [:select
          {:id "course-name"
           :class "form-control"
           :name "course"
           :on-change #(let [course-id (-> % .-target .-value)
                             course (get (:courses @state) course-id)]
                         (change-fn course))}
          (for [c (vals (:courses @state))]
            ^{:key c}
            [:option
             {:value (:_id c)}
             (:name c)])]
         ]]])))


(defn <register-check-session [check-url]
  (let [c (chan)]
    (go
      (POST check-url
            {:handler #(put! c (% "check"))
             :error-handler #(.alert js/window %)}))
    c))


(defn <attendance-stream [stream-url]
  (let [c (chan)]
    (go
      (let [event-source (new js/EventSource stream-url)]
        (.addEventListener event-source "message" #(put! c %))))
    c))


(defn students-section [props]
  (let [checked-students (reagent/atom #{})]
    (letfn [(start-check [course-id]
              (reset! checked-students #{})
              (let [check-url (str "/dashboard/courses/"
                                   course-id
                                   "/attendance-checks/")]
                (go
                  (let [check-id (-> check-url
                                     <register-check-session
                                     <!
                                     (get "_id"))
                        stream-url (str check-url check-id "/")
                        stream (<attendance-stream stream-url)]
                    (go-loop []
                      (let [student (->> stream
                                         <!
                                         (.-data)
                                         (.parse js/JSON)
                                         (.-student))]
                        (swap! checked-students conj (.-_id student))
                        (recur)))))))]
      (fn [props]
        (let [{:keys [students course]} props]
          [:div
           [:table {:class "table"}
            [:thead
             [:tr
              [:th {:style {:width "70%"}} "이름"]
              [:th {:style {:width "30%"}} "출석"]]]
            [:tbody
             (if-let [students (:students props)]
               (doall
                (for [s students]
                  ^{:key s}
                  [:tr
                   [:td (:name s)]
                   [:td (if (@checked-students (:_id s)) "출석" "미확인")]]))
                [:tr
                 [:td
                  {:colSpan "2"
                   :style {:text-align "center"}}
                  "수강 중인 학생이 없습니다."]])]]

           (when (not (empty? students))
             [:button
              {:class "btn btn-default btn-lg"
               :style {:width "100%"}
               :type "button"
               :on-click #(start-check (:_id course))}
              "출석 부르기"])])))))


(defn app []
  (let [state (reagent/atom {:coruse nil})]
    (fn []
      (let [course (:course @state)
            students (:students course)]
        [:div
         [:nav
          [courses-form {:on-change #(swap! state assoc :course %)}]]
         [students-section {:students students
                            :course course}]]))))


(defn mount-components []
  (reagent/render [#'app] (.getElementById js/document "app")))


(defn start []
  (mount-components)
  nil)
