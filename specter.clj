(add-libs ' { com.rpl/specter {:mvn/version "1.1.4"}})

(ns s
  (:require [com.rpl.specter :as S :refer :all]))


;; From:
;;   * [StackOverflow](https://stackoverflow.com/questions/45764946/how-to-find-indexes-in-deeply-nested-data-structurevectors-and-lists-in-clojure)
;;   * [clojureverse](https://clojurians-log.clojureverse.org/specter/2017-08-20.html)
(defn find-index-route [v data]
  (let [p (recursive-path [] RECUR
                          [(if-path map? ALL INDEXED-VALS)
                           (if-path [LAST (pred= v)]
                             FIRST
                             [(collect-one FIRST) LAST coll? RECUR])])]

    (let [ret (select-first p data)]
      (if (or (nil? ret) (vector? ret)) ret [ret]))))

(find-index-route :a [{:x 3} {:x 4 :y :a}])  ;; => [1 :y]


;; From [StackOverflow question](https://stackoverflow.com/questions/76200172/paths-from-root-to-leaf-of-a-binary-tree-in-clojure)
(def input
  {:value 10,
   :left {:value 8, :left {:value 3}, :right {:value 5}},
   :right {:value 2, :left {:value 1}}})


(def leaf-paths
  (recursive-path [] RECUR
      (if-path #(some % [:left :right])
        [(collect-one :value) (submap [:left :right]) MAP-VALS RECUR]
        ;; also works:
        ;; [(collect-one :value) (multi-path :left :right) some? RECUR]
        :value)))

(select leaf-paths input)
; => [[10 8 3] [10 8 5] [10 2 1]]

(def leaf-values
  (recursive-path [] RECUR
      (if-path #(some % [:left :right])
        [(submap [:left :right]) MAP-VALS RECUR]
        :value)))

(select leaf-values input)
;; => [3 5 1]

(transform leaf-values #(* 100 %) input)
;; => {:value 10,
;;     :left {:value 8, :left {:value 300}, :right {:value 500}},
;;     :right {:value 2, :left {:value 100}}}


