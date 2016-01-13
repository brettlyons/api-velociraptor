(ns api-velociraptor.config
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[api-velociraptor started successfully]=-"))
   :middleware identity})
