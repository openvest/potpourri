;; records to use
(def all
  [[1 #inst "2010-12-25" "Mr Slate" 1 true]
   [2 #inst "2010-12-25" "Fred Flintstone" 1 true ]
   [3 #inst "2010-12-25" "Wilma Flintstone" 2 false]
   [4 #inst "2014-12-25" "Pebbles Flintstone" 3 false]
   [5 #inst "2012-12-25" "Betty Rubble" 5 true]
   [6 #inst "2012-12-25" "Barney Rubble" 5 false]
   [7 #inst "2014-12-25" "Bambam Rubble" 6 false]                  
   [7 #inst "2015-12-25" "Bambam Rubble" 2 false]                  
])

(defrecord Employee [emp-id asof name mgr-id is-comp-mgr])

;; put records into one set per date and each set indexed by empid
(def dt-map (->> all
                 ;; make keyed records
                 (map (partial apply ->Employee))
                 ;; make asof -> emp-id -> employee map
                 (reduce (fn [acc rec]
                           (assoc-in acc ((juxt :asof :emp-id) rec) rec)) {})
                 sort
                 (reductions (fn [[d0 m0] [d1 m1]]
                               [d1 (into m0 m1)]))))


(defn comp-mgr
  "find the manager id of an employee
  airity 2 is employee map for a date and employee id
  airity 3 is all maps, asof date and employee id
   asofdate should be an `#inst`"
  ([e-map eid]
   (let [mid (get-in e-map [eid :mgr-id])]
     (cond (nil? mid) :none  ;; maybe raise an error as eid was not found
           (get-in e-map [mid :is-comp-mgr]) mid
           :else (recur e-map mid))))
  ([dt-map asof eid]
   (-> (take-while #(= 1 (compare  asof (first %))) dt-map)  ;; bisect ??
       last ;; latest date that is apt
       second ;; just the map
       (comp-mgr eid))))


(comment
  (comp-mgr dt-map #inst "2015-12-24" 6)
)
