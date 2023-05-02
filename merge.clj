;; generate some symbols
(->> (fn []  (rand-nth "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
     (repeatedly 2)
     (apply str)
     (partial gensym)
     (repeatedly 3))

(repeatedly 10
            (fn []
              (->> (fn []  (rand-nth "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
                   (repeatedly 2)
                   (apply str)
                   (gensym))))
;; merge example
(def a [{:x 0 :z 100}
        {:x 1 :y 10 :z 101}
        {:x 1 :y 11 :z 102}
        {:x 2 :y 12 :z 103}
        {:x 2 :y 14 :z 104}])

(def b [{:x 1 :a 20 :b 205 :z 2000}
        {:x 2 :a 21 :b 206}
        {:x 2 :a 22 :b 207}
        {:x 2 :a 23 :b 208}
        {:x 3 :a 33}])

;; join the above data on :x
;; is in effect a FULL JOIN, duplicate columns last one (right one) wins
(->> [a b]
     (map  (partial group-by :x)) 
     (apply merge-with
            (fn [left right]
              (for [l left r right]
                (merge l r))))
     (vals)
     (apply concat))


(def a
  (repeatedly 200000
              (fn []
                (->> (fn []  (rand-nth "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
                     (repeatedly 2)
                     (apply str)
                     ((fn [x] (str x (rand-int 100))))
                     (assoc {:z (rand)  :y (rand-int 1000)} :x)))))
(def b
  (repeatedly 20000
              (fn []
                (->> (fn []  (rand-nth "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
                     (repeatedly 2)
                     (apply str)
                     ((fn [x] (str x (rand-int 100))))
                     (assoc {:z (rand)  :y (rand-int 1000)} :x)))))
