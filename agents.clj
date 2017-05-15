(ns agents-queue)
;;https://lethain.com/a-couple-of-clojure-agent-examples/

(def logger (agent []))

(defn log [msg]
  (send logger conj msg))

(defn create-relay [coll]
  (reduce (comp agent vector) (agent nil) (reverse coll)))

(defn relay-msg [next-agent prev-msg]
  (if (nil? next-agent)
    (log (str "finished:" prev-msg))
    (let [new-msg (str prev-msg (second next-agent))]
      ;; do something interesting with new-msg then:
      (log new-msg)
      ;; go do the next thing
      (send (first next-agent) relay-msg new-msg))))

(send (create-relay [:alice :bob :charlie]) relay-msg "hello")
(. java.lang.Thread sleep 500)
(doseq [line @logger] (println line))
