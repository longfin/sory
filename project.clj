(defproject sory "0.1.0-SNAPSHOT"
  :description "Sound communication library"
  :license {:name "MIT"}
  :url "http://github.com/longfin/sory"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.122"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :min-lein-version "2.0.0"
  :jvm-opts ["-server"]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.4.1"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src-cljs" "src-clj"]

              :figwheel { :on-jsload "sory.core/on-js-reload" }

              :compiler {:main sory.core
                         :asset-path "js/compiled/out"
                         :output-to "resources/public/js/compiled/sory.js"
                         :output-dir "resources/public/js/compiled/out"
                         :source-map-timestamp true }}
             {:id "min"
              :source-paths ["src-cljs" "src-clj"]
              :compiler {:output-to "resources/public/js/compiled/sory.js"
                         :main sory.core
                         :optimizations :advanced
                         :pretty-print false}}]}

  :figwheel {
             ;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this
             ;; doesn't work for you just run your own server :)
             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log" 
             })
