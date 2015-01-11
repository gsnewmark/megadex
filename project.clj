(defproject megadex "0.1.0-SNAPSHOT"
  :description "Web application with Persona 4 Golden personas description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.facebook/react "0.12.1"]
                 [reagent "0.5.0-alpha"]
                 [reagent-forms "0.3.9"]
                 [bidi "1.12.0"]
                 [datascript "0.7.2"]
                 [org.clojure/clojurescript "0.0-2511" :scope "provided"]
                 [com.cemerick/piggieback "0.1.3"]
                 [cljs-uuid "0.0.4"]
                 [weasel "0.4.2"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.2"]
                 [prone "0.8.0"]
                 [compojure "1.3.1"]
                 [selmer "0.7.7"]
                 [enlive "1.1.5"]
                 [environ "1.0.0"]
                 [leiningen "2.5.0"]
                 [figwheel "0.2.0-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-environ "1.0.0"]
            [lein-ring "0.8.13"]
            [lein-asset-minifier "0.2.0"]]

  :ring {:handler megadex.handler/app}

  :min-lein-version "2.5.0"

  :uberjar-name "megadex.jar"

  :aliases {"fixture/p4g" ["run" "-m" "megadex.tasks/generate-p4g-fixture"]}

  :minify-assets
  {:assets
    {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :externs       ["react/externs/react.js"]
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:repl-options {:init-ns megadex.handler
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :dependencies [[ring-mock "0.1.5"]
                                  [ring/ring-devel "1.3.2"]
                                  [pjstadig/humane-test-output "0.6.0"]]

                   :plugins [[lein-figwheel "0.2.0-SNAPSHOT"]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :css-dirs ["resources/public/css"]
                              :ring-handler megadex.handler/app}

                   :env {:dev? true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]
                                              :compiler {:source-map true}}}}}

             :uberjar {:hooks [leiningen.cljsbuild minify-assets.plugin/hooks]
                       :env {:production true}
                       :aot :all
                       :omit-source true
                       ;;TODO: figure out how to clean properly
                       ;:prep-tasks [["cljsbuild" "clean"]]
                       :cljsbuild {:jar true
                                   :builds {:app
                                             {:source-paths ["env/prod/cljs"]
                                              :compiler
                                              {:optimizations :advanced
                                               :pretty-print false}}}}}

             :production {:ring {:open-browser? false
                                 :stacktraces?  false
                                 :auto-reload?  false}}})
