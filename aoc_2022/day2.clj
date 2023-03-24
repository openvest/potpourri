(def outcomes-1 {"A X" (+ 3 1) "A Y" (+ 6 2) "A Z" (+ 0 3)
                 "B X" (+ 0 1) "B Y" (+ 3 2) "B Z" (+ 6 3)
                 "C X" (+ 6 1) "C Y" (+ 0 2) "C Z" (+ 3 3)})

(->> (slurp "day2_input.txt")
           (#(str/split % #"\n"))
           (map outcomes-1)
           (reduce +))

(for [e [1 2 3] m [1 2 3]]
        [[e m]
         (case (- m e)
           -2 (+ 6 m)
           -1 (+ 0 m)
            0 (+ 3 m)
            1 (+ 6 m)
            2 (+ 0 m))]
        )



(for [e "ABC"  m "XYZ"] [(str e " " m) (play-val m)])

(def outcomes-2
  (let [play-val {\A 1 \B 2 \C 3 \X 1 \Y 2 \Z 3}]
    (->> (for [e "ABC"  m "XYZ"]
           [(str e " " m)
            (-> (- (play-val m)(play-val e))
                inc
                (mod 3)
                (* 3)
                (+ (play-val m)))])
        (into {}))))

(defn what-to-play [e wld] 
        (case wld
          ;; draw
          \X ({\A \Z \B \X \C \Y} e)
          \Y ({\A \X \B \Y \C \Z} e)
          \Z ({\A \Y \B \Z \C \X} e)))

(defn play-2 
  ([abc-xyz]
   (play-2 (first abc-xyz) (nth abc-xyz 2)))
  ([abc xyz]
   (let [wld (str/index-of "XYZ" xyz)
         play (nth (cycle "XYZ") (mod (+ (str/index-of "ABC" abc) (dec wld)) 3))]
     (str abc " " play))))

(->> (slurp "day2_input.txt")
           (#(str/split % #"\n"))
           (map play-2)
           (map outcomes-2)
           (reduce +))

(defn play-22 
  ([abc-xyz]
   (play-22 (first abc-xyz) (last abc-xyz)))
  ([abc xyz]
   (let [ldw (str/index-of "XYZ" xyz) ;; lose, draw or win "XYZ" -> [0 1 2]
         play (-> (str/index-of "ABC" abc)
                  (+ (dec ldw))
                  (mod 3))]
     (+ (inc play) (* 3 ldw)))))

(->> (slurp "day2_input.txt")
           (#(str/split % #"\n"))
           (map play-22)
           (reduce +))
