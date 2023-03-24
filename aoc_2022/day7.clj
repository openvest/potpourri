(require '[clojure.zip :as z]
         '[clojure.string :as str])

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


(-> (z/xml-zip {:tag :root :attrs {:name "/"} :content '()})
    (z/append-child {:tag :dir :attrs {:name "d1"} :content '()})
    (z/down) z/rightmost
    (z/append-child {:tag :file :attrs {:name "foo.txt" :size 1200}})
    (z/append-child {:tag :file :attrs {:name "bar.xls" :size 222}})
    (z/append-child {:tag :dir :attrs {:name "d11"}})
    (z/down) (z/rightmost)
    (z/append-child {:tag :file :attrs {:name "bard.mp4" :size 444222}})
    (z/up))


(defn do-one [loc row]
  (if-let [[_ size fname] (re-matches #"([0-9]+) *(.*)" row)]
    ;; if it starts with an int, append a child file
    (z/append-child loc  {:tag :file :attrs {:name fname :size (Integer/parseInt size)}})
    (cond
      ;; go up one dir
      (= row "$ cd ..") (z/up loc)
      ;; create new subdir and go there
      (str/starts-with? row "$ cd ")
      (let [subdir (subs row 5)]
        (-> loc
            (z/append-child {:tag :dir :attrs {:name subdir}})
            (z/down)
            (z/rightmost)))
      ;; ignore ls command and subdirectories (until we navigate to them that is)
      :else loc)))

(def x
  (->> day7-input
       (#(str/split % #" *\n *"))
       (rest)
       (reduce do-one (z/xml-zip {:tag :dir :attrs {:name "/"}}))
       (z/root)))

(defn accsz
  [acc node]
  (if-let [subdir (:content node)]
    (let [dir-name (-> node :attrs :name)
          subtotals  (reduce accsz [[dir-name 0]] subdir)
          subtotal (get-in subtotals [0 1])]
      (into (update-in acc [0 1] + subtotal) subtotals))
    (let [fsize (-> node :attrs :size)]
      (update-in acc [0 1] + fsize))))

;; why is this root needed/duplicated?
(reduce accsz [[:root 0]] [x])

(->> [x]
     (reduce accsz [["/" 0]])
     (transduce (comp (map second) (filter #(<= % 100000))) +))

;; part 2
(let [disk-size 70000000
      required 30000000
      sizes (->> (reduce accsz [[:root 0]] [x])
                 (map second))
      shortage (- required (- disk-size (first sizes)))]
  (->> sizes
       (filter #(>= % shortage))
       (reduce min)))

