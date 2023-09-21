(ns metosin.arcone.session.apikey-test
  (:require [clojure.test :as test :refer [deftest testing is]]
            [matcher-combinators.test]
            [integrant.core :as ig]
            [metosin.arcone.session.core :as session]
            [metosin.arcone.session.apikey-middleware :as apikey]))


(deftest apikey-session-middleware-test
  (let [*apikey-header-name*      "*apikey-header-name*"
        get-account-by-apikey     (fn [apikey]
                                    (case apikey
                                      "foo" {:user "foo"}
                                      "bar" nil))
        apikey-session-middleware (ig/init-key ::apikey/apikey-session-middleware {:apikey-header-name    *apikey-header-name*
                                                                                   :get-account-by-apikey get-account-by-apikey})
        request-session           (atom nil)
        handler                   (apikey-session-middleware (fn [req]
                                                               (reset! request-session (session/get-session req))
                                                               {:status 200}))]
    (testing "request without apikey"
      (let [request {}]
        (is (match? {:status 200} (handler request)))
        (is (= nil @request-session))))
    (testing "request with correct apikey"
      (let [request {:headers {*apikey-header-name* "foo"}}]
        (is (match? {:status 200} (handler request)))
        (is (= {:user "foo"} @request-session))))
    (testing "request with incorrect apikey"
      (let [request {:headers {*apikey-header-name* "bar"}}]
        (is (match? {:status 403} (handler request)))
        (is (= {:user "foo"} @request-session))))))
