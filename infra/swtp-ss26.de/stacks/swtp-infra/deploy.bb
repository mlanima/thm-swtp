#!/usr/bin/env bb
;; /opt/stacks/swtp-infra/deploy.bb
;; Starts the swtp-infra stack (MySQL, Keycloak, Hoppscotch).

(require '[babashka.process :refer [sh]]
         '[clojure.string :as str])

(def script-dir (-> (sh "dirname" (System/getProperty "babashka.file")) :out str/trim))
(def logfile (str script-dir "/deploy.log"))

(defn now-str []
  (-> (sh "date" "+%Y-%m-%d %H:%M:%S") :out str/trim))

(defn log [msg]
  (spit logfile (str "[" (now-str) "] " msg "\n") :append true))

(log "Deploy triggered (swtp-infra)")
(sh ["docker" "compose" "-f" (str script-dir "/docker-compose.yml") "up" "-d"] {:dir script-dir})
(log "Deploy complete")
(spit logfile "----------------------------------------\n" :append true)
