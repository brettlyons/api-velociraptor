(ns api-velociraptor.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            ;; [cljsjs.chartist]
            ;; [cljsjs.d3]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :as ajax :refer [GET POST]])
  (:import goog.History))

(defn nav-link [uri title page collapsed?]
  [:li {:class (when (= page (session/get :page)) "active")}
   [:a {:href uri
        :on-click #(reset! collapsed? true)}
    title]])

(defn navbar []
  (let [collapsed? (atom true)]
    (fn []
      [:nav.navbar.navbar-inverse.navbar-fixed-top
       [:div.container
        [:div.navbar-header
         [:button.navbar-toggle
          {:class         (when-not @collapsed? "collapsed")
           :data-toggle   "collapse"
           :aria-expanded @collapsed?
           :aria-controls "navbar"
           :on-click      #(swap! collapsed? not)}
          [:span.sr-only "Toggle Navigation"]
          [:span.icon-bar]
          [:span.icon-bar]
          [:span.icon-bar]]
         [:a.navbar-brand {:href "#/"} "API Velociraptor"]]
        [:div.navbar-collapse.collapse
         (when-not @collapsed? {:class "in"})
         [:ul.nav.navbar-nav
          [nav-link "#/" "Home" :home collapsed?]
          [nav-link "#/results" "Current Results" :results collapsed?]
          [nav-link "#/about" "About" :about collapsed?]]]]])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "This app is for defining load-testing targets and showing load-testing results"]]])

(defonce counter (reagent/atom 0))
(defonce test-info (reagent/atom {}))

(defn del-api-test [id]
  (swap! test-info update-in [:testing] dissoc id)) 

(defn get-test [id]
  (get-in @test-info [:testing id]))

(defn get-testing-name [id]
  (get-in @test-info [:testing id :name]))

(defn get-testing-addr [id]
  (get-in @test-info [:testing id :address]))

(defn get-testing-method [id]
  (get-in @test-info [:testing id :method]))

(defn get-testing-payload [id]
  (get-in @test-info [:testing id :payload]))

(defn get-testing-results [id]
  (get-in @test-info [:testing id :results]))

(defn get-testing-errors [id]
  (get-in @test-info [:testing id :errors]))

(defn push-to-results [id result]
  (swap! test-info update-in [:testing id :results] conj result)
  (println (get-testing-results id)))

(defn push-to-errors [id err]
  (swap! test-info update-in [:testing id :errors] conj err)
  (println (get-testing-errors id)))

(defn clear-all-results []
  (println "ETC")
  (for [test (:testing @test-info)]
    (let [testId (first test)]
         (swap! test-info update-in [:testing testId :results] vec [])))
  (println "TESTING: "(get-in @test-info [:testing 1 :results])))

(defn clear-all-errors []
  (for [test (:testing @test-info)]
    (swap! test-info update-in [:testing (first test) :errors] vec []))
  (println "CLEAR ALL ERRORS TESTING" (get-in @test-info [:testing])))
  
(defn set-editing! [& test-id]
  (swap! test-info assoc :editing
         (get (:testing @test-info) (first test-id) {:address "http://" :name nil :payload nil :method  "GET" :results [] :errors []}))
  (del-api-test (first test-id)))

(defn add-api-test! []
  (let [id (swap! counter inc)]
    (swap! test-info assoc-in [:testing id] (:editing @test-info))
    (set-editing!)))

(defonce init (do
                (set-editing!)))

(defn make-ajax-call [id & n-calls]
  (let [address (get-testing-addr id) method (get-testing-method id) payload (get-testing-payload id) start (.now js/Date)]
    ;; (println (str "N CALLS: " @n-calls))
    (if (= method "GET")
      (ajax/GET address {:timeout 60
                         :handler (fn [res]
                                    (println (str "response " res " and the Time to respond was: " (- (.now js/Date) start))
                                             (push-to-results id (- (.now js/Date) start))
                                             (get-testing-results id))
                                    (if (< (+ (count (get-testing-errors id)) (count (get-testing-results id))) 1000)
                                      (make-ajax-call id)
                                      nil))
                :error-handler (fn [res]
                                    (println (str "ERROR response" res " and the Time to respond was: " (- (.now js/Date) start))
                                             (push-to-errors id (- (.now js/Date) start))
                                             (get-testing-errors id))
                                    (if (< (+ (count (get-testing-errors id)) (count (get-testing-results id))) 1000)
                                      (make-ajax-call id)
                                      nil))})
      (ajax/POST address {:body payload
                          :handler (fn [res]
                                    (println (str "response " res " and the Time to respond was: " (- (.now js/Date) start))
                                             (push-to-results id (- (.now js/Date) start))
                                             (get-testing-results id))
                                    (if (< (+ (count (get-testing-errors id)) (count (get-testing-results id))) 1000)
                                      (make-ajax-call id)
                                      nil))
                          :error-handler (fn [res]
                                    (println (str "ERROR response " res " and the Time to respond was: " (- (.now js/Date) start))
                                             (push-to-errors id (- (.now js/Date) start))
                                             (get-testing-errors id))
                                    (if (< (+ (count (get-testing-errors id)) (count (get-testing-results id))) 1000)
                                      (make-ajax-call id)
                                      nil))}))))

(defn edit-btns []
  [:div.col-md-8
   [:button.btn.btn-primary.btn-lg {:on-click #(;(if ())
                                                (add-api-test!)
                                                (.. js/document (querySelector "input[name='url-form']") (focus)))}
    [:span.glyphicon.glyphicon-plus] (str " API To List")]
   [:button.btn.btn-info.btn-lg.pull-right {:on-click #(set-editing! nil)} "Clear Edits"]])

(defn api-form []
  (fn []
    [:div.row
     [:div.col-md-8
      [:div.btn-group {:role "group" :id (str "method-" (:editing @test-info))}
       [:button.btn.btn-primary {:type "button"
                                 :name "get-btn"
                                 :value "GET"
                                 :disabled (= (get-in @test-info [:editing :method]) "GET") 
                                 :on-click #((swap! test-info assoc-in [:editing :method] (.-target.value %))
                                             ())} "GET"] ;; empty parens if other lists/fns are needed
       ;; (println (get-in @test-info [:editing :method])))} "GET"]
       [:button.btn.btn-primary {:type "button"
                                 :name "get-btn"
                                 :value "POST"
                                 :disabled (= (get-in @test-info [:editing :method]) "POST") 
                                 :on-click #((swap! test-info assoc-in [:editing :method] (.-target.value %))
                                             ())} "POST"]] ;; empty parens if other lists/fns are needed 
      ;; (println (get-in @test-info [:editing :method])))} "POST"]]
      
      [:p]
      [:label "Full URL"]
      [:div.input-group
       [:span.input-group-addon "Url: "]
       [:input.form-control {:type "URL"
                             :name "url-form"
                             :value (get-in @test-info [:editing :address])
                             :on-change #(swap! test-info assoc-in [:editing :address] (.-target.value %))}]]


      [:label "Name of the API to appear on the graph"]
      [:div.input-group
       [:span.input-group-addon "Name: "]
       [:input.form-control {:type "text"
                             :value (get-in @test-info [:editing :name])
                             :on-change #(swap! test-info assoc-in [:editing :name] (.-target.value %))}]]]
     [:div.col-md-4
      [:p]
      [:div {:style {:visibility ((fn []
                                    (if (= (get-in @test-info [:editing :method]) "POST")
                                      "visible"
                                      "hidden"))) }}
       [:label "Payload for POST"]
       [:div.input-group 
        [:textarea.form-control {:type "textarea"
                                 :disabled (= (get-in @test-info [:editing :method]) "GET")
                                 :rows 5
                                 :value (get-in @test-info [:editing :payload])
                                 :on-change #(swap! test-info assoc-in  [:editing :payload] (.-target.value %))}]]]]]))

(defn rm-test-btn [id]
  [:span.glyphicon.destroy.glyphicon-trash {;:style {:color "white"}
                                            :title "Delete?"
                                            :on-click #(del-api-test id)}])
(defn edit-test-btn [id]
  [:span.glyphicon.destroy.glyphicon-pencil.pull-right {;:style {:color "white"}
                                                        :title "Edit?"
                                                        :on-click #(set-editing! id)}])

(defn name-capsule [id]
  [:div.row 
   [:p]
   [:div.col-md-12.capsule.capsule-small {:style {:cursor "pointer"} 
                                          :title "Click To Edit"
                                          :on-click #(set-editing! id)} (str (get-testing-name id))
    [:div.row
     [edit-test-btn id]
     [rm-test-btn id]]]])

(defn home-page []
  ;;(let [n-calls (reagent/atom false)]
    (fn [] 
      [:div.container
       ;; [:div.row
       ;;  [:h2 {:style {:background-color "tomato"}} (str "DEVINFO: " @test-info " counter: " @counter)]]
       [:div.row
        [:div.col-md-2
         [:h3.capsule.capsule-bar  "API's"]
         (doall (for [test1 (:testing @test-info)]
                  ^{:key (first test1)} [name-capsule (first test1)]))]
        [:div.col-md-10
         [:h3.capsule.capsule-bar "Edit API Info"]
         [api-form]
         [:div.row
          [:p]
          [edit-btns]]]]
       [:p]
       [:div.row
        [:div.col-md-12.capsule.capsule-bar]]
       [:p]
       [:div.row
        ;; [:div.col-md-10
        ;;  [:div.input-group.pull-right
        ;;   [:span.input-group-addon "Number of Requests: "]
        ;;   [:input.form-control {:type "number"
        ;;                         :value @n-calls
        ;;                         :on-change #(reset! n-calls (-> % .-target .-value))}]]]
         [:a {:href "#/results"}
          [:button.btn.btn-lg.btn-danger.pull-right {:on-click (fn [e]
                                                                 (doall (for [test (:testing @test-info)]
                                                                          (let [testId (first test)]
                                                                            (make-ajax-call testId)))))} "Run the Test" ]]]]))

(defn bar-for-chart [id]
  (let [perf (get-testing-results id) label (get-testing-name id) errors (get-testing-errors id)]
    [:div.row
     [:p label]
     [:div.progress
      [:div.progress-bar.progress-bar-striped.active {:style
                                                      {
                                                       ;; :height (str "10%")
                                                       :width (str (/ (count perf) 10) "%")
                                                       ;; :background-color "grey"
                                                       }}] 
      [:div.progress-bar.progress-bar-striped.progress-bar-warning {:style
                                                                    {:width (str (/ (count errors) 10) "%")}}]
      ]
     [:p (str "Avg. Time For Response in ms: " (/ (reduce + perf) (count perf)))]
     [:p {:style {:color "red"}}"ERRORS: " (count errors)]]))

(defn results []
  [:div.container
   ;; [chartist-bar]
   [:div.row
    [:h2 "THE RESULTS PAGE"]
    [:div.well
     (doall (for [test1 (:testing @test-info)]
              ^{:key (first test1)} [bar-for-chart (first test1)]))]]
   [:div.row
    [:div.col-md-12
     [:button.btn.btn-lg.btn-info.pull-right {:on-click (fn [e] )} "Clear Results"]]]])

(def pages
  {:home #'home-page
   :results #'results
   :about #'about-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/results" []
  (session/put! :page :results))

(secretary/defroute "/about" []
  (session/put! :page :about))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET (str js/context "/docs") {:handler #(session/put! :docs %)}))

(defn mount-components []
  (reagent/render [#'navbar] (.getElementById js/document "navbar"))
  (reagent/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
