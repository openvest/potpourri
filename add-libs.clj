;; from sean corfield https://github.com/seancorfield/dot-clojure/blob/develop/deps.edn
{:extra-deps {org.clojure/tools.deps.alpha ; add-lib3 branch
                {:git/url "https://github.com/clojure/tools.deps.alpha"
                 :sha "e4fb92eef724fa39e29b39cc2b1a850567d490dd"}}}

;; also an older add-lib from https://insideclojure.org/2018/05/04/add-lib/

(require '[clojure.tools.deps.alpha.repl :refer [add-libs]])

(add-libs '{datascript/datascript {:mvn/version "1.4.2"}})
(add-libs '{org.clj-commons/hickory {:mvn/version "0.7.3"}})
(add-libs '{org.clojure/test.check {:mvn/version "1.1.1"}})


(comment
  clj -Sdeps "{:deps
               {org.clojure/tools.deps.alpha
                {:git/url \"https://github.com/clojure/tools.deps.alpha.git\"
                 :sha \"e4fb92eef724fa39e29b39cc2b1a850567d490dd\"}}}"
  ;; powershell vesion:  or """ for quotes?
  clj -Sdeps '{:deps
               {org.clojure/tools.deps.alpha
                {:git/url """https://github.com/clojure/tools.deps.alpha.git"""
                 :sha """e4fb92eef724fa39e29b39cc2b1a850567d490dd"""}}}'

  ;; maybe in emacs:
  (cider-add-to-alist 'cider-jack-in-dependencies
                      "org.clojure/tools.deps.alpha"
                      '(("git/sha" . "e4fb92eef724fa39e29b39cc2b1a850567d490dd")
                        ("git/url" . "https://github.com/clojure/tools.deps.alpha.git")))
  )
