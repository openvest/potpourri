(ns philosophers)

;; (def atomic-food (atom 1000))
;; (def food (ref 1000))

(defn make-philosophers
  [n]
  (let [names (map #(keyword (str "thinker" %))
                   (range 1 (inc n)))
        forks (for [_ names] (ref nil))]
    (map #(agent {:name %1
                  :forks [%2 %3]
                  :ate 0})
         names forks (concat (drop 1 forks)
                             [(first forks)]))))

(defn think [phil food]
  (dosync
   (let [forks (:forks phil)]
     (println "thinking " (:name phil))(flush)
     (doall (map #(ref-set % nil) forks))
     (send-off *agent* eat food)
     phil)))

(defn eat [phil food]
  (dosync
   (if (<= @food 0)
     phil
     (let [forks (:forks phil)]
       (if (not-any? identity (map deref forks))
         (do (doall (map #(ref-set % (:name phil)) forks))
             (println "eating " (:name  phil)) (flush)
             (alter food dec)
             (send-off *agent* think food)
             (update phil :ate inc))
         (do (println "no forks for " (:name  phil))(flush)
             (send-off *agent* think food)
             phil))))))


(defn report [philosophers food]
  (println "philosophers:")
  (doseq [phil philosophers]
    (println (:name @phil) \tab
             (:ate @phil) \tab
             (map deref (:forks @phil))))
  (println "food:" @food))


(defn dine [num-philosophers amt-food]
  (let [philosophers (make-philosophers num-philosophers)
        food (ref amt-food)]
    (doseq [phil philosophers] (send-off phil eat food))
    (. java.lang.Thread sleep 500)
    (report philosophers food)))

(dine 4 10)
