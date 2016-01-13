(ns api-velociraptor.app
  (:require [api-velociraptor.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
