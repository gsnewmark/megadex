(ns megadex.dev
  (:require [megadex.app :as app]
            [figwheel.client :as figwheel :include-macros true]
            [weasel.repl :as weasel]
            [reagent.core :as r]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :jsload-callback (fn [] (r/force-update-all)))

(weasel/connect "ws://localhost:9001" :verbose true)

(app/init!)
