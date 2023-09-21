(ns metosin.arcone.test.db-fixture-test
  (:require [clojure.test :as test :refer [deftest is]]
            [matcher-combinators.test]
            [metosin.arcone.test.db-fixture :as db-fixture :refer [*test-dbname* *test-ds*]]
            [next.jdbc :as jdbc]))


;;
;; Test DB setup:
;;


(def test-db {:flyway {:schemas      ["flyway" "test"]
                       :locations    ["metosin/arcone/test/db-fixture-test"]
                       :placeholders {"test_username" "tiger"
                                      "test_password" "hunter2"}
                       :clean?       true}
              :ds     {:username          "tiger"
                       :password          "hunter2"
                       :connectionInitSql "set role monkey"}})


;;
;; Set fixtures:
;;


(test/use-fixtures :once
  (db-fixture/with-test-db test-db))


(test/use-fixtures :each
  (db-fixture/with-test-fixture test-db)
  (db-fixture/with-test-datasource test-db))


;;
;; Test helpers:
;;


(defn get-connection-tiger [] (db-fixture/get-connection "tiger" "hunter2"))


;;
;; Tests:
;;


(deftest ^:db database-is-created-test
  (is (= {:current_database *test-dbname*}
         (with-open [conn (db-fixture/get-connection "postgres" "postgres")]
           (jdbc/execute-one! conn ["select current_database()"])))))


(deftest ^:db migration-is-applied-test
  (is (= {:current_user "tiger"}
         (with-open [conn (get-connection-tiger)]
           (jdbc/execute-one! conn ["select current_user"])))))


;; These two tests below are identical, on purpose. They both alter the database
;; table content and test that the changes they make and not visible to each
;; other. So in other words; these tests check that the migrations are properly 
;; cleaned between the tests.

(deftest ^:db fixture-is-applied-for-each-test-1-test
  (with-open [conn (get-connection-tiger)]
    (jdbc/execute-one! conn ["set role monkey"])
    (jdbc/with-transaction [tx conn]
      (jdbc/execute-one! tx ["insert into test.test_data (id) values (1)"]))
    (is (= {:count 1}
           (jdbc/execute-one! conn ["select count(*) from test.test_data as count"])))))


;; Part 2 of the above test:

(deftest ^:db fixture-is-applied-for-each-test-2-test
  (with-open [conn (get-connection-tiger)]
    (jdbc/execute-one! conn ["set role monkey"])
    (jdbc/with-transaction [tx conn]
      (jdbc/execute-one! tx ["insert into test.test_data (id) values (1)"]))
    (is (= {:count 1}
           (jdbc/execute-one! conn ["select count(*) from test.test_data as count"])))))


(deftest ^:db datasource-is-created-test
  (is (= {:answer 42}
         (with-open [conn (jdbc/get-connection *test-ds*)]
           (jdbc/execute-one! conn ["select 42 as answer"])))))


(deftest ^:db connection-init-sql-is-applied-test
  (is (= {:session_user_role "tiger"
          :current_user_role "monkey"}
         (with-open [conn (jdbc/get-connection *test-ds*)]
           (jdbc/execute-one! conn ["select * from test.who_am_i()"])))))
