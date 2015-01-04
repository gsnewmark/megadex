(ns megadex.util
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [bidi.bidi :as bidi]
            [cljs.core.async :refer [<! chan close! put!]]
            [cljs.reader :refer [read-string]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as r])
  (:import goog.History goog.net.XhrIo))

(defn hook-browser-navigation! []
  (let [navigation-chan (chan)]
    (doto (History.)
      (events/listen
       EventType/NAVIGATE
       (fn [event] (put! navigation-chan (.-token event))))
      (.setEnabled true)
      (.setToken "/"))
    navigation-chan))

(defn fetch-edn [url]
  (let [c (chan)]
    (.send XhrIo url
           (fn [e] (put! c (read-string (.getResponseText (.-target e))))))
    c))

(defn router [routes-fn]
  (let [navigation-c (hook-browser-navigation!)
        current-page (r/atom "/")]
    (r/create-class
     {:render
      (fn [_]
        [:div [(->> @current-page (bidi/match-route (routes-fn)) :handler)]])
      :component-will-mount
      (fn [_]
        (go-loop []
          (reset! current-page (<! navigation-c))
          (recur)))
      :component-will-unmount (fn [_] (close! navigation-c))})))

