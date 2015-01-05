(ns megadex.app
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<! close!]]
            [datascript :as d]
            [megadex.util :as util]
            [reagent.core :as r]))

(defn home-page []
  [:div [:h2 "Welcome to megadex"]
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About megadex"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn loading []
  [:div.loading "Loading..."])

(defn routes []
  ["/" {"" home-page
        "about" about-page}])

(defn init! []
  (let [navigation (util/hook-browser-navigation!)
        dom (.getElementById js/document "app")]
    (r/render [loading] dom)
    (go-loop []
      (if-let [{:keys [schema fixture]} (<! (util/fetch-edn "/fixtures/p4g.edn"))]
        (let [conn (d/create-conn schema)]
          (d/transact! conn fixture)
          (r/render [util/router navigation routes] dom))
        (recur)))))
