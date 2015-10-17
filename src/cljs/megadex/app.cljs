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
    {:value [:count]
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
                #js {:onClick
                     (fn [e]
                       (om/transact! this '[(increment)]))}
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
    (om/add-root! animals-reconciler AnimalsList el)))
