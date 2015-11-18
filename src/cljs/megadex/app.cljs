(ns megadex.app
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(defui HelloWorld
  Object
  (render [this]
    (dom/div nil (:title (om/props this)))))

(def hello (om/factory HelloWorld))

(defonce app-state
  (atom
   {:count 0
    :app/title "Animals"
    :animals/list
    [[1 "Ant"] [2 "Antelope"] [3 "Bird"] [4 "Cat"] [5 "Dog"]
     [6 "Lion"] [7 "Mouse"] [8 "Monkey"] [9 "Snake"] [10 "Zebra"]]}))

(defmulti read (fn [env key params] key))

(defmethod read :default
  [{:keys [state] :as env} key params]
  (let [st @state]
    (if-let [[_ value] (find st key)]
      {:value value}
      {:value :not-found})))

(defmethod read :animals/list
  [{:keys [state] :as env} key {:keys [start end]}]
  {:value (subvec (:animals/list @state) start end)})

(defn mutate [{:keys [state] :as env} key params]
  (if (= 'increment key)
    {:value {:keys [:count]}
     :action #(swap! state update-in [:count] inc)}
    {:value :not-found}))

(defui Counter
  static om/IQuery
  (query [this]
    [:count])
  Object
  (render [this]
    (let [{:keys [count]} (om/props this)]
      (dom/div nil
               (dom/span nil (str "Count: " count))
               (dom/button
                #js {:onClick #(om/transact! this '[(increment)])}
                "Click me!")))))

(defui AnimalsList
  static om/IQueryParams
  (params [this]
    {:start 0 :end 10})
  static om/IQuery
  (query [this]
    '[:app/title (:animals/list {:start ?start :end ?end})])
  Object
  (render [this]
    (let [{:keys [app/title animals/list]} (om/props this)]
      (dom/div nil
               (dom/title nil title)
               (apply dom/ul nil
                      (map
                       (fn [[i name]]
                         (dom/li nil (str i ". " name)))
                       list))))))

(def counter-reconciler
  (om/reconciler
   {:state app-state
    :parser (om/parser {:read read :mutate mutate})}))

(def animals-reconciler
  (om/reconciler
   {:state app-state
    :parser (om/parser {:read read :mutate mutate})}))

(def init-data
  {:list/one [{:name "John" :points 0}
              {:name "Mary" :points 0}
              {:name "Bob"  :points 0}]
   :list/two [{:name "Mary" :points 0 :age 27}
              {:name "Gwen" :points 0}
              {:name "Jeff" :points 0}]})

(defmulti read-norm om/dispatch)

(defn get-people [state key]
  (let [st @state]
    (into [] (map #(get-in st %)) (get st key))))

(defmethod read-norm :list/one
  [{:keys [state] :as env} key params]
  {:value (get-people state key)})

(defmethod read-norm :list/two
  [{:keys [state] :as env} key params]
  {:value (get-people state key)})

(defmulti mutate-norm om/dispatch)

(defmethod mutate-norm 'points/increment
  [{:keys [state]} _ {:keys [name]}]
  {:action
   (fn []
     (swap! state update-in
            [:person/by-name name :points]
            inc))})

(defmethod mutate-norm 'points/decrement
  [{:keys [state]} _ {:keys [name]}]
  {:action
   (fn []
     (swap! state update-in
            [:person/by-name name :points]
            #(max 0 (dec %))))})

(defui Person
  static om/Ident
  (ident [this {:keys [name]}]
    [:person/by-name name])
  static om/IQuery
  (query [this]
    '[:name :points])
  Object
  (render [this]
    (let [{:keys [points name] :as props} (om/props this)]
      (println "Render Person" name)
      (dom/li nil
        (dom/label nil (str name ", points: " points))
        (dom/button
         #js {:onClick #(om/transact! this `[(points/increment ~props)])}
         "+")
        (dom/button
         #js {:onClick #(om/transact! this `[(points/decrement ~props)])}
         "-")))))

(def person (om/factory Person {:keyfn :name}))

(defui ListView
  Object
  (render [this]
    (println "Render ListView" (-> this om/path first))
    (let [list (om/props this)]
      (apply dom/ul nil (map person list)))))

(def list-view (om/factory ListView))

(defui RootView
  static om/IQuery
  (query [this]
    (let [subquery (om/get-query Person)]
      `[{:list/one ~subquery} {:list/two ~subquery}]))
  Object
  (render [this]
    (println "Render RootView")
    (let [{:keys [list/one list/two]} (om/props this)]
      (apply dom/div nil
        [(dom/h2 nil "List A")
         (list-view one)
         (dom/h2 nil "List B")
         (list-view two)]))))

(def parser-norm (om/parser {:read read-norm :mutate mutate-norm}))

(def norm-reconciler
  (om/reconciler
   {:state  init-data
    :parser parser-norm}))

(def dashboard-init-data
  {:dashboard/items
   [{:id 0 :type :dashboard/post
     :author "Laura Smith"
     :title "A Post!"
     :content "Lorem ipsum dolor sit amet, quem atomorum te quo"
     :favorites 0}
    {:id 1 :type :dashboard/photo
     :title "A Photo!"
     :image "photo.jpg"
     :caption "Lorem ipsum"
     :favorites 0}
    {:id 2 :type :dashboard/post
     :author "Jim Jacobs"
     :title "Another Post!"
     :content "Lorem ipsum dolor sit amet, quem atomorum te quo"
     :favorites 0}
    {:id 3 :type :dashboard/graphic
     :title "Charts and Stufff!"
     :image "chart.jpg"
     :favorites 0}
    {:id 4 :type :dashboard/post
     :author "May Fields"
     :title "Yet Another Post!"
     :content "Lorem ipsum dolor sit amet, quem atomorum te quo"
     :favorites 0}]})

(defui Post
  static om/IQuery
  (query [this]
    [:title :author :content])
  Object
  (render [this]
    (let [{:keys [title author content] :as props} (om/props this)]
      (dom/div nil
        (dom/h3 nil title)
        (dom/h4 nil author)
        (dom/p nil content)))))

(def post (om/factory Post))

(defui Photo
  static om/IQuery
  (query [this]
    [:title :image :caption])
  Object
  (render [this]
    (let [{:keys [title image caption]} (om/props this)]
      (dom/div nil
        (dom/h3 nil (str "Photo: " title))
        (dom/div nil image)
        (dom/p nil (str "Caption: " caption))))))

(def photo (om/factory Photo))

(defui Graphic
  static om/IQuery
  (query [this]
    [:title :image])
  Object
  (render [this]
    (let [{:keys [title image]} (om/props this)]
      (dom/div nil
        (dom/h3 nil (str "Graphic: " title))
        (dom/div nil image)))))

(def graphic (om/factory Graphic))

(defui DashboardItem
  static om/Ident
  (ident [this {:keys [id type]}]
    [type id])
  static om/IQuery
  (query [this]
    (zipmap
     [:dashboard/post :dashboard/photo :dashboard/graphic]
     (map #(conj % :favorites :id :type)
          [(om/get-query Post)
           (om/get-query Photo)
           (om/get-query Graphic)])))
  Object
  (render [this]
    (let [{:keys [id type favorites] :as props} (om/props this)]
      (dom/li
       #js {:style #js {:padding 10 :borderBottom "1px solid black"}}
       (dom/div nil
         (({:dashboard/post    post
            :dashboard/photo   photo
            :dashboard/graphic graphic} type)
          props))
       (dom/div nil
         (dom/p nil (str "Favorites: " favorites))
         (dom/button
          #js {:onClick
               #(om/transact! this
                  `[(dashboard/favorite {:ref [~type ~id]})])}
          "Favorite!"))))))

(def dashboard-item (om/factory DashboardItem))

(defui Dashboard
  static om/IQuery
  (query [this]
    [{:dashboard/items (om/get-query DashboardItem)}])
  Object
  (render [this]
    (let [{:keys [dashboard/items]} (om/props this)]
      (apply dom/ul
        #js {:style #js {:padding 0}}
        (map dashboard-item items)))))

(defmulti read-dashboard om/dispatch)

(defmethod read-dashboard :dashboard/items
  [{:keys [state]} k _]
  (let [st @state]
    {:value (into [] (map #(get-in st %)) (get st k))}))

(defmulti mutate-dashboard om/dispatch)

(defmethod mutate-dashboard 'dashboard/favorite
  [{:keys [state]} k {:keys [ref]}]
  {:action
   (fn []
     (swap! state update-in (conj ref :favorites) inc))})

(def dashboard-reconciler
  (om/reconciler
   {:state  dashboard-init-data
    :parser (om/parser {:read read-dashboard :mutate mutate-dashboard})}))

(defn init! []
  (when-let [el (gdom/getElement "container")]
    (js/ReactDOM.render
     (apply dom/div nil
            (map #(hello {:title (str "Hello, world #" %)})
                 (range 3)))
     el))
  (when-let [el (gdom/getElement "counter-container")]
    (om/add-root! counter-reconciler Counter el))
  (when-let [el (gdom/getElement "animals-container")]
    (om/add-root! animals-reconciler AnimalsList el))
  (when-let [el (gdom/getElement "norm-container")]
    (om/add-root! norm-reconciler RootView el))
  (when-let [el (gdom/getElement "dashboard-container")]
    (om/add-root! dashboard-reconciler Dashboard el)))
