Need to do more to get logging working.
Look into:
http://www.bahmanm.com/blogs/how-to-add-logging-to-a-clojure-project


(require '[cemerick.pomegranate :as pom]
         '[cemerick.pomegranate.aether :as aether])

(pom/add-dependencies :coordinates '[[scicloj/tablecloth "7.000-beta-27"]]
                      :repositories (merge aether/maven-central
                                           {"clojars" "https://clojars.org/repo"}))

(require '[nextjournal.clerk :as clerk])
(clerk/serve! {:watch-paths ["notebooks" "src"] :show-filter-fn #(clojure.string/starts-with? % "notebooks") :port 7777 :host "0.0.0.0"})


or  can we

clj -M:nextjournal/clerk nextjournal.clerk/serve! --watch-paths notebooks --port 7777 --browse