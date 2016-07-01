(ns starcity.controllers.application.logistics
  (:require [ring.util.response :as response]
            [starcity.controllers.application.logistics
             [data :as data]
             [params :as p]]
            [starcity.controllers.utils :refer :all]
            [starcity.models.application :as application]
            [starcity.views.application.logistics :as view]))

;; =============================================================================
;; Helpers
;; =============================================================================

(defn- show-logistics*
  [{:keys [params identity] :as req} & {:keys [errors] :or {errors []}}]
  (let [property-id (:db/id (data/property-by-internal-name "alpha"))
        account-id  (:db/id identity)]
    (view/logistics
     (application/current-steps account-id)
     (data/pull-application account-id)
     (data/pull-property property-id)
     (data/pull-available-units property-id)
     errors)))

;; =============================================================================
;; API
;; =============================================================================

(defn show-logistics
  [req]
  (ok (show-logistics* req)))

(defn save! [{:keys [params form-params identity] :as req}]
  (let [vresult    (-> params p/clean p/validate)
        account-id (:db/id identity)]
    (if-let [{:keys [selected-lease pet availability property-id]} (valid? vresult)]
      (let [desired-availability (data/availability property-id availability)]
        ;; If there is an existing rental application for this user, we're
        ;; dealing with an update.
        (if-let [{application-id :db/id} (application/by-account-id account-id)]
          (application/update! application-id
                               {:desired-lease        selected-lease
                                :desired-availability desired-availability
                                :pet                  pet})
          ;; otherwise, we're creating a new rental application for this user.
          (application/create! account-id selected-lease desired-availability :pet pet))
        ;; afterwards, redirect to the next step
        (response/redirect "/application/checks"))
      ;; results aren't valid! indicate errors
      (malformed (show-logistics* req :errors (errors-from vresult))))))
