(ns potpourri.palindrome
  "length of palindrom created by removing smallest number of letters
 to form a palindrome")

;; (set! *warn-on-reflection* true)
;; somewhat suprising that this is the fastest among the candidates below
;; update: pal-len2 is faster now (mapv to keep using vectors was added)
;; 1766ms
(defn pal-len1 [abc]
  (let [diag  (map #(hash-map 
                     :beg %
                     :end (inc %)
                     :cost 0)
                   (range (count abc)))
        move-up (fn [[l r]]
                  (-> (update l :end inc)
                      (assoc :l l)
                      (assoc :r r)
                      (assoc :cost (if (= (nth abc (:beg l))
                                          (nth abc (:end l)))
                                     (get-in l [:r :cost] 0)
                                     (inc (min (:cost l) (:cost r)))))))]
    (loop [diag diag]
      ;; (println (map #(vector (subs abc (:beg %) (:end %)) (:cost %)) diag))
      (if (= 1 (count diag))
        (- (count abc) (:cost (first diag)))
        (recur (map move-up (partition 2 1 diag)))))))

;; 1109ms
(defn pal-len3 [abc]
  (let [diag  (map #(hash-map 
                     :beg %
                     :end (inc %)
                     :cost 0)
                   (range (count abc)))
        move-up (fn [[l r]]
                  ;; this creates a hashmap rather than updating l.
                  ;; so why slower than pal-len1??
                  (hash-map :beg (:beg l)
                            :end (inc (:end l))
                            :l l
                            :r r
                            :cost (if (= (nth abc (:beg l))
                                         (nth abc (:end l)))
                                      ;; (log/spy :info (get-in l [:l :r :cost] 0))
                                      (get-in l [:r :cost] 0)
                                      (inc (min (:cost l) (:cost r))))))]
    (loop [diag diag]
      ;; (println (map #(vector (subs abc (:beg %) (:end %)) (:cost %)) diag))
      (if (= 1 (count diag))
        (- (count abc) (:cost (first diag)))
        (recur (map move-up (partition 2 1 diag)))))))

;; with vectors
;; fast 97ms (1/2 that with .charAt)
(defn pal-len2
  [abc]
  (let [len (count abc)]
    (loop [sub-len    (int 2)
           prev-costs (vec (repeat len 0))
           costs      (vec (repeat len 0))]
      (if (< len sub-len)
          (- len (first costs))
          (recur (inc sub-len)
                 costs
                 (doall  ;; needed to avoid StackOverflow
                  (mapv #(if (= (nth abc %)  ;; .charAt would speedup (with ^String)
                                (nth abc (+ % sub-len -1)))
                           (nth prev-costs (inc %))
                           (inc (min (nth costs %) (nth costs (inc %)))))
                        (range (- len sub-len -1)))))))))
(defn pal-len-vec-trans
  [^String abc]
  (let [len (count abc)]
    (loop [sub-len    (int 2)
           prev-costs (transient (vec (repeat len 0)))
           costs      (transient (vec (repeat len 0)))]
      (if (< len sub-len)
          (- len (costs 0))
          (do
            (loop [i 0 j (dec sub-len)]
              (when (< j len)
               (if (= (.charAt abc i) ;; .charAt speeds (with ^String)
                      (.charAt abc j))
                 (assoc! prev-costs i (prev-costs (unchecked-inc i)))
                 (assoc! prev-costs i (inc (min (costs i) (costs (unchecked-inc i))))))
               (recur (unchecked-inc i) (unchecked-inc j))))
            (recur (unchecked-inc sub-len) costs prev-costs))))))

(defn pal-len-vec
  [^String abc]
  (let [len (count abc)]
    (loop [sub-len    (int 2)
           prev-costs (vec (repeat len 0))
           costs      (vec (repeat len 0))]
      (if (< len sub-len)
          (- len (costs 0))
          (recur (unchecked-inc sub-len)
                 costs
                 (doall (mapv #(if (= (.charAt abc %) ;; .charAt speeds (with ^String)
                                      (.charAt abc (dec (+ % sub-len))))
                                 (prev-costs (unchecked-inc %))
                                 (inc (min (costs %) (costs (unchecked-inc %)))))
                              (range (unchecked-inc (- len sub-len))))))))))

;; with int-array
;; second fastest 14ms (after clearing reflection warnings)
(defn pal-len
  [^String abc]
  (let [len (count abc)]
    (loop [sub-len    2
           prev-costs (int-array (inc len) 0)
           costs      (int-array (inc len) 0)]
      (if (< len sub-len)
          (- len (first costs))
          (do
            (doseq [i (range (inc (- len sub-len)))]
              ;; why is aset faster than aset-int
              (aset prev-costs i                 ;; "int" here clears a reflection warning
                    (int (if (= (.charAt abc i)  ;; gives a significant (50X) speedup (aset x y (int z) is faster than (aset-int x y z) or (aset-int x y (int z))
                                (.charAt abc (dec (+ i sub-len))))  ;; .charAt is also faster than nth
                           (aget prev-costs (unchecked-inc i))
                           (unchecked-inc (min (aget costs i)
                                               (aget costs (unchecked-inc i))))))))
            (recur (unchecked-inc sub-len) costs prev-costs)))))) 

;; fastest 5ms same as above with loop rather than seq
(defn pal-lenx
  [^String abc]
  (let [len (count abc)]
    (loop [sub-len    2
           prev-costs (int-array (inc len) 0)
           costs      (int-array (inc len) 0)]
      (if (> sub-len len)
          (- len (first costs)) ;; termination here
          (do
            (loop [i 0 j (dec sub-len)]
              (when (< j len)
                  (aset prev-costs i ;; "int" here clears a reflection warning
                        (int (if (= (.charAt abc i) ;; gives a significant (50X) speedup (aset x y (int z) is faster than (aset-int x y z) or (aset-int x y (int z))
                                    (.charAt abc j)) ;; .charAt is also faster than nth
                               (aget prev-costs (unchecked-inc i))
                               (unchecked-inc (min (aget costs i)
                                                   (aget costs (unchecked-inc i)))))))
                  (recur (unchecked-inc i) (unchecked-inc j))))
            (recur (unchecked-inc sub-len) costs prev-costs))))))

;; all seq's
;; faster than pal-len1 at higher lenths 1310ms
(defn pal-len-seq
  [abc]
  (loop [bc (drop 1 abc)
         prev-prev  (repeat 0)
         prev-costs (repeat 0)]
    (if-not (seq bc)
      (- (count abc) (first prev-costs))
      (recur (drop 1 bc)
             (drop 1 prev-costs)
             (doall  ;; doall required to avoid StackOverflow circa len=1000
              (map (fn [a b pp pc1 pc2]
                     (if (= a b)
                       pp
                       (inc (min pc1 pc2))))
                   abc bc
                   prev-prev
                   prev-costs
                   (drop 1 prev-costs)))))))

(comment 
(defn rand-abc
  "create a random string of lower case letters a-z"
  [len]
  (apply str (repeatedly len #(char (+ (int \a) (rand-int 26))))))

(set! *warn-on-reflection* true)
;; note that with warn-on-reflection that this generates a warning
;; this is disappointing as the if statement will AWAYS return an int
;; if you wrap it in (int ) the warning is gone and the performance is boosted
(let [a (int-array [4 5 6 7])]
  (aset a 1  (if (seq a) 40 (aget a 0))))
)
