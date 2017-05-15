(ns xxx-test
  (:require [xxx :refer :all]
           [clojure.test :refer [deftest is] :as test]))

(deftest  xxx-test
  (is (= "food" (foo-xxx "d"))))

