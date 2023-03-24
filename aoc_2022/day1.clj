;; part 1
(->> (slurp "day1_input.txt")
           (#(str/split % #"\r\n\r\n"))
           (map #(str/split % #"\r\n"))
           (map (partial map #(Integer/parseInt %)))
           (map (partial apply +))
           (reduce max))

;; part 2
(->> (slurp "day1_input.txt")
           (#(str/split % #"\r\n\r\n"))
           (map #(str/split % #"\r\n"))
           (map (partial map #(Integer/parseInt %)))
           (map (partial apply +))
           (sort #(compare %2 %1))
           (take 3)
           (reduce +))
