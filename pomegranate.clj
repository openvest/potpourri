(comment
  ;; first include pomegranate...
  ;; in project.clj:
 [clj-commons/pomegranate "1.2.23"]
  ;; in deps.edn:
 clj-commons/pomegranate {:mvn/version "1.2.23"}
 )

(require '[cemerick.pomegranate :as pom]
         '[cemerick.pomegranate.aether :as aether])

(pom/add-dependencies :coordinates '[[org.clojure/data.zip "1.0.0"]
                                     [org.clojure/data.xml "0.2.0-alpha8"]]
                      :repositories (merge aether/maven-central
                                           {"clojars" "https://clojars.org/repo"}))


(require '[clojure.zip :as z]
         ;; these require extra deps
         '[clojure.data.xml :refer :all :as x]         
         '[clojure.data.zip.xml :refer [xml-> xml1->] :as zx])

