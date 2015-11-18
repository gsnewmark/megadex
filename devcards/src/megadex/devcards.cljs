;;; TODO fix devcards
(ns megadex.devcards
  (:require [devcards.core :as dc :refer-macros [defcard]]
            [megadex.app :as app]
            [megadex.app-test]))

(defcard check-render
  (app/hello))

(defn init! []
  (devcards.core/start-devcard-ui!))
