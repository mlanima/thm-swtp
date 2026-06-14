#!/usr/bin/env bb
;; /opt/stacks/swtp-main/deploy.bb
;; Pulls :latest images and restarts the swtp-main stack.

(require '[babashka.process :refer [sh]]
         '[clojure.string :as str])

(def script-dir (-> (sh "dirname" (System/getProperty "babashka.file")) :out str/trim))
(def logfile (str script-dir "/deploy.log"))

(defn now-str []
  (-> (sh "date" "+%Y-%m-%d %H:%M:%S") :out str/trim))

(defn log [msg]
  (spit logfile (str "[" (now-str) "] " msg "\n") :append true))

(log "Deploy triggered (swtp-main)")

;; Pull images
(def services ["swtp-main-api" "swtp-main-web"])
(def compose-file (str script-dir "/docker-compose.yml"))

(defn service-image [svc]
  (-> (sh ["docker" "compose" "-f" compose-file "config" "--images" svc] {:dir script-dir})
      :out str/trim))

(defn image-id [image]
  (-> (sh ["docker" "image" "inspect" "-f" "{{.Id}}" image] {:dir script-dir})
      :out str/trim))

(def service-images (into {} (for [svc services] [svc (service-image svc)])))

(let [before (into {} (for [svc services] [svc (image-id (get service-images svc))]))]
  (sh ["docker" "compose" "-f" compose-file "pull"] {:dir script-dir})
  (let [after (into {} (for [svc services] [svc (image-id (get service-images svc))]))]
    (doseq [svc services]
      (if (= (get before svc) (get after svc))
        (log (str svc ": up-to-date"))
        (log (str svc ": updated"))))))

(log "Restarting services")
(sh ["docker" "compose" "-f" (str script-dir "/docker-compose.yml") "up" "-d"] {:dir script-dir})
(log "Deploy complete")
(spit logfile "----------------------------------------\n" :append true)
