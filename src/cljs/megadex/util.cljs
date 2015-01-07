(ns megadex.util
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [bidi.bidi :as bidi]
            [cljs.core.async :refer [<! chan close! put! sliding-buffer]]
            [cljs.reader :refer [read-string]]
            [cljs-uuid.core :as uuid]
            [datascript :as d]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as r])
  (:import goog.History goog.net.XhrIo))

(defn map-over-vals [f m]
  (into {} (for [[k v] m] [k (f v)])))


(defn fetch-edn [url]
  (let [c (chan (sliding-buffer 1))]
    (.send XhrIo url
           (fn [e] (put! c (read-string (.getResponseText (.-target e))))))
    c))


(defn hook-browser-navigation! []
  (let [navigation-chan (chan (sliding-buffer 1))]
    (doto (History.)
      (events/listen
       EventType/NAVIGATE
       (fn [event] (put! navigation-chan (.-token event))))
      (.setEnabled true)
      (.setToken "/"))
    navigation-chan))


(defn router [navigation-c routes-fn]
  (let [current-page (r/atom "/")]
    (go-loop []
      (reset! current-page (<! navigation-c))
      (recur))
    (fn [_]
      [:div [(->> @current-page (bidi/match-route (routes-fn)) :handler)]])))


(defn bind
  ([conn q]
   (bind conn q (r/atom nil)))
  ([conn q state]
   (let [k (uuid/make-random)]
     (reset! state (d/q q @conn))
     (d/listen! conn k (fn [tx-report]
                         (let [novelty (d/q q (:tx-data tx-report))]
                           (when (not-empty novelty)
                             (reset! state (d/q q (:db-after tx-report)))))))
     (set! (.-__key state) k)
     state)))

(defn unbind
  [conn state]
  (d/unlisten! conn (.-__key state)))
