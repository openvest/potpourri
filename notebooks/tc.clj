(require '[tablecloth.api :as tc])

;; # Load some data
(def stocks 
  (-> "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv"
      (tc/dataset {:key-fn keyword})
      (tc/group-by (fn [row]
                     {:symbol (:symbol row)
                      :year (tech.v3.datatype.datetime/long-temporal-field :years (:date row))}))
      (tc/aggregate #(tech.v3.datatype.functional/mean (% :price)))
      (tc/order-by [:symbol :year])))


;; We can use `.toString` and `clerk/md` (markdown).
;; There should be a viewer, if not we should create one.
(-> stocks
    (tc/head 10)
    (.toString)
    (clerk/md))
