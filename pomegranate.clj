(require '[cemerick.pomegranate :as pom]
         '[cemerick.pomegranate.aether :as aether])

(pom/add-dependencies :coordinates '[[org.clojure/data.zip "1.0.0"]
                                     [org.clojure/data.xml "0.2.0-alpha8"]]
                      :repositories (merge aether/maven-central
                                           {"clojars" "https://clojars.org/repo"}))



(require '[clojure.data.xml :refer :all :as x]
         '[clojure.zip :as z])

(require '[clojure.zip :as z]
         ;; these require extra deps
         '[clojure.data.xml :refer :all :as x]         
         '[clojure.data.zip.xml :refer [xml-> xml1->] :as zx])

(defmacro def-let [bindings & body]
  (assert (= 0 (mod (count bindings) 2)) "Must have an even number of bindings")
  `(do ~@(->> bindings
           (partition 2)
           (map (fn [[s v]] (list 'def s v))))
       ~@body))
