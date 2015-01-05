(ns megadex.app
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<! close!]]
            [datascript :as d]
            [megadex.util :as u]
            [reagent.core :as r]))

(defn home-page [conn]
  (let [q '[:find ?persona
            :where
            [_ :persona/name ?persona]]
        personas (u/bind conn q)]
    (fn []
      (let [ps (sort-by first @personas)]
        [:div
         [:h2 "Welcome to megadex"]
         [:h3 (str "Personas (" (count ps) "):")]
         [:ul (map (fn [[persona]] [:li persona]) ps)]
         [:div [:a {:href "#/about"} "go to about page"]]]))))

(defn about-page []
  [:div [:h2 "About megadex"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn loading []
  [:div.loading "Loading..."])

(defn routes [conn]
  (fn []
    ["/" {"" (home-page conn)
          "about" about-page}]))

(defn init! []
  (let [navigation (u/hook-browser-navigation!)
        dom (.getElementById js/document "app")]
    (r/render [loading] dom)
    (go-loop []
      (if-let [{:keys [schema fixture]} (<! (u/fetch-edn "/fixtures/p4g.edn"))]
        (let [conn (d/create-conn schema)]
          (d/transact! conn fixture)
          (r/render [u/router navigation (routes conn)] dom))
        (recur)))))
