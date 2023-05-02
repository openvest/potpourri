(defmacro def-let [bindings & body]
  (assert (= 0 (mod (count bindings) 2)) "Must have an even number of bindings")
  `(do ~@(->> bindings
           (partition 2)
           (map (fn [[s v]] (list 'def s v))))
       ~@body))
