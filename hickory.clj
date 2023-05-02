
(use 'hickory.core)
(require '[clj-http.client :as client])

(def snp-jsoup (->  "https://en.wikipedia.org/wiki/List_of_S%26P_500_companies"
                    client/get
                    :body
                    parse))

(def snp-hiccup  (->> snp-jsoup as-hiccup (filter vector?) first))
(def snp-hickory  (->> snp-jsoup as-hickory))

;; select is for hickory not hiccup
(-> (s/select (s/child (s/id "constituents") (s/tag "tbody") (s/tag "tr"))
              snp-hickory)
    count)
;; => 504
