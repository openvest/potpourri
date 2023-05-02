(defn alternating-inc [inc-coll]
         "returns a stateful function that will increases and input by the next amout in inc-coll
          can be used in iterate e.g. (iterate (alternating-inc [1 10]) 0) => '(0 1 11 12 22 23 ....) "
         (let [v (volatile! (cycle inc-coll))]
           (fn [x]
             (let [step-size (first @v)]
               (vswap! v rest)
               (+ x step-size)))))

;; much simpler, appx same speed
(take 100 (reductions + 7 (cycle [4 2 4 2 4 6 2 6])))


(take 100 (iterate (alternating-inc [2]) 3)) ;;easier to inc by 2   ;; possible primes > 3 skipping multiples of 2 and 3
(take 100 (iterate (alternating-inc [2 4]) 5))  ;;faster alternating inc for this  ;; possible primes > 3 skipping multiples of 2 and 3
(take 100 (iterate (alternating-inc [4 2 4 2 4 6 2 6]) 7))  ;; possible primes > 5 skipping multiples of 2, 3 and 5

;; what it would take to eliminate all multiples of 7 during iteration
(take 100 (reductions + 11
                      [2 4 2 4 6 2 6 4 2 4 6 6 2 6 4 2 6 4 6 8 4 2 4 2 4
                       8 6 4 6 2 4 6 2 6 6 4 2 4 6 2 6 4 2 4 2 10 2 10]))

