(ns attendance-check.dashboard
  "Tutor side smpale app
  it only broadcasts sound signal to student app. (no receive logic)"

  (:require [attendance-check.codec :refer [encode]]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [chan put! <! close!]]
            [cljs-time.core :refer [time-now]]
            [cljs-time.format :refer [formatter unparse]]
            [reagent.core :as reagent]
            [sory.socket :refer [initialize-socket]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


;; initialize-socket only once.
(defonce sory-socket (initialize-socket))
(defonce time-formatter (formatter "yyyy-MM-dd HH:mm:ss"))


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


(defn <attendance-stream [event-source]
  (let [c (chan)]
    (go
      (.addEventListener event-source "message" #(put! c %)))
    c))


(defn broadcast-sound [code]
  (let [message (encode code)]
    (.debug js/console (str "broadcated: " code))
    (.broadcast! sory-socket message)
    (.setInterval js/window
                  #(.broadcast! sory-socket message)
                  (* (count message) 1000))))


(defn students-section [props]
  (let [checked-students (reagent/atom {})
        timer-id (reagent/atom nil)
        event-source (atom nil)
        stream (atom nil)]
    (letfn [(start-check [course-id]
              (reset! checked-students {})
              (let [check-url (str "/dashboard/courses/"
                                   course-id
                                   "/attendance-checks/")]
                (go
                  (let [check (-> check-url
                                  <register-check-session
                                  <!)
                        check-id (get check "_id")
                        stream-url (str check-url check-id "/")
                        code (get check "code")]
                    (reset! event-source (new js/EventSource stream-url))
                    (reset! stream (<attendance-stream @event-source))
                    (reset! timer-id (broadcast-sound code))
                    (go-loop []
                      (let [student (->> @stream
                                         <!
                                         (.-data)
                                         (.parse js/JSON)
                                         (.-student))]
                        (swap! checked-students assoc
                               (.-_id student)
                               (time-now))
                        (recur)))))))
            (stop-check []
              (.clearInterval js/window @timer-id)
              (.close @event-source)
              (close! @stream)
              (reset! event-source nil)
              (reset! stream nil)
              (reset! timer-id nil))]
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
                   [:td
                    (when-let [attendance-at (get @checked-students (:_id s))]
                      (unparse time-formatter attendance-at))]]))
                [:tr
                 [:td
                  {:colSpan "2"
                   :style {:text-align "center"}}
                  "수강 중인 학생이 없습니다."]])]]

           (when (not (empty? students))
             (if (nil? @timer-id)
               [:button
                {:class "btn btn-default btn-lg"
                 :style {:width "100%"}
                 :type "button"
                 :on-click #(start-check (:_id course))}
                "출석 부르기"]
               [:button
                {:class "btn btn-default btn-lg"
                 :style {:width "100%"}
                 :type "button"
                 :on-click stop-check}
                "그만 부르기"]))])))))


(defn app
  "Root component of dashboard app."
  []
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


(defn start
  "Entry point of dashboard app."
  []
  (mount-components)
  nil)
