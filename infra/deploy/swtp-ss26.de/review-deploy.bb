#!/usr/bin/env bb
;; /opt/stacks/swtp/review-deploy.bb <pr-number> [api] [web]
;;
;; Spins up per-PR containers routed via Traefik.
;; URLs:
;;   Frontend: https://pr-<n>.review.swtp-ss26.de
;;   Backend:  https://pr-<n>-api.review.swtp-ss26.de

(require '[babashka.process :refer [sh]]
         '[clojure.string :as str])

;; ── Config ────────────────────────────────────────────────────────────────────
(def traefik-network "traefik-net")
(def certresolver "letsencrypt-inwx")
(def domain "review.swtp-ss26.de")
(def registry (or (System/getenv "GHCR_NAMESPACE") "ghcr.io/mlanima"))
(def logfile "/opt/stacks/swtp/deploy.log")
;; ──────────────────────────────────────────────────────────────────────────────

(defn now-str []
  (-> (sh "date" "+%Y-%m-%d %H:%M:%S") :out str/trim))

(defn log [msg]
  (spit logfile (str "[" (now-str) "] [PR-" pr "] " msg "\n") :append true))

;; Parse args
(def args (filter #(not (str/blank? %)) *command-line-args*))
(def pr (first args))
(def services (rest args))

(when (or (nil? pr) (empty? services))
  (binding [*out* *err*]
    (println "Usage: review-deploy.bb <pr-number> <api|web> [api|web]"))
  (System/exit 1))

(log (str "Review deploy triggered (services: " (str/join " " services) ")"))

(defn deploy-web []
  (let [name (str "swtp-web-pr-" pr)
        host (str "pr-" pr "." domain)
        image (str registry "/swtp-web:pr-" pr)]
    (log "Pulling frontend image...")
    (sh "sudo" "docker" "pull" image)
    (log "Starting frontend container...")
    (sh "sudo" "docker" "rm" "-f" name)
    (sh "sudo" "docker" "run" "-d"
        "--name" name
        "--network" traefik-network
        "--restart" "unless-stopped"
        "--label" (str "traefik.enable=true")
        "--label" (str "traefik.http.routers." name ".entrypoints=websecure")
        "--label" (str "traefik.http.routers." name ".rule=Host(`" host "`)")
        "--label" (str "traefik.http.routers." name ".tls=true")
        "--label" (str "traefik.http.routers." name ".tls.certresolver=" certresolver)
        "--label" (str "traefik.http.routers." name ".tls.domains[0].main=*." domain)
        "--label" (str "traefik.http.services." name ".loadbalancer.server.port=80")
        image)
    (log (str "Frontend live → https://" host))))

(defn deploy-api []
  (let [name (str "swtp-api-pr-" pr)
        host (str "pr-" pr "-api." domain)
        image (str registry "/swtp-api:pr-" pr)]
    (log "Pulling backend image...")
    (sh "sudo" "docker" "pull" image)
    (log "Starting backend container...")
    (sh "sudo" "docker" "rm" "-f" name)
    (sh "sudo" "docker" "run" "-d"
        "--name" name
        "--network" traefik-network
        "--network" "review_net"
        "--restart" "unless-stopped"
        "--env-file" "/opt/stacks/swtp/review.env"
        "--label" (str "traefik.enable=true")
        "--label" (str "traefik.http.routers." name ".entrypoints=websecure")
        "--label" (str "traefik.http.routers." name ".rule=Host(`" host "`)")
        "--label" (str "traefik.http.routers." name ".tls=true")
        "--label" (str "traefik.http.routers." name ".tls.certresolver=" certresolver)
        "--label" (str "traefik.http.routers." name ".tls.domains[0].main=*." domain)
        "--label" (str "traefik.http.services." name ".loadbalancer.server.port=8080")
        image)
    (log (str "Backend live → https://" host))))

(doseq [svc services]
  (case svc
    "api" (deploy-api)
    "web" (deploy-web)
    (log (str "Unknown service: " svc " — skipped"))))

(log "Review deploy complete")
(spit logfile "----------------------------------------\n" :append true)
