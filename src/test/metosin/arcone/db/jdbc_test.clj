(ns metosin.arcone.db.jdbc-test
  (:require [clojure.test :as test :refer [deftest is]]
            [matcher-combinators.test]
            [next.jdbc]
            [metosin.arcone.db.jdbc :as jdbc]
            [metosin.arcone.test.db-fixture :as db-fixture :refer [*test-ds*]]))


(test/use-fixtures :once
  (db-fixture/with-test-db)
  (db-fixture/with-test-datasource))


(deftest execute-one!-test
  (is (match? {:now (partial instance? java.util.Date)}
              (jdbc/execute-one! *test-ds* ["select now() as now"]))))


(deftest execute!-test
  (is (match? [{:now (partial instance? java.util.Date)}]
              (jdbc/execute! *test-ds* ["select now() as now"]))))

