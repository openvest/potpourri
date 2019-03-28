(defproject potpourri "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/data.zip "0.1.3"]
                 [org.clojure/tools.cli "0.4.2"]
                 [incanter "1.5.7"]
                 [redux "0.1.4"]
                 [cider/cider-nrepl "0.15.0-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]
                                  [criterium "0.4.4"]
                                  [ring/ring-spec "0.0.4"]]
                   ;; :plugins [[lein-gorilla "0.4.0"]]
                   }}

)
