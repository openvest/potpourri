(ns potpourri.palindrome-test
  (:require  [potpourri.palindrome :refer :all]
             [clojure.test :refer [deftest is] :as test]))

(deftest pal-test
  (is (= 1 (pal "abc")))
  (is (= 2 (pal "aa")))
  (is (= 3 (pal "xaqza")))
  (is (= 3 (pal "aba")))
  (is (= 5 (pal "abxba")))
  (is (= 5 (pal "ababa"))))
