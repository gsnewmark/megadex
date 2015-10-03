(task-options!
 pom {:project     'megadex
      :version     "0.2.0-SNAPSHOT"
      :description "Web application with Persona 4 Golden personas description"
      :url         "https://example.com/FIXME"
      :scm         {:url "https://github.com/gsnewmark/megadex"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}})

(set-env!
 :source-paths    #{"src/cljs" "src/clj"}
 :resource-paths  #{"resources"}
 :dependencies '[[adzerk/boot-cljs              "0.0-3308-0"     :scope "test"]
                 [adzerk/boot-cljs-repl         "0.2.0"          :scope "test"]
                 [adzerk/boot-reload            "0.3.2"          :scope "test"]
                 [pandeiro/boot-http            "0.6.3"          :scope "test"]
                 [crisptrutski/boot-cljs-test   "0.1.0-SNAPSHOT" :scope "test"]
                 [org.martinklepsch/boot-garden "1.2.5-7"        :scope "test"]

                 [org.clojure/clojurescript "1.7.122"]
                 ;; TODO rum
                 [reagent                   "0.5.0"]
                 ;; TODO devcards
                 ])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http    :refer [serve]]
 '[crisptrutski.boot-cljs-test :refer [test-cljs]]
 '[org.martinklepsch.boot-garden :refer [garden]])

(deftask build []
  (comp (speak)
        (cljs)
        (garden :styles-var 'megadex.styles/screen
                :output-to "css/garden.css")))

(deftask run []
  (comp (serve)
        (watch)
        (cljs-repl)
        (reload)
        (build)))

(deftask production []
  (task-options! cljs {:optimizations :advanced}
                      garden {:pretty-print false})
  identity)

(deftask development []
  (task-options! cljs {:optimizations :none
                       :unified-mode true
                       :source-map true}
                 reload {:on-jsload 'megadex.app/init})
  identity)

(deftask dev
  "Simple alias to run application in development mode"
  []
  (comp (development)
        (run)))

(deftask testing []
  (set-env! :source-paths #(conj % "test/cljs"))
  identity)

;;; This prevents a name collision WARNING between the test task and
;;; clojure.core/test, a function that nobody really uses or cares
;;; about.
(ns-unmap 'boot.user 'test)

(deftask test []
  (comp (testing)
        (test-cljs :js-env :phantom
                   :exit?  true)))

(deftask auto-test []
  (comp (testing)
        (watch)
        (test-cljs :js-env :phantom)))
