(require '[criterium.core :as c])

(def f (lazy-cat [0 1] (map + f (rest f))))
(take 20 f)
;; 6 micro
(c/quick-bench (nth (let [f (lazy-cat [0 1] (map + f (rest f)))]
                      f) 50))

(defn sum-last-2 
   ([] (sum-last-2 1 2)) 
   ([n m] (cons n (lazy-seq (sum-last-2 m (+ n m))))))
;; 2.7 micro
(c/quick-bench (nth (sum-last-2) 50))


(take 20 (let [v (volatile! 0)]
           (cons 0 (iterate (fn [x] (let [ret (+ @v x)]
                                      (vreset! v x)
                                      ret)) 1))))
;; 3.6 micro
(c/quick-bench (nth (let [v (volatile! 1)]
                      (iterate (fn [x] (let [ret (+ @v x)]
                                         (vreset! v x)
                                         ret)) 1)) 50))

;; small diff between these two, big time diff (extra boxing/unboxing)
;; 18 micro
(c/quick-bench (nth (map first (iterate (fn [[a b]] [b (+ a b)]) [0 1])) 50))
;; 4.6 mico
(c/quick-bench (first (nth (iterate (fn [[a b]] [b (+ a b)]) [0 1]) 50)))
