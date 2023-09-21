(ns metosin.arcone.db.hugsql-test
  (:require [clojure.test :as test :refer [deftest is]]
            [matcher-combinators.test]
            [next.jdbc]
            [metosin.arcone.db.hugsql :as hugsql]
            [metosin.arcone.test.db-fixture :as db-fixture :refer [*test-ds*]]))


(hugsql/register-sql "hugsql_test.sql")


(test/use-fixtures :once
  (db-fixture/with-test-db)
  (db-fixture/with-test-datasource))


(deftest execute-one!-test
  (is (match? {:now (partial instance? java.util.Date)
               :bar "Foo"}
              (hugsql/execute-one! *test-ds* ::get-now {:foo "Foo"}))))


(deftest execute!-test
  (is (match? [{:now (partial instance? java.util.Date)
                :bar "Foo"}]
              (hugsql/execute! *test-ds* ::get-now {:foo "Foo"}))))

