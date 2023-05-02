(add-libs ' { com.rpl/specter {:mvn/version "1.1.4"}})

(ns s
  (:require [com.rpl.specter :as S :refer :all]))



;; https://stackoverflow.com/questions/45764946/how-to-find-indexes-in-deeply-nested-data-structurevectors-and-lists-in-clojur
;; https://clojurians-log.clojureverse.org/specter/2017-08-20.html
(defn find-index-route [v data]
  (let [p (recursive-path [] p
                          [(if-path map? ALL INDEXED-VALS)
                           (if-path [LAST (pred= v)]
                             FIRST
                             [(collect-one FIRST) LAST coll? p]
                             )])]
    (let [ret (select-first p data)]
      (if (or (nil? ret) (vector? ret)) ret [ret]))))

(find-index-route :a [{:x 3} {:x 4 :y :a}])  ;; => [1 :y]

