(def queue  (ref (with-meta
                   clojure.lang.PersistentQueue/EMPTY
                   {:tally 0})))
(def seats  3)
 
(defn debug
  [msg n]
  (println msg (apply str (repeat (- 35 (count msg)) \space)) n)
  (flush))
 
(defn the-shop
  [a]
  (debug "(c) entering shop" a)
  (dosync
   (if (< (count @queue) seats)
     (alter queue conj a)
     (debug "(s) turning away customer" a))))
 
(defn the-barber
  [order q oval nval]
  ;; (println   order q oval nval)
  (Thread/sleep (+ 100 (rand-int 600)))
  (dosync
   (when (peek @q)
     (debug "(b) cutting hair of customer" (peek @q))
     (ref-set queue (with-meta (pop @q)
                      {:tally (inc (:tally (meta @q)))})))))
 
(add-watch queue :cut-me the-barber)
 
(doseq [customer (range 1 20)]

 (Thread/sleep (+ 50 (rand-int 20)))
  (send-off (agent customer) the-shop))
 
(Thread/sleep 2000)
(println "(!) " (:tally (meta @queue)) "customers got haircuts today")
