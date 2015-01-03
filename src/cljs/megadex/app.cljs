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
(def routes
  ["/" {"" home-page
        "about" about-page}])

(defn router [routes home-page]
  (let [navigation-c (hook-browser-navigation!)
        current-page (r/atom home-page)]
    (r/create-class
     {:render (fn [_] [:div [@current-page]])
      :component-will-mount
      (fn [_]
        (go-loop []
          (reset! current-page
                  (->> (<! navigation-c)
                       (bidi/match-route routes)
                       :handler))
          (recur)))
      :component-will-unmount (fn [_] (close! navigation-c))})))

;; -------------------------
;; Initialize app
(defn init! []
  (r/render [router routes home-page] (.getElementById js/document "app")))
