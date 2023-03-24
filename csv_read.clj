;;;;;;;;;; [org.clojure/data.csv "0.1.4"] ;;;;;;;
(require '[clojure.data.csv :as csv]
         '[clojure.java.io :as io])

(defn read-csv [fname]
  (with-open [file (io/reader fname)]
    (let [reader (csv/read-csv file)
          headers (map keyword (first reader))]
      (doall (->> (rest reader)
                  (map #(zipmap headers %)))))))

;;;;;;;;;; [clojure-csv/clojure-csv "2.0.2"] ;;;
(require '[clojure-csv.core :as cc])

(cc/parse-csv (slurp "project-files/sp500.csv"))
(csv/read-csv (slurp "project-files/sp500.csv"))

;;;;;;;;;; [semantic-csv "0.2.1-alpha1"] ;;;;;;;
(require '[semantic-csv.core :as sc])
