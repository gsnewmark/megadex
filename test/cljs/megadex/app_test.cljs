(ns megadex.app-test
  (:require-macros [cljs.test :refer [deftest testing is]])
  (:require [cljs.test :as t]
            [megadex.app :as app]))

(deftest test-arithmetic []
  (is (= (+ 1 2) 3)))
