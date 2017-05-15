(ns philosophers)

;(def atomic-food (atom 1000))
(def food (ref 1000))

(def philosophers
  (let [names [:plato :kant :hume]
        forks (for [_ names] (ref nil))]
    (map #(agent {:name %1
                  :forks [%2 %3]
                  :ate 0})
         names forks (concat (drop 1 forks)
                             [(first forks)]))))

(defn think [phil]
  (dosync
   (let [forks (:forks phil)]
     ;; (println "thinking " (:name phil))
     (doall (map #(ref-set % nil) forks))
     (send-off *agent* eat)
     phil)))

(defn eat [phil]
  (dosync
   (if (<= @food 0)
     phil
     (let [forks (:forks phil)]
       (if (not-any? identity (map deref forks))
         (do (doall (map #(ref-set % (:name phil)) forks))
             ;; (println "eating " (:name  phil))
             (alter food dec)
             (send-off *agent* think)
             (update phil :ate inc))
         (do ;(println "no forks for " (:name  phil))
             (send-off *agent* think)
             phil))))))


(defn report [philosophers]
  (println "philosophers:")
  (doseq [phil philosophers]
    (println (:name @phil) \tab
             (:ate @phil) \tab
             (map deref (:forks @phil))))
  (println "food:" @food))

(doseq [phil philosophers] (send-off phil eat))
(. java.lang.Thread sleep 500)
(report philosophers)

(comment 
  (send (nth philosophers 0) eat)
  (send (nth philosophers 0) think)
  (report philosophers)
  (send (nth philosophers 1) eat)
  (send (nth philosophers 1) think)
  (report philosophers)
  (send (nth philosophers 2) eat)
  (send (nth philosophers 2) think)
  (report philosophers))
