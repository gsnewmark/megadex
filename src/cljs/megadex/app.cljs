(ns megadex.app
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [bidi.bidi :as bidi]
            [cljs.core.async :refer [<! close!]]
            [datascript :as d]
            [megadex.query :as q]
            [megadex.util :as u]
            [reagent.core :as r]))

(def routes
  ["/" {"" ::arcanas-page
        ["arcana/" :arcana] {"" ::arcana-page
                             ["/persona/" :persona] ::persona-page}}])


(defn persona-page [conn name]
  (let [persona-q (u/bind conn q/persona name)]
    (fn []
      (let [[arcana level
             st ma en ag lu
             inherit resists block absorbs reflects weak] (first @persona-q)]
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
         [:p [:b "Weak: "] weak]]))))

(defn arcana-page [conn name]
  (let [arcana-q (u/bind conn q/arcana-with-personas name)]
    (fn []
      [:div
       (for [[persona] (sort-by second @arcana-q)]
         ^{:key persona}
         [persona-page conn persona])])))

(defn arcana-overview [[arcana personas]]
  [:div
   (u/link (bidi/path-for routes ::arcana-page :arcana arcana)
           [:h3 arcana])
   [:ul
    (for [[persona level] (sort-by second personas)]
      ^{:key persona}
      [:li level " - "
       (u/link (bidi/path-for routes ::persona-page
                              :arcana arcana :persona persona)
               persona)])]])

(defn arcanas-page [conn]
  (let [arcanas-q (u/bind conn q/arcanas-with-personas)]
    (fn []
      (let [arcanas (->> @arcanas-q
                         (group-by first)
                         (u/map-over-vals #(map rest %))
                         (sort-by first))]
        [:div
         (for [arcana arcanas]
           ^{:key (first arcana)} [arcana-overview arcana])]))))

(defn loading-page []
  [:div.loading "Loading..."])


(defn router [navigation-c routes conn]
  (let [current-page (r/atom "/")]
    (go-loop []
      (reset! current-page (<! navigation-c))
      (recur))
    (fn [_]
      (let [{:keys [handler route-params] :as m}
            (bidi/match-route routes @current-page)]
        [:div
         (condp = handler
           ::arcanas-page [arcanas-page conn]
           ::arcana-page [arcana-page conn (:arcana route-params)]
           ::persona-page [persona-page conn (:persona route-params)])]))))


(defn init! []
  (let [navigation (u/hook-browser-navigation!)
        dom (.getElementById js/document "app")]
    (r/render [loading-page] dom)
    (go-loop []
      (if-let [{:keys [schema fixture]} (<! (u/fetch-edn "/fixtures/p4g.edn"))]
        (let [conn (d/create-conn schema)]
          (d/transact! conn fixture)
          (r/render [router navigation routes conn] dom))
        (recur)))))
