(ns megadex.util
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<! chan close! put! sliding-buffer]]
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
  (let [navigation-chan (chan (sliding-buffer 1))
        history (History.)]
    (doto history
      (events/listen
       EventType/NAVIGATE
       (fn [event] (put! navigation-chan (.-token event))))
      (.setEnabled true))
    [history navigation-chan]))


(defn bind-to-state
  [conn state q & args]
  (let [k (uuid/make-random)
        rq (fn [c] (if (seq? args)
                     (apply d/q q c args)
                     (d/q q c)))]
    (reset! state (rq @conn))
    (d/listen! conn k (fn [tx-report]
                        (let [novelty (rq (:tx-data tx-report))]
                          (when (not-empty novelty)
                            (reset! state (rq (:db-after tx-report)))))))
    (set! (.-__key state) k)
    state))

(defn bind
  ([conn q]
   (bind-to-state conn (r/atom nil) q))
  ([conn q & args]
   (apply bind-to-state conn (r/atom nil) q args)))

(defn unbind
  [conn state]
  (d/unlisten! conn (.-__key state)))


(defn link [uri body]
  [:a {:href (str "#" uri)} body])
