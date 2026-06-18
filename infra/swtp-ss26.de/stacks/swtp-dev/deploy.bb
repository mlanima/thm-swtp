#!/usr/bin/env bb
;; /opt/stacks/swtp-dev/deploy.bb
;; Pulls :dev images and restarts the swtp-dev stack.

(require '[babashka.process :refer [sh]]
         '[cheshire.core :as json]
         '[clojure.string :as str])

(def script-dir (-> (sh "dirname" (System/getProperty "babashka.file")) :out str/trim))
(def logfile (str script-dir "/deploy.log"))
(def dashboard-file "/opt/stacks/swtp-infra/dashboard/data.json")

(defn now-str []
  (-> (sh "date" "+%Y-%m-%d %H:%M:%S") :out str/trim))

(defn log [msg]
  (spit logfile (str "[" (now-str) "] " msg "\n") :append true))

(log "Deploy triggered (swtp-dev)")

;; Pull images
(def services ["swtp-dev-api" "swtp-dev-web"])
(def compose-file (str script-dir "/docker-compose.yml"))

(defn service-image [svc]
  (-> (sh ["docker" "compose" "-f" compose-file "config" "--images" svc] {:dir script-dir})
      :out str/trim))

(defn image-id [image]
  (-> (sh ["docker" "image" "inspect" "-f" "{{.Id}}" image] {:dir script-dir})
      :out str/trim))

(def service-images (into {} (for [svc services] [svc (service-image svc)])))

(def before (into {} (for [svc services] [svc (image-id (get service-images svc))])))

;; Pull and actually check the result. A failed pull (e.g. missing tag in the
;; registry) leaves the local image-id unchanged, which the old before/after
;; comparison silently logged as "up-to-date".
(def pull-result (sh ["docker" "compose" "-f" compose-file "pull"] {:dir script-dir}))
(when-not (zero? (:exit pull-result))
  (log (str "WARNING: 'docker compose pull' exited " (:exit pull-result)
            (let [err (str/trim (str (:err pull-result)))]
              (when (seq err) (str " — " err))))))

(def after (into {} (for [svc services] [svc (image-id (get service-images svc))])))
(def missing (filterv #(str/blank? (get after %)) services))

(doseq [svc services]
  (cond
    (str/blank? (get after svc)) (log (str svc ": IMAGE MISSING (" (get service-images svc) ") — pull failed"))
    (= (get before svc) (get after svc)) (log (str svc ": up-to-date"))
    :else (log (str svc ": updated"))))

;; No usable image -> restarting would fail or run a stale image. Bail out loudly
;; with a non-zero exit so the caller (backup/CD) sees the failure.
(when (seq missing)
  (log (str "ABORT: missing image(s): " (str/join ", " missing) " — not restarting"))
  (spit logfile "----------------------------------------\n" :append true)
  (System/exit 1))

(log "Restarting services")
(def up-result (sh ["docker" "compose" "-f" compose-file "up" "-d"] {:dir script-dir}))
(when-not (zero? (:exit up-result))
  (log (str "ERROR: 'docker compose up -d' exited " (:exit up-result)
            (let [err (str/trim (str (:err up-result)))]
              (when (seq err) (str " — " err)))))
  (spit logfile "----------------------------------------\n" :append true)
  (System/exit 1))

;; ── Dashboard aktualisieren ──────────────────────────────────────────────────
(defn update-dashboard! [path entry]
  (let [data (if (.exists (java.io.File. dashboard-file))
               (json/parse-string (slurp dashboard-file))
               {})
        updated (assoc-in data (mapv str path) (assoc entry "updated_at" (now-str)))]
    (spit dashboard-file (json/generate-string updated {:pretty true}))))

(update-dashboard! ["dev"]
  {"fe" "https://dev.swtp-ss26.de"
   "be" "https://api.dev.swtp-ss26.de/swagger-ui/index.html"
   "logs" "https://logs.dev.swtp-ss26.de"})

(log "Deploy complete")
(spit logfile "----------------------------------------\n" :append true)
