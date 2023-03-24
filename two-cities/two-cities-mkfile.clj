

(require '[clojure.string :as s])
(import '[java.io BufferedReader StringReader])

(->> (clojure.java.io/reader "tale-of-two-cities.txt")
     line-seq
     (drop 112)
     (map s/lower-case)
     (map #(re-seq #"[a-z]+(?:[-'][a-z]+)?" %))
     (mapcat identity)
     (partition 2 1)
     (reduce (fn [d [a b]] (update d a conj b)) {})
     (take 10))


(def f (java.io.RandomAccessFile. "tale-of-two-cities.txt" "r"))

(def t (slurp "tale-of-two-cities.txt"))

(defn skip-v-ntimes [s value n offset]
  (loop [begin offset remaining n]
    (if (zero? remaining)
      begin
      (if-let [i (s/index-of s value begin)]
       (recur (inc i) (dec remaining))))))

(let [end (count t)
      begin (skip-v-ntimes t "\n" 12 0)
      step (int (/ (- end begin) 4))]
  (partition 2 1 (range begin (inc end) step)))

(->> (range 15)
     (iterate next)
     (map (partial take 3))
     (take-while #(= 3 (count %))))

(let [window-size 3]
 (->> (range 10)
      (iterate next)
      (map (partial take window-size))
      (take-while some?)
      (drop-last (dec window-size))))
