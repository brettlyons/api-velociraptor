(ns api-velociraptor.config
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [api-velociraptor.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[api-velociraptor started successfully using the development profile]=-"))
   :middleware wrap-dev})
