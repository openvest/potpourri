;; assumes that:
;;     - splits and raw data are both sorted date descending
;;     - structure is [date price]

(defn xform-with-splits
  "stateful transducer that takes splits and then applies split
  adjustments to a collection during a reduction"
  [splits]
  (if-not (seq splits)
    (fn no-splits [rf] rf)
    (fn reduce-with-splits [rf]
      (let [split-factor (volatile! 1)
            next-split-date (volatile! (or (ffirst splits) 0))
            next-split-amt (volatile! (or (second (first splits)) 1))
            splits (volatile! (next splits))]
        (fn adjust-for-split
          ([] (rf))
          ([acc] (rf acc))
          ([acc [d v]]
           (while (< d @next-split-date)
             (vswap! split-factor * @next-split-amt)
             (vreset! next-split-date (or (ffirst @splits) 0))
             (vreset! next-split-amt (or (second (first @splits)) 1))
             (vswap! splits next))
           (rf acc [d (* v @split-factor)])))))))

(comment
  "lets make the date and value functions generic
rather than (first xxx) for date use (:date xxx)"
  (def raw (for [i (range 10)] [(- 110 i) (+ 25 (* i 0.25))]))
  (def splits (reverse (sort {102 2, 105 3})))
  (sequence (xform-with-splits splits) raw))

