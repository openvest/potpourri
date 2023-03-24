(def day4-input "2-4,6-8
2-3,4-5
5-7,7-9
2-8,3-7
6-6,4-6
2-6,4-8")


(defn f [[a1 a2 b1 b2]] 
        (let [l1 (- a2 a1)
              l2 (- b2 b1)]
          (if (<= l1 l2)
            (and (>= a1 b1) (<= a2 b2))
            (and (<= a1 b1) (>= a2 b2)))))

(->> (slurp "day4_input.txt")
     (#(str/split % #"\n"))
     (map (partial re-matches #"(\d+)-(\d+),(\d+)-(\d+)"))
     (map rest)
     (map (partial map #(Integer/parseInt %)))
     (filter f)
     count)


(defn f2 [[a1 a2 b1 b2]]
  (and (>= a2 b1) (<= a1 b2)))

(->> (slurp "day4_input.txt"); day4-input
     (#(str/split % #"\n"))
     (map (partial re-matches #"(\d+)-(\d+),(\d+)-(\d+)"))
     (map rest)
     (map (partial map #(Integer/parseInt %)))
     (filter f2)
     count)
