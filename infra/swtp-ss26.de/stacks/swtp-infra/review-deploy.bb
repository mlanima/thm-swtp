#!/usr/bin/env bb
;; /opt/stacks/swtp-infra/review-deploy.bb <namespace> <pr-number>
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
(def logfile "/opt/stacks/swtp-infra/deploy.log")
(def script-dir (-> (sh "dirname" (System/getProperty "babashka.file")) :out str/trim))
;; ──────────────────────────────────────────────────────────────────────────────

;; Parse args
(def args (filter #(not (str/blank? %)) *command-line-args*))
(def namespace (first args))
(def pr-num (second args))
(def registry (str "ghcr.io/" namespace))
(def db-name (str "swtp_pr_" pr-num))
(def template-db "swtp_template")

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
        "--env-file" "/opt/stacks/swtp-infra/review.env"
        "--label" "traefik.enable=true"
        "--label" (str "traefik.http.routers." name ".entrypoints=websecure")
        "--label" (str "traefik.http.routers." name ".rule=Host(`" host "`)")
        "--label" (str "traefik.http.routers." name ".tls=true")
        "--label" (str "traefik.http.routers." name ".tls.certresolver=" certresolver)
        "--label" (str "traefik.http.routers." name ".tls.domains[0].main=*." domain)
        "--label" (str "traefik.http.services." name ".loadbalancer.server.port=8080")
        image)
    (log (str "Backend live → https://" host))))

;; ── DB clone (swtp_template -> swtp_pr_<n>) ─────────────────────────────────
;; MySQL has no CREATE DATABASE ... LIKE-with-data, so: mysqldump | mysql.

(defn read-env [path key]
  (->> (slurp path)
       str/split-lines
       (keep #(let [[k v] (str/split % #"=" 2)]
                (when (= (some-> k str/trim) key) (some-> v str/trim))))
       first))

(def env-file (str script-dir "/.env"))
(def db-root-pw (read-env env-file "MYSQL_ROOT_PASSWORD"))
(def db-app-user (or (read-env env-file "MYSQL_USER") "swtp"))

(defn mysql! [sql]
  (sh "sudo" "docker" "exec" "-i" "swtp-db" "mysql" "-uroot" (str "-p" db-root-pw) "-e" sql))

(defn check-db [{:keys [exit err]} step]
  (when-not (zero? exit)
    (log (str "DB clone FAILED during " step ": " err))
    (System/exit 1)))

(defn clone-db! []
  (log (str "Cloning DB (" template-db " -> " db-name ")..."))
  (when (str/blank? db-root-pw)
    (log (str "MYSQL_ROOT_PASSWORD not found in " env-file))
    (System/exit 1))

  (check-db (mysql! (str "CREATE DATABASE IF NOT EXISTS `" db-name "`;")) "CREATE DATABASE")
  (check-db (mysql! (str "GRANT ALL PRIVILEGES ON `" db-name "`.* TO '" db-app-user "'@'%'; FLUSH PRIVILEGES;"))
            "GRANT")

  (let [dump (sh ["sudo" "docker" "exec" "swtp-db" "mysqldump" "-uroot" (str "-p" db-root-pw)
                  "--set-gtid-purged=OFF" template-db])]
    (check-db dump "mysqldump")
    (let [restore (sh ["sudo" "docker" "exec" "-i" "swtp-db" "mysql" "-uroot" (str "-p" db-root-pw) db-name]
                      {:in (:out dump)})]
      (check-db restore "mysql restore")))

  (log "DB clone done"))

(clone-db!)
(deploy-web)
(deploy-api)

(log "Review deploy complete")
(spit logfile "----------------------------------------\n" :append true)
