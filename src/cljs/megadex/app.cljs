(ns megadex.app
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<! close!]]
            [datascript :as d]
            [megadex.util :as u]
            [reagent.core :as r]))

(defn personas-list-for-arcana [[arcana personas]]
  [:div
   [:h3 arcana]
   [:ul
    (for [persona personas]
      ^{:key persona} [:li persona])]])

(defn arcanas [conn]
  (let [q '[:find ?arcana ?persona ?level
            :where
            [?aid :arcana/name ?arcana]
            [?pid :persona/arcana ?aid]
            [?pid :persona/name ?persona]]
        arcanas (u/bind conn q)]
    (fn []
      (let [as (->> @arcanas
                    (group-by first)
                    (u/map-over-vals #(map second %))
                    (sort-by first))]
        [:div
         (for [a as]
           ^{:key (first a)} [personas-list-for-arcana a])]))))

(defn loading []
  [:div.loading "Loading..."])

(defn routes [conn]
  (fn []
    ["/" {"" (arcanas conn)}]))

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
