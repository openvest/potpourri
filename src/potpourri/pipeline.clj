(ns user)
;; https://tbaldridge.pivotshare.com/media/design-thoughts-ep-4-why-i-avoid-closures/76800/feature

(defn markup [amount]
  {:op :mul :amount (+ 1.0 amount) :desc "Add Markup"})

(defn tax [region amount]
  {:op :mul :region region :amount (+ 1.0 amount)
   :desc (str "Add " region " tax of " amount  )})

(defn charity [charity amount]
  {:op :add :charity charity :amount amount
   :desc (str "Donate  " amount " to " charity  )})



(defmulti process-step (fn [input step]
                         (:op step)))


(defmethod process-step :mul
  [acc {:keys [amount]}]
  (* acc amount))

(defmethod process-step :add
  [acc {:keys [amount]}]
  (+ acc amount))


(def pipeline [(markup 0.25)
               (tax :city 0.03)
               (tax :state 0.10)
               (charity :good-will 2.0)])

(reduce process-step 120 pipeline)
