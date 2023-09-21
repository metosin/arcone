(ns metosin.arcone.db.honey-test
  (:require [clojure.test :as test :refer [deftest is]]
            [matcher-combinators.test]
            [next.jdbc]
            [metosin.arcone.test.db-fixture :as db-fixture :refer [*test-ds*]]
            [metosin.arcone.db.honey :as honey]))


(test/use-fixtures :once
  (db-fixture/with-test-db)
  (db-fixture/with-test-datasource))


(deftest execute-one!-test
  (let [ds *test-ds*]
    (is (match? {:ts (partial instance? java.util.Date)}
                (honey/execute-one! ds {:select [[[:now] :ts]]})))))


(deftest execute!-test
  (let [ds *test-ds*]
    (is (match? [{:ts (partial instance? java.util.Date)}]
                (honey/execute! ds {:select [[[:now] :ts]]})))))

