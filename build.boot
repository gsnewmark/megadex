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
 :dependencies '[[org.clojure/clojurescript "1.7.122"]
                 [org.omcljs/om             "1.0.0-alpha3"]
                 [kibu/pushy                "0.3.4"]
                 [com.domkm/silk            "0.1.1"]

                 [datascript                "0.13.1"]
                 [datascript-transit        "0.2.0"]

                 [devcards                      "0.2.0-3"        :scope "test"]
                 [adzerk/boot-cljs              "0.0-3308-0"     :scope "test"]
                 [adzerk/boot-cljs-repl         "0.2.0"          :scope "test"]
                 [adzerk/boot-reload            "0.3.2"          :scope "test"]
                 [pandeiro/boot-http            "0.6.3"          :scope "test"]
                 [crisptrutski/boot-cljs-test   "0.1.0-SNAPSHOT" :scope "test"]
                 [org.martinklepsch/boot-garden "1.2.5-7"        :scope "test"]])

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

(deftask cljs-development []
  (task-options! cljs {:optimizations :none
                       :unified-mode true
                       :source-map true})
  identity)

(deftask development []
  (task-options! reload {:on-jsload 'megadex.app/init!})
  identity)

(deftask devcards []
  (set-env! :source-paths #(conj % "devcards/src")
            :resource-paths #{"devcards/resources"})
  (task-options! cljs {:devcards true})
  identity)

(deftask testing []
  (set-env! :source-paths #(conj % "test/cljs"))
  identity)

;;; TODO find way to simultaneously compile both development & devcards
(deftask dev
  [c cards bool "Enable devcards."]
  (comp (cljs-development)
        (if cards
          (comp (testing) (devcards))
          (development))
        (run)))

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
