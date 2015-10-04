(ns megadex.app
  (:require [rum.core :as rum]))

(rum/defc some-component []
  [:div
   [:h3 "I am a component!"]
   [:p.someclass
    "I have " [:strong "bold"]
    [:span {:style {:color "red"}} " and red"]
    " text."]])

(rum/defc calling-component []
  [:div "Parent component"
   (some-component)])

(defn init! []
  (when-let [el (.getElementById js/document "container")]
    (rum/mount (calling-component) el)))
