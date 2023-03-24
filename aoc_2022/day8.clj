(require '[clojure.string :as str])

(def day8-input
 "30373
25512
65332
33549
35390")

(def day8-input (slurp "day8_input.txt"))

(as-> day8-input a
       (str/split a #"\n")
       (map (partial map (comp #(- % 48) int)) a)
       (to-array-2d a)
       (aget a 0 2) ;; => 8
       #_(pprint a))

(def a 
  (as-> day8-input a
    (str/split a #"\n")
    (map (partial map (comp #(- % 48) int)) a)
    (to-array-2d a)))


(let [a (as-> day8-input a
          (str/split a #"\n")
          (map (partial map (comp #(- % 48) int)) a)
          (to-array-2d a))
      max-i (count a) 
      max-j (count (first a))]
  (->> (for [i (range max-i)
             j (range max-j)
             :let [height (aget a i j)]]
         (cond
           ;; at the edges
           (or (= i 0) (= j 0) (= i (dec  max-i)) (= j (dec max-j))) :edge
           ;; visible from left
           (> height (apply max (for [j' (range j)] (aget a i j')))) :left
           ;; visible from right
           (> height (apply max (for [j' (range (inc j) max-j)] (aget a i j')))) :right
           ;; visible from top
           (> height (apply max (for [i' (range i)] (aget a i' j)))) :top
           ;; visible from bottom
           (> height (apply max (for [i' (range (inc i) max-i)] (aget a i' j)))) :bottom
           :else false))
       (filter identity)
       (count)))

(defn look
  "look in one direction either:
  :up :down :left or :right"
  [a i j max-i max-j direction]
  (let [height (aget a i j)
        [iter-i iter-j] (direction {:up    [dec identity]
                                    :down  [inc identity]
                                    :left  [identity dec]
                                    :right [identity inc]})]
    (loop [other-i (iter-i i)
           other-j (iter-j j)
           viewed 0]
      (if (or (< other-i 0) (< other-j 0) (>= other-i max-i) (>= other-j max-j))
        ;; over the edge so stop here
        viewed
        (let [other-height (aget a other-i other-j)]
          (if (>= other-height height)
            ;; this is the last one visible
            (inc viewed)
            ;; keep looking
            (recur (iter-i other-i) (iter-j other-j) (inc viewed))))))))

(defn look-around [a i j max-i max-j]
  (->> (for [direction [:up :left :down :right]]
         (look a i j max-i max-j direction))
       (filter pos?)
       (reduce *)))

(let [a (as-> day8-input a
              (str/split a #"\n")
              (mapv (partial mapv (comp #(- % 48) int)) a)
              (to-array-2d a))
      max-i (count a)
      max-j (count (first a))]
  (->> (for [i (range max-i)
             j (range max-j)]
         (look-around a i j max-i max-j))
       (reduce max)))
