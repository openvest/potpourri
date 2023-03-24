(def day3-input 
"vJrwpWtwJgWrhcsFMMfFFhFp
jqHRNqRjqzjGDLGLrsFMfFZSrLrFZsSL
PmmdzqPrVvPwwTWBwg
wMqvLMZHhHMvwLHjbvcjnnSBnvTQFn
ttgJtRGJQctTZtZT
CrZsJsPPZsGzwwsLwLmpwMDw")

(defn priority [ch]
  (let [i (int ch)]
    (if (< i 97)
      (- i 38)
      (- i 96))))

(->> (slurp "day2_input.txt")
           (#(str/split % #"\n"))
           (map outcomes-1)
           (reduce +))


(->> (slurp "day3_input.txt")
     (#(str/split % #"\n"))
     (map (fn [x] (let [l (count x)] [(subs x 0 (/ l 2))
                                      (subs x (/ l 2) l)])))
     (map (partial map (partial into #{})))
     (map (partial apply some))
     (map priority)
     (reduce +))
;; => 7845

;; part 2
(->> (slurp "day3_input.txt")
     (#(str/split % #"\n"))
     (map (partial into #{}))
     (partition 3)
     (map (partial reduce clojure.set/intersection))
     (map first)
     (map priority)
     (reduce +))
