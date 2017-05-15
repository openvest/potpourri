(ns potpourri.palindrome
  (:require [clojure.string :refer [last-index-of]]))

(defn pal
  [s]
  (let [len (count s)]
    (if (< len 2)
      len
      (let [c (last-index-of s (first s))]
        (if (pos? c)
          (max
           (+ 2 (pal (subs s 1 c)))
           (pal (subs s 1)))
          (pal (subs s 1)))))))

;;(pal "ababa")


