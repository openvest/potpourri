;; records to use
(def all
  [[1 #inst "2010-12-25" "Mr Slate" 1 true]
   [2 #inst "2010-12-25" "Fred Flintstone" 1 true]
   [3 #inst "2010-12-25" "Wilma Flintstone" 2 false]
   [4 #inst "2014-12-25" "Pebles Flintstone" 3 false]
   [5 #inst "2012-12-25" "Betty Rubble" 5 true]
   [6 #inst "2012-12-25" "Barney Rubble" 5 false]
   [7 #inst "2014-12-25" "Bambam Rubble" 6 false]                  
   [7 #inst "2015-12-25" "Bambam Rubble" 2 false]                  
])

;; put records into one set per date and each set indexed by empid

(defn mk-dt-map
  "take a collection of employees and return a seq of
  asof date (decreasing) and map of emp->manager"
  [emp-coll]
  (->> emp-coll
       ;; make keyed records
       (map #(into {} (map vector [:emp-id :asof :name :mgr-id :is-comp-mgr] %)))
       ;; make asof -> emp-id -> employee map
       (reduce (fn [acc rec]
                 (assoc-in acc ((juxt :asof :emp-id) rec) rec)) {})
       sort
       (reductions (fn [[d0 m0] [d1 m1]]
                     [d1 (into m0 m1)]))
       reverse))  ;; final reverse is so we can take the first valid date

(defn emp-map-asof
  "From a sorted seq of [dt map] find the fist map that is 
  of the correct date"
  [emp-maps asof]
  (->> emp-maps
       (drop-while  (fn [[d, m]] (< 1 (compare asof d))))
       first
       second))

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
  ([e-maps asof eid]
   (-> (emp-map-asof e-maps asof)
       (comp-mgr eid))))

;; useage:
(def emp-maps (mk-dt-map all))
(emp-map-asof emp-maps #inst "2015-06-30") ;; => map of eid->record
(comp-mgr emp-maps #inst "2015-06-30" 3) ;; => 2

(comment 
  "java.util.Collections might be the way to go but if you most often want a recent date, the above
   should be just fine.  If you want a faster version then see
   https://stackoverflow.com/questions/8949837/binary-search-in-clojure-implementation-performance")
