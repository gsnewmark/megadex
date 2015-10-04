(ns megadex.app-test
  (:require [cljs.test :as t :refer-macros [is]]
            [devcards.core :refer-macros [deftest]]
            [megadex.app :as app]))

(deftest test-arithmetic []
  (is (= (+ 1 2) 3)))
