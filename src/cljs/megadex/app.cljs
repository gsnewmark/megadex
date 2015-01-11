(ns megadex.app
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [bidi.bidi :as bidi]
            [cljs.core.async :refer [<! close!]]
            [datascript :as d]
            [megadex.query :as q]
            [megadex.util :as u]
            [reagent.core :as r]
            [reagent-forms.core :refer [bind-fields]]))

(def routes
  ["/" {"" ::arcanas-page
        ["arcana/" :arcana] {"" ::arcana-page
                             ["/persona/" :persona] ::persona-page}}])

(defn path-for-arcana [arcana-normalized]
  (bidi/path-for routes ::arcana-page :arcana arcana-normalized))

(defn path-for-persona [arcana-normalized persona-normalized]
  (bidi/path-for routes ::persona-page
                 :arcana arcana-normalized :persona persona-normalized))


(defn skill-view [[name level effect cost]]
  [:div level " - " [:b name] ": " effect " (" cost ")"])

(defn persona-page [conn normalized-name]
  (let [persona-q (u/bind conn q/persona normalized-name)
        skills-q (u/bind conn q/skills-of-persona normalized-name)]
    (fn []
      (let [[name arcana level
             st ma en ag lu
             inherit resists block absorbs reflects weak] (first @persona-q)

             sorted-skills
             (sort-by second
                      #(cond
                         (= %1 "Innate") -1
                         (= %2 "Innate") 1
                         :default (- (js/parseInt %1) (js/parseInt %2)))
                      @skills-q)]
        [:div
         [:h3 name]
         [:p [:b "Arcana: "] arcana]
         [:p [:b "Level: "] level]
         [:p [:b "ST: "] st]
         [:p [:b "MA: "] ma]
         [:p [:b "EN: "] en]
         [:p [:b "AG: "] ag]
         [:p [:b "LU: "] lu]
         [:p [:b "Inherit: "] inherit]
         [:p [:b "Resists: "] resists]
         [:p [:b "Block: "] block]
         [:p [:b "Absorbs: "] absorbs]
         [:p [:b "Reflects: "] reflects]
         [:p [:b "Weak: "] weak]
         [:h4 "Skills"]
         [:div
          (for [skill sorted-skills]
            ^{:key (first skill)}
            (skill-view skill))]]))))

(defn arcana-page [conn normalized-name]
  (let [arcana-q (u/bind conn q/arcana-with-personas normalized-name)]
    (fn []
      [:div
       (for [[persona] (sort-by second @arcana-q)]
         ^{:key persona}
         [persona-page conn persona])])))

(defn arcana-overview [[[arcana arcana-normalized] data]]
  [:div
   (u/link (path-for-arcana arcana-normalized) [:h3 arcana])
   [:ul
    (for [[persona persona-normalized level] (sort-by #(nth % 2) data)]
      ^{:key persona}
      [:li level " - "
       (u/link (path-for-persona arcana-normalized persona-normalized)
               persona)])]])

(defn links-for [arcanas]
  (->> (mapcat (fn [[[a an] ps]]
                 (conj (map (fn [[p pn]] [p (path-for-persona an pn)]) ps)
                       [a (path-for-arcana an)]))
               arcanas)
       (into {})))

(defn quick-select-view [history arcanas]
  (let [links (links-for arcanas)
        template
        [:div {:field :typeahead :id :selected
               :data-source (u/typeahead-source (keys links))}]
        data (r/atom {})]
    (fn []
      (let [selected (:selected @data)
            link (get links selected)]
        [:div
         [:h2 "Quick select"]
         [bind-fields template data
          (fn [[id] value {:keys [selected]}]
            (when (and (= id :selected) selected)
              (when-let [link  (get links value)]
                (.setToken history link))))]]))))

(defn arcanas-page [conn history]
  (let [arcanas-q (u/bind conn q/arcanas-with-personas)]
    (fn []
      (let [arcanas (->> @arcanas-q
                         (map (partial split-at 2))
                         (group-by first)
                         (u/map-over-vals #(map second %))
                         (sort-by (comp first first)))]
        [:div
         [quick-select-view history arcanas]
         (for [arcana arcanas]
           ^{:key (first (first arcana))} [arcana-overview arcana])]))))

(defn loading-page []
  [:div.loading "Loading..."])


(defn router [history navigation-c routes conn]
  (let [current-page (r/atom "/")]
    (go-loop []
      (reset! current-page (<! navigation-c))
      (recur))
    (fn [_]
      (let [{:keys [handler route-params] :as m}
            (bidi/match-route routes @current-page)]
        [:div
         (condp = handler
           ::arcanas-page [arcanas-page conn history]
           ::arcana-page [arcana-page conn (:arcana route-params)]
           ::persona-page [persona-page conn (:persona route-params)])]))))


(defn init! []
  (let [[history navigation] (u/hook-browser-navigation!)
        dom (.getElementById js/document "app")]
    (when (= "" (.getToken history))
      (.setToken history "/"))
    (when (.-orientation js/window)
      (.initializeTouchEvents js/React true))
    (r/render [loading-page] dom)
    (go-loop []
      (if-let [{:keys [schema fixture]} (<! (u/fetch-edn "/fixtures/p4g.edn"))]
        (let [conn (d/create-conn schema)]
          (d/transact! conn fixture)
          (r/render [router history navigation routes conn] dom))
        (recur)))))
