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
(def services ["swtp-dev-api" "swtp-dev-web"])

(defn image-ids []
  (into {}
        (for [svc services]
          [svc (-> (sh ["docker" "compose" "-f" (str script-dir "/docker-compose.yml") "images" "-q" svc] {:dir script-dir})
                    :out str/trim)])))

(let [before (image-ids)]
  (sh ["docker" "compose" "-f" (str script-dir "/docker-compose.yml") "pull"] {:dir script-dir})
  (let [after (image-ids)]
    (doseq [svc services]
      (if (= (get before svc) (get after svc))
        (log (str svc ": up-to-date"))
        (log (str svc ": updated"))))))

(log "Restarting services")
(sh ["docker" "compose" "-f" (str script-dir "/docker-compose.yml") "up" "-d"] {:dir script-dir})
(log "Deploy complete")
(spit logfile "----------------------------------------\n" :append true)
