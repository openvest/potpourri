
; # Setup Datascript
; move some to deps.edn?


(require '[clojure.tools.deps.alpha.repl :refer [add-libs]])
^{::clerk/visibility {:result :hide}}
(add-libs '{datascript/datascript {:mvn/version "1.4.2"}})

^{::clerk/visibility {:result :hide}}
(require '[datascript.core :as d])

; # Create the empty database

; ## Schema
; need more info on [schema usage](https://github.com/kristianmandrup/datascript-tutorial/blob/master/create_schema.md).
;;
;; Unlike XTDB, in Datascript, we quire some sechema setup. This identifies attributes like `:issuer` as references to other entities.
;; In RDF terms that means another subject, in OO terms it means another object.

^{::clerk/visibility {:result :hide}}
(def schema {:issuer   {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
              :asset    {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
              :holdings {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}})
^{::clerk/visibility {:result :hide}}
(def conn (d/create-conn schema))
;; from xtdb nextjournal notebook
(d/transact! conn
             [ ;; add 3 companies
              {:db/id ":company/t1", :name "IBM"}
              {:db/id ":company/t2", :name "JP Morgan"}
              {:db/id -100, :name "Ford"}
              ;; add 3 assets based on those companies
              {:db/id ":security/t1",
               :issuer ":company/t1" :ticker "IBM",
               :type :Equity}
              {:db/id ":security/t2", 
               :issuer ":company/t2" :ticker "JPM",
               :type :Equity}
              {:db/id ":security/t3",
               :issuer -100 :ticker "F",
               :type :Equity}
              ;; add 2 positions (portfolio unspecified)
              {:db/id ":pm/p1", :asset ":security/t1", :quantity 100}
              {:db/id ":pm/p2", :asset ":security/t2", :quantity 200}
              ;; add portfolio (with those 2 positions)
              {:db/id ":p/p1", 
               :holdings [":pm/p1" ":pm/p2"],
               :name "My Trading Portfolio"}])

;; use pull syntax to get security info and issuer name
(d/q '[:find (pull ?security  ["*" {:issuer [:name]}])
       :where [?security :ticker "F"]]
     @conn)


(d/q '[:find (pull ?h  [:quantity {:asset [:ticker {:issuer [:name]}]}])
       :where [?port :name "My Trading Portfolio"]
              [?port :holdings ?h]]
     @conn)

;; same info flattened
(d/q '[:find ?t ?q ?n
       :where [?port :name "My Trading Portfolio"]
              [?port :holdings ?h]
              [?h :quantity ?q]
              [?h :asset ?a]
              [?a :ticker ?t]
              [?a :issuer ?c]
              [?c :name ?n]]
     @conn)
;; now transpose so we can use it for a dataset
(->> (d/q '[:find ?t ?q ?n
       :where [?port :name "My Trading Portfolio"]
              [?port :holdings ?h]
              [?h :quantity ?q]
              [?h :asset ?a]
              [?a :ticker ?t]
              [?a :issuer ?c]
              [?c :name ?n]]
     @conn)
 (apply map vector)
 (map vector [:ticker :quantity :name])
 (into {}))
