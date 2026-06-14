#!/usr/bin/env bb
;; /opt/stacks/swtp-dev/deploy.bb
;; Pulls :dev images and restarts the swtp-dev stack.

(require '[babashka.process :refer [sh]]
         '[clojure.string :as str])

(def script-dir (-> (sh "dirname" (System/getProperty "babashka.file")) :out str/trim))
(def logfile (str script-dir "/deploy.log"))

(defn now-str []
  (-> (sh "date" "+%Y-%m-%d %H:%M:%S") :out str/trim))

(defn log [msg]
  (spit logfile (str "[" (now-str) "] " msg "\n") :append true))

(log "Deploy triggered (swtp-dev)")

;; Pull images
(let [{:keys [out]} (sh "docker" "compose" "-f" (str script-dir "/docker-compose.yml") "pull")]
  (doseq [svc ["swtp-dev-api" "swtp-dev-web"]]
    (cond
      (re-find (re-pattern (str "(?s)" svc ".*Downloaded newer image")) out)
      (log (str svc ": updated"))
      (re-find (re-pattern (str "(?s)" svc ".*Image is up to date")) out)
      (log (str svc ": up-to-date"))
      :else
      (log (str svc ": check output")))))

(log "Restarting services")
(sh "docker" "compose" "-f" (str script-dir "/docker-compose.yml") "up" "-d")
(log "Deploy complete")
(spit logfile "----------------------------------------\n" :append true)
