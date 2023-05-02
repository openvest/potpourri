"http://matthewrocklin.com/blog/work/2014/01/13/Text-Benchmarks"

(require '[clojure.string :as s])
(import '[java.io BufferedReader StringReader])

(let [f (line-seq (BufferedReader. (StringReader. "hi,there\nhi,sam\nby,there\nhi,there")))]
  (->> f
       (map #(s/split % #","))
       (reduce (fn [d [a b]] (update d a conj b)) {})))

(let [f (line-seq (BufferedReader. (StringReader. "hi,there\nhi,sam\nby,there\nhi,there\nby,yall")))]
  (transduce (map #(s/split % #","))
             (fn ([] {})
                 ([d] d)
                 ([d [a b]] (update d a conj b)))
             f))

(let [f (line-seq (BufferedReader. (StringReader. "hi,there\nhi,sam\nby,there\nhi,there\nby,yall")))]
  (transduce (map #(s/split % #","))
             (fn ([] (transient {}))
                 ([d] (persistent! d))
                 ([d [a b]] (assoc! d a (conj (get d a []) b))))
             f))

(time (let [f (line-seq (clojure.java.io/reader "../potpourri/word-pairs.txt"))]
   (->> f
        (map #(s/split % #","))
        (reduce (fn [d [a b]] (update d a conj b)) {})
        count)))

(time (let [f (line-seq (clojure.java.io/reader "../potpourri/word-pairs.txt"))]
   (->> f
        (map #(s/split % #","))
        (group-by first)
        (count))))

(time (let [f (line-seq (clojure.java.io/reader "../potpourri/word-pairs.txt"))]
   (count (transduce (map #(s/split % #","))
               (fn ([] {})
                 ([d] d)
                 ([d [a b]] (update d a conj b)))
               f))))

(time (let [f (line-seq (clojure.java.io/reader "../potpourri/word-pairs.txt"))]
   (count (transduce (map #(s/split % #","))
               (fn ([] (transient {}))
                 ([d] (persistent! d))
                 ([d [a b]] (assoc! d a (conj (get d a []) b))))
               f))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(time (let [filename "../potpourri/word-pairs.txt"
            lines (s/split (slurp filename) #"\n")
            pairs (map #(s/split % #",") lines)
            result (group-by first pairs)]
nil))


(time (let [f (line-seq (clojure.java.io/reader "../potpourri/word-pairs.txt"))]
   (->> f
        count)))


(require '[criterium.core :refer [quick-bench]])
