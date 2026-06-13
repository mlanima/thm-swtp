#!/usr/bin/env bb
;; /opt/stacks/swtp-dev/deploy.bb
;; Pulls :dev images and restarts the swtp-dev stack.

(require '[babashka.process :refer [sh]]
         '[clojure.string :as str])

(def dir (-> (sh "dirname" (System/getProperty "babashka.file")) :out str/trim))
(def logfile (str dir "/deploy.log"))

(defn now-str []
  (-> (sh "date" "+%Y-%m-%d %H:%M:%S") :out str/trim))

(defn log [msg]
  (spit logfile (str "[" (now-str) "] " msg "\n") :append true))

(log "Deploy triggered (swtp-dev)")

;; Pull images
(let [{:keys [out pull-out]} (sh "docker" "compose" "-f" (str dir "/docker-compose.yml") "pull")]
  (doseq [svc ["swtp-dev-api" "swtp-dev-web"]]
    (cond
      (str/includes? pull-out (str svc ".*Downloaded newer image"))
      (log (str svc ": updated"))
      (str/includes? pull-out (str svc ".*Image is up to date"))
      (log (str svc ": up-to-date"))
      :else
      (log (str svc ": check output")))))

(log "Restarting services")
(sh "docker" "compose" "-f" (str dir "/docker-compose.yml") "up" "-d")
(log "Deploy complete")
(spit logfile "----------------------------------------\n" :append true)
