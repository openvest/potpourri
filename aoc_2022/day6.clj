
(def day6-input 
  "nznrnfrfntjfmvfwmzdfjlvtqnbhcprsg")

(def day6-input
  (slurp "day6_input.txt"))

(defn msg-offset [raw-msg cnt]
 (->> raw-msg
      (partition cnt 1 )
      (map-indexed (fn counts [i msg-blk] [(+ cnt i) (count (set msg-blk))]))
      (some (fn ok-len [[i l]] (when (= cnt l) i)))))

(msg-offset day6-input 14)

;; as list comprehension

(defn msg-offset2 [raw-msg cnt]
  (-> (for [i (range 14 (count raw-msg))
            :let [s (count (set (subs raw-msg (- i 14) i)))]
            :when (= s 14)]
        i)
      first))

