(def dice-re  #"(\d+)(?:d(\d+)(?:([kdx])(\d+))?)?")
(defn roll
  [roll-str]
  (if-let [[_ n s kdx a] (re-matches dice-re roll-str)]
    (let [n (if (zero? (count n)) 1 (read-string n))
          sides (read-string s)
          a (if a (read-string a))
          rolls (repeatedly #(inc (rand-int sides)))]
      (reduce +
              (cond (nil? s) [n]
                    (nil? kdx)  (take n rolls)
                    (= kdx "d") (drop a (sort (take n rolls)))
                    (= kdx "k") (take a (sort-by - (take n  rolls)))
                    (= kdx "x") (take n (filter #(< % a) rolls))))))) ;; maybe guard against a=1

(defn roller
  [roll-str]
  (if-let [[_ n s kdx a] (re-matches dice-re roll-str)]
    (let [n (read-string n)
          sides (if (zero? (count s)) 6 (read-string s))
          a (if a (read-string a))
          roll #(inc (rand-int sides))
          explode (fn [rf]
                    (fn 
                      ([] (rf))
                      ([result] (rf result))
                      ([result val] (if (< val a)
                                      (rf result val)
                                      (-> (rf result val)
                                          (recur (roll)))))))
          rolls (repeatedly n roll)]
      (;reduce +       ;; for prod
       reduce conj [] ;; for dev
              (cond (nil? s) [n]
                    (nil? kdx)  (take n rolls)
                    (= kdx "d") (do (assert (<= 0 a n) "Bad arg to drop")
                                    (drop a (sort rolls)))
                    (= kdx "k") (do (assert (<= 0 a n) "Bad arg to keep")
                                    (take a (sort-by - rolls)))
                    (= kdx "x") (do (assert (<= 2 a sides) "Bad arg to explode")
                                    (sequence explode rolls))
                    :else (throw (Exception. "impossible to be here")))))
    (assert false (str "parsing error: " roll-str " <-> " dice-re))))

(map roller ["2d6" "3d6d1" "4d4k2" "3d6x5"])
;; first way to get explosive rolls with loop recur
(let [n 3
      sides 6
      limit 5
      roll #(inc (rand-int sides))]
  (loop [acc []
         i 0
         r (roll)]
    (cond (>= i n) acc
          (>= r limit) (recur (conj acc r) i (roll))
          :else (recur (conj acc r) (inc i) (roll)))))
;; second way to get explosive rolls with recusive call and lazy-seq
(let [n 3
      sides 6
      limit 5
      roll #(inc (rand-int sides))
      foo (fn foo [i]
            (let [r (roll)]
             (cond (zero? i) nil
                   (>= r limit) (cons r (lazy-seq (foo i)))
                   :else (cons r (lazy-seq (foo (dec i)))))))]
  (foo n))
;; third way to get explosive rolls with recusive call and lazy-seq
(let [n 3
      sides 6
      limit 5
      roll #(inc (rand-int sides))
      foo (fn foo [i]
            (lazy-seq
             (let [r (roll)]
               (cond (zero? i) nil
                     (>= r limit) (cons r (foo i))
                     :else (cons r (foo (dec i)))))))]
  (foo n))
;; fourth way to get explosive rolls. this uses a transducer
(defn explode [limit roll]
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result val] (let [new-result (rf result val)]
                      (if (< val limit)
                        new-result
                        (recur new-result (roll))))))))
(let [n 4
      sides 6
      limit 5
      roll #(inc (rand-int sides))]
  (transduce  (explode limit roll) conj (repeatedly n roll)))

(def operator-re #" *([-+]) *")
;; something to maybe do the expression adding up
(let [roll-pat (re-pattern (str "^ *" dice-re))
      op-roll-pat (re-pattern (str operator-re dice-re))
      [all num sides typ arg] (re-find roll-pat a)]
  (println  "start with: " a  all num sides typ arg)
  (loop [a (subs a (count all))]
    (println (str "continue with: " a))
    (if-let  [[op aa bb cc dd] (re-find op-roll-pat a)]
      (do (println "now do " op aa bb cc)
          (recur (subs a (count op)))))))

