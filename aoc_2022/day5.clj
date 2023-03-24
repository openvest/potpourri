(def day5-input
"    [D]    
[N] [C]    
[Z] [M] [P]
 1   2   3 

move 1 from 2 to 1
move 3 from 1 to 3
move 2 from 2 to 1
move 1 from 1 to 2")

(def day5-input (slurp "day5_input.txt"))

(def initial-stacks
  (->> (str/split day5-input #"\n")
       (take-while (partial re-find #"\[") )
       reverse
       (map #(map second (re-seq #"[\[ ]([A-Z ])[\] ] ?" %)))
       (apply map list)
       (map (comp #(drop-while (partial re-matches #" *") % ) reverse))
       (map-indexed (fn [i stack] [(inc i) stack]))                                 
       (into (sorted-map))))

(def moves
  (->> (re-seq #"move ([\d]+) from ([\d]+) to ([\d]+)" day5-input)
       (map rest)
       (map (fn [move](map #(Integer/parseInt %) move)))))

;; => {1 ("N" "Z"), 2 ("D" "C" "M"), 3 ("P")}
(defn move [stacks [mv-count mv-src mv-dest]]
 (let [[move remainder] (split-at mv-count (stacks mv-src))]
   (assert (<=  mv-count (count (stacks mv-src))))
      (-> stacks
          (assoc mv-src remainder)
          (update mv-dest into move))))


(->> (reduce move initial-stacks moves)
     (vals)
     (map #(or (first %) " "))
     (apply str))

;; part 2
(defn move-9001 [stacks [mv-count mv-src mv-dest]]
 (let [[move remainder] (split-at mv-count (stacks mv-src))]
   (assert (<=  mv-count (count (stacks mv-src))))
      (-> stacks
          (assoc mv-src remainder)
          (update mv-dest (partial concat move)))))



(->> (reduce move-9001 initial-stacks moves)
     (vals)
     (map #(or (first %) " "))
     (apply str))
;; => "RWLWGJGFD"
