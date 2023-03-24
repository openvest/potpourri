"This does not work because maps do not keep order
Thus the attempt to insert a node and navigate to it with
z/append-child -> z/down z/rightmost
does not always work"

(require '[clojure.zip :as z])

(def day7-input "$ cd /
$ ls
dir a
14848514 b.txt
8504156 c.dat
dir d
$ cd a
$ ls
dir e
29116 f
2557 g
62596 h.lst
$ cd e
$ ls
584 i
$ cd ..
$ cd ..
$ cd d
$ ls
4060174 j
8033020 d.log
5626152 d.ext
7214296 k")

(def day7-input (slurp "day7_input.txt"))

(defn map-zipper
  "create a zipper that uses maps
  from https://clojuredocs.org/clojure.zip/zipper"
  [m]
  (zip/zipper 
   (fn [x] (or (map? x) (map? (nth x 1))))
   (fn [x] (seq (if (map? x) x (nth x 1))))
   (fn [x children]
     (if (map? x) 
       (into {} children) 
       (assoc x 1 (into {} children))))
   m))

(defn do-one [loc row]
  (if-let [[_ size fname] (re-matches #"([0-9]+) *(.*)" row)]
    ;; if it starts with an int, append a child file
    (z/append-child loc [fname (Integer/parseInt size)])
    (cond
      ;; go up one dir
      (= row "$ cd ..") (z/up loc)
      ;; create new subdir and go there
      (str/starts-with? row "$ cd ")
      (let [subdir (subs row 5)]
        (when (= subdir "sqphfslv")
          (tap> (str "creating subdir for " subdir row " " loc)))
        ;; (tap> (str "\ncreating dir " subdir))
        (-> loc
            (z/append-child [(subs row 5) {}])
            (z/down)
            (z/rightmost)
            ((fn [loc] 
               (when-not (= subdir (first (z/node loc)))
                 (tap> (str "created dir  " (first (z/node loc))))
                 (tap> (str "oops " (z/children (z/up loc)))))
               loc))))
      ;; ignore ls command and subdirectories (until we navigate to them that is)
      :else loc)))

(def x
  (->> day7-input
       (#(str/split % #" *\n *"))
       (reduce do-one (map-zipper {}))
       (z/root)))

(defn accsz
  ([] [])
  ([acc] acc )
  ([acc [k v]]
   (if (map? v)
     (let [subs (reduce accsz [[k 0]] v)
           subtotal (get-in subs [0 1])]
       (into (update-in acc [0 1] + subtotal) subs))
     (update-in acc [0 1] + v))))

;; what's wrong with this:
;; (reduce accsz [] x)
(reduce accsz [["/" 0]] (get x "/"))

(->> (get x "/")
     (reduce accsz [["/" 0]])
     (transduce (comp (map second) (filter #(<= % 100000))) +))


(comment 
  (reverse (tree-seq map? (fn [map] ) vals x))

  (->> (map-zipper x)
       (iterate z/next)
       (take-while (complement z/end?))
       (map z/node)))



;; shows an error 
(map (fn [[k v]] [k (if (map? v) (keys v) v)])  (get x "/"))
(filter (fn [[k v]] [k (if (map? v) (keys v) v)])  (get x "/"))

;; error is in the map-zip creation not in the accsz accumulator
;; why is "sqphfslv" empty?
