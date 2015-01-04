(ns megadex.app
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [put! chan <! close!]]
            [reagent.core :as r]
            [datascript :as d]
            [bidi.bidi :as bidi]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to megadex"]
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About megadex"]
   [:div [:a {:href "#/"} "go to the home page"]]])

;; -------------------------
;; History
(defn hook-browser-navigation! []
  (let [navigation-chan (chan)]
    (doto (History.)
      (events/listen
       EventType/NAVIGATE
       (fn [event] (put! navigation-chan (.-token event))))
      (.setEnabled true)
      (.setToken "/"))
    navigation-chan))

;; -------------------------
;; Routes
(defn routes []
  ["/" {"" home-page
        "about" about-page}])

(defn router [routes-fn]
  (let [navigation-c (hook-browser-navigation!)
        current-page (r/atom "/")]
    (r/create-class
     {:render
      (fn [_] [:div [(->> @current-page (bidi/match-route (routes-fn)) :handler)]])
      :component-will-mount
      (fn [_] (go-loop [] (reset! current-page (<! navigation-c)) (recur)))
      :component-will-unmount (fn [_] (close! navigation-c))})))

;; -------------------------
;; Initialize app
(defn init! []
  (r/render [router routes] (.getElementById js/document "app")))
