; Copyright (c) 2017-present Walmart, Inc.
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns com.walmartlabs.lacinia.pedestal-async-error-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [com.walmartlabs.lacinia.pedestal :refer [inject default-interceptors] :as lp]
   [com.walmartlabs.lacinia.test-utils :refer [error-proof-interceptor
                                               test-server-fixture
                                               send-request]]
   [clojure.spec.test.alpha :as stest]
   [io.pedestal.interceptor :refer [interceptor]]
   [io.pedestal.interceptor.chain :as chain]))

(stest/instrument)

(def error-proof (atom nil))

(use-fixtures :once (test-server-fixture {}
                                         (fn [schema]
                                           {:interceptors (-> (default-interceptors schema {:async true})
                                                              ;; Should be called
                                                              (inject (error-proof-interceptor error-proof)
                                                                      :after  ::lp/error-response))})))

(deftest calls-post-error-interceptor
  (reset! error-proof nil)
  (send-request "{ fail }")
  (let [[ctx ex] @error-proof]
    (is (= "java.lang.IllegalStateException in Interceptor :com.walmartlabs.lacinia.pedestal/async-query-executor - resolver exception"
           (ex-message ex)))
    (is (= {:exception-type :java.lang.IllegalStateException
            :execution-id   (::chain/execution-id ctx)
            :interceptor    ::lp/async-query-executor
            :stage          :enter}
           (dissoc (ex-data ex) :exception)))
    (is (instance? Exception
                   (:exception (ex-data ex))))))
