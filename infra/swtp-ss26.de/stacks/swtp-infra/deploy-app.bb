#!/usr/bin/env bb
;; /opt/stacks/swtp-infra/deploy.bb <stack>
;; Pulls images and restarts an app stack. <stack> is "main" or "dev".

(require '[babashka.process :refer [sh]]
         '[cheshire.core :as json]
         '[clojure.string :as str])

;; ── Per-stack config ─────────────────────────────────────────────────────────
(def stacks
  {"main" {:tag       "latest"
           :dashboard {"fe"         "https://www.swtp-ss26.de"
                       "keycloak"   "https://auth.swtp-ss26.de"
                       "maildev"    "https://mail.swtp-ss26.de"
                       "hoppscotch" "https://restclient.swtp-ss26.de"
                       "github"     "https://github.com/mlanima/thm-swtp"
                       "logs"       "https://logs.swtp-ss26.de"}}
   "dev"  {:tag       "dev"
           :dashboard {"fe"         "https://dev.swtp-ss26.de"
                       "be"         "https://api.dev.swtp-ss26.de/swagger-ui/index.html"
                       "logs"       "https://logs.dev.swtp-ss26.de"}}})

(def stack   (first *command-line-args*))
(def api-key (second *command-line-args*))
(def cfg     (get stacks stack))

(when-not cfg
  (binding [*out* *err*]
    (println (str "usage: deploy.bb <stack>  (one of: " (str/join ", " (keys stacks)) ")")))
  (System/exit 2))

(def stack-dir    (str "/opt/stacks/swtp-" stack))
(def compose-file (str stack-dir "/docker-compose.yml"))
(def logfile      (str stack-dir "/deploy.log"))
(def dashboard-file "/opt/stacks/swtp-infra/dashboard/data.json")
(def services [(str "swtp-" stack "-api") (str "swtp-" stack "-web")])

;; ── Logging ──────────────────────────────────────────────────────────────────
(defn now-str [] (-> (sh "date" "+%Y-%m-%d %H:%M:%S") :out str/trim))
(defn log [msg] (spit logfile (str "[" (now-str) "] " msg "\n") :append true))
(defn done [code]
  (spit logfile "----------------------------------------\n" :append true)
  (System/exit code))

(log (str "Deploy triggered (swtp-" stack ")"))

;; ── Inject secrets ────────────────────────────────────────────────────────────
(when api-key
  (let [env-file (str stack-dir "/.env")]
    (spit env-file (str "OPENAI_API_KEY=" api-key "\n") :append true)
    (log (str "Wrote OPENAI_API_KEY to .env"))))

;; ── Pull ─────────────────────────────────────────────────────────────────────
(def pull (sh ["docker" "compose" "-f" compose-file "pull"] {:dir stack-dir}))
(when-not (zero? (:exit pull))
  (log (str "WARNING: 'docker compose pull' exited " (:exit pull)
            (let [err (str/trim (str (:err pull)))]
              (when (seq err) (str " — " err))))))

;; ── Verify each service has a usable image; bail loudly if not ───────────────
;; A failed pull (e.g. tag missing in the registry) would otherwise restart a
;; stale image — or fail on `up`. Check before touching the running stack.
(defn service-image [svc]
  (-> (sh ["docker" "compose" "-f" compose-file "config" "--images" svc] {:dir stack-dir})
      :out str/trim))
(defn image-present? [image]
  (zero? (:exit (sh ["docker" "image" "inspect" "-f" "{{.Id}}" image] {:dir stack-dir}))))

(def missing
  (filterv (fn [svc] (not (image-present? (service-image svc)))) services))

(doseq [svc missing]
  (log (str svc ": IMAGE MISSING (" (service-image svc) ") — pull failed")))

(when (seq missing)
  (log (str "ABORT: missing image(s): " (str/join ", " missing) " — not restarting"))
  (done 1))

;; ── Restart ──────────────────────────────────────────────────────────────────
(log "Restarting services")
(def up (sh ["docker" "compose" "-f" compose-file "up" "-d"] {:dir stack-dir}))
(when-not (zero? (:exit up))
  (log (str "ERROR: 'docker compose up -d' exited " (:exit up)
            (let [err (str/trim (str (:err up)))]
              (when (seq err) (str " — " err)))))
  (done 1))

;; ── Dashboard ────────────────────────────────────────────────────────────────
(let [data (if (.exists (java.io.File. dashboard-file))
             (json/parse-string (slurp dashboard-file))
             {})]
  (spit dashboard-file
        (json/generate-string
          (assoc data stack (assoc (:dashboard cfg) "updated_at" (now-str)))
          {:pretty true})))

(log "Deploy complete")
(done 0)
