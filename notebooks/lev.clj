(defn levenshtein [w1 w2]
  (letfn [(cell-value [same-char? prev-row cur-row col-idx]
            (min (inc (nth prev-row col-idx))
                 (inc (last cur-row))
                 (+ (nth prev-row (dec col-idx)) (if same-char?
                                                   0
                                                   1))))]
    (loop [row-idx  1
           max-rows (inc (count w2))
           prev-row (range (inc (count w1)))]
      (if (= row-idx max-rows)
        (last prev-row)
        (let [ch2           (nth w2 (dec row-idx))
              next-prev-row (reduce (fn [cur-row i]
                                      (let [same-char? (= (nth w1 (dec i)) ch2)]
                                        (conj cur-row (cell-value same-char?
                                                                  prev-row
                                                                  cur-row
                                                                  i))))
                                    [row-idx] (range 1 (count prev-row)))]
          (recur (inc row-idx) max-rows next-prev-row))))))

(defn lev1 [w1 w2]
  (let [max-rows  (inc (count w2))
        cell-value (fn [same-char? prev-row cur-row col-idx]
                     (min (inc (nth prev-row col-idx))
                          (inc (last cur-row))
                          (+ (nth prev-row (dec col-idx))
                             (if same-char? 0 1))))]
    (loop [row-idx  1
           prev-row (range (inc (count w1)))]
      (if (= row-idx max-rows)
        (last prev-row)
        (let [ch2           (nth w2 (dec row-idx))
              next-prev-row (reduce (fn [cur-row i]
                                      (let [same-char? (= (nth w1 (dec i)) ch2)]
                                        (conj cur-row (cell-value same-char?
                                                                  prev-row
                                                                  cur-row
                                                                  i))))
                                    [row-idx] (range 1 (count prev-row)))]
          (recur (inc row-idx) next-prev-row))))))

(defn lev2 [w1 w2]
  (letfn [(step-row [prev-row [ch2 row-num]]
            (first (reduce
                    (fn [[cur-row cur-last] [ch1 [ri-1 ri]]]
                      (let [new-ri (min (inc ri)
                                        (inc cur-last)
                                        (if (= ch1 ch2) ri-1 (inc ri-1)))]
                        [(conj cur-row new-ri) new-ri]))
                    [[row-num] row-num]
                    (map vector w1 (partition 2 1 prev-row)) )))]
    (last (reduce step-row
                  (range (inc (count w1))) ;; inital row
                  (map vector w2 (iterate inc 1))))))

;; all the reduce conj stuff above in lev2 is avoided by shifting from reduce to reductions
(defn lev3 [w1 w2]
  (letfn [(step-row [prev-row [ch2 row-num]]
            (reductions
             (fn [cur-last [ch1 [ri-1 ri]]]
               (min (inc ri)
                    (inc cur-last)
                    (if (= ch1 ch2) ri-1 (inc ri-1))))
             row-num
             (map vector w1 (partition 2 1 prev-row))))]
    (last (reduce step-row
                  (range (inc (count w1)))
                  (map vector w2 (iterate inc 1))))))

;; recursive and fast because it's memoized
;; strange and awkward that the memoized function must be passed forward
;; as in (g g a b) and (s s as bs)
(defn lev-cgrand [a b]
  (let [f (fn [s a b]
            (if-let [[aa & as] (seq a)]
              (if-let [[bb & bs] (seq b)]
                (if (= aa bb)
                  (s s as bs)
                  (inc (min (s s as b) (s s as bs) (s s a bs))))
                (count a))
              (count b)))
         g (memoize f)]
    (g g a b)))

;; very cool very fast but gets `(lev-chouser "bar" "are")` wrong
(defn lev-chouser [& z]
  (apply (fn f [[a & x :as j] [b & y :as k]]
           (if j
             (if (= a b)
               (f x y)
               (+ 1
                  (if (= (count x) (count y))
                    (f x y)
                    (min (f x y) (f j y)))))
             (count k)))
         (sort-by count z)))

(defn levd [a b]
  (let [a1 (first a) b1 (first b)]
  (cond
    (nil? a1) (count b)
    (nil? b1) (count a)
    (= a1 b1) (levd (rest a) (rest b))
  :else
    (inc (min (levd (rest a) (rest b)) (levd a (rest b)) (levd (rest a) b))))))
