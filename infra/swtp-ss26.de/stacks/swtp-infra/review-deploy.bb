#!/usr/bin/env bb
;; /opt/stacks/swtp/review-deploy.bb <namespace> <pr-number>
;;
;; Spins up per-PR containers (web + api) routed via Traefik.
;; URLs:
;;   Frontend: https://pr-<n>.review.swtp-ss26.de
;;   Backend:  https://pr-<n>-api.review.swtp-ss26.de

(require '[babashka.process :refer [sh]]
         '[clojure.string :as str])

;; ── Config ────────────────────────────────────────────────────────────────────
(def traefik-network "traefik-net")
(def certresolver "letsencrypt-inwx")
(def domain "review.swtp-ss26.de")
(def logfile "/opt/stacks/swtp/deploy.log")
;; ──────────────────────────────────────────────────────────────────────────────

;; Parse args
(def args (filter #(not (str/blank? %)) *command-line-args*))
(def namespace (first args))
(def pr-num (second args))
(def registry (str "ghcr.io/" namespace))

(defn now-str []
  (-> (sh "date" "+%Y-%m-%d %H:%M:%S") :out str/trim))

(defn log [msg]
  (spit logfile (str "[" (now-str) "] [PR-" pr-num "] " msg "\n") :append true))

(when (or (nil? namespace) (nil? pr-num))
  (binding [*out* *err*]
    (println "Usage: review-deploy.bb <namespace> <pr-number>"))
  (System/exit 1))

(log (str "Review deploy triggered (namespace: " namespace ")"))

(defn deploy-web []
  (let [name (str "swtp-web-pr-" pr-num)
        host (str "pr-" pr-num "." domain)
        image (str registry "/swtp-web:pr-" pr-num)]
    (log "Pulling frontend image...")
    (sh "sudo" "docker" "pull" image)
    (log "Starting frontend container...")
    (sh "sudo" "docker" "rm" "-f" name)
    (sh "sudo" "docker" "run" "-d"
        "--name" name
        "--network" traefik-network
        "--restart" "unless-stopped"
        "--label" "traefik.enable=true"
        "--label" (str "traefik.http.routers." name ".entrypoints=websecure")
        "--label" (str "traefik.http.routers." name ".rule=Host(`" host "`)")
        "--label" (str "traefik.http.routers." name ".tls=true")
        "--label" (str "traefik.http.routers." name ".tls.certresolver=" certresolver)
        "--label" (str "traefik.http.routers." name ".tls.domains[0].main=*." domain)
        "--label" (str "traefik.http.services." name ".loadbalancer.server.port=80")
        image)
    (log (str "Frontend live → https://" host))))

(defn deploy-api []
  (let [name (str "swtp-api-pr-" pr-num)
        host (str "pr-" pr-num "-api." domain)
        image (str registry "/swtp-api:pr-" pr-num)]
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
        "--label" "traefik.enable=true"
        "--label" (str "traefik.http.routers." name ".entrypoints=websecure")
        "--label" (str "traefik.http.routers." name ".rule=Host(`" host "`)")
        "--label" (str "traefik.http.routers." name ".tls=true")
        "--label" (str "traefik.http.routers." name ".tls.certresolver=" certresolver)
        "--label" (str "traefik.http.routers." name ".tls.domains[0].main=*." domain)
        "--label" (str "traefik.http.services." name ".loadbalancer.server.port=8080")
        image)
    (log (str "Backend live → https://" host))))

(deploy-web)
(deploy-api)

(log "Review deploy complete")
(spit logfile "----------------------------------------\n" :append true)
