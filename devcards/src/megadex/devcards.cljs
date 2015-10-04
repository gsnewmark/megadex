(ns megadex.devcards
  (:require [devcards.core :as dc :refer-macros [defcard]]
            [megadex.app :as app]
            [megadex.app-test]))

(defcard check-rum-render
  (app/calling-component))

(defn init! []
  (devcards.core/start-devcard-ui!))
