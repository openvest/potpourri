(defn max-pond [landscape]
  (let [puddles (for [[r row] (map-indexed vector landscape)
                      [c val] (map-indexed vector row)
                      :when (zero? val)]
                  [r c])
        newshore (fn [[row col]]
                   (drop 2 (for [r ((juxt identity dec) row)
                                 c ((juxt inc identity dec) col)]
                             [r c])))
        agg-ponds (fn agg-ponds
                    ([ponds puddle] (agg-ponds ponds (set [puddle]) (newshore puddle)))
                    ([[p1 & ps] p ns] (lazy-seq (cond (nil? p1) [p]
                                                      (some p1 ns) (agg-ponds ps (into p1 p) ns)
                                                      :else (cons p1 (agg-ponds ps p ns))))))]
   (->> puddles
        (reduce agg-ponds [])
        (map count)
        (apply max))))

;;(max-pond [[0 1 0 1]
;;           [1 0 1 1]
;;           [1 1 1 0]
;;           [0 1 1 1]])
;;-> 3
