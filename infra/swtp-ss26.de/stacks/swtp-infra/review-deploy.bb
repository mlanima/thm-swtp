#!/usr/bin/env bb
;; /opt/stacks/swtp-infra/review-deploy.bb <namespace> <pr-number>
;;
;; Spins up per-PR containers (web + api) routed via Traefik.
;; URLs:
;;   Frontend: https://pr-<n>.review.swtp-ss26.de
;;   Backend:  https://pr-<n>-api.review.swtp-ss26.de

(require '[babashka.process :refer [sh]]
         '[babashka.http-client :as http]
         '[cheshire.core :as json]
         '[clojure.string :as str])

;; ── Config ────────────────────────────────────────────────────────────────────
(def traefik-network "traefik-net")
(def certresolver "letsencrypt-inwx")
(def domain "review.swtp-ss26.de")
(def logfile "/opt/stacks/swtp-infra/deploy.log")
(def script-dir (-> (sh "dirname" (System/getProperty "babashka.file")) :out str/trim))
(def kc-base "https://auth.swtp-ss26.de")
(def kc-realm "swtp")
(def kc-client-id "swtp-frontend")
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
        "--label" (str "pr=" pr-num)
        "--label" (str "traefik.http.routers." name ".entrypoints=websecure")
        "--label" (str "traefik.http.routers." name ".rule=Host(`" host "`)")
        "--label" (str "traefik.http.routers." name ".tls=true")
        "--label" (str "traefik.http.routers." name ".tls.certresolver=" certresolver)
        "--label" (str "traefik.http.routers." name ".tls.domains[0].main=*." domain)
        "--label" (str "traefik.http.services." name ".loadbalancer.server.port=4000")
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
        "-e" (str "SPRING_DATASOURCE_URL=jdbc:mysql://swtp-db:3306/" db-name)
        "--label" "traefik.enable=true"
        "--label" (str "pr=" pr-num)
        "--label" (str "traefik.http.routers." name ".entrypoints=websecure")
        "--label" (str "traefik.http.routers." name ".rule=Host(`" host "`)")
        "--label" (str "traefik.http.routers." name ".tls=true")
        "--label" (str "traefik.http.routers." name ".tls.certresolver=" certresolver)
        "--label" (str "traefik.http.routers." name ".tls.domains[0].main=*." domain)
        "--label" (str "traefik.http.services." name ".loadbalancer.server.port=8080")
        image)
    (log (str "Backend live → https://" host))))

;; ── Dozzle (Logs pro PR) ─────────────────────────────────────────────────────

(defn deploy-dozzle []
  (let [name (str "swtp-logs-pr-" pr-num)
        host (str "pr-" pr-num "-logs." domain)]
    (log "Starting Dozzle container...")
    (sh "sudo" "docker" "rm" "-f" name)
    (sh "sudo" "docker" "run" "-d"
        "--name" name
        "--network" traefik-network
        "--restart" "unless-stopped"
        "-v" "/var/run/docker.sock:/var/run/docker.sock:ro"
        "-e" (str "DOZZLE_FILTER=label=pr=" pr-num)
        "--label" "traefik.enable=true"
        "--label" (str "traefik.http.routers." name ".entrypoints=websecure")
        "--label" (str "traefik.http.routers." name ".rule=Host(`" host "`)")
        "--label" (str "traefik.http.routers." name ".tls=true")
        "--label" (str "traefik.http.routers." name ".tls.certresolver=" certresolver)
        "--label" (str "traefik.http.routers." name ".tls.domains[0].main=*." domain)
        "--label" (str "traefik.http.services." name ".loadbalancer.server.port=8080")
        "amir20/dozzle:latest")
    (log (str "Dozzle live → https://" host))))

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

;; ── Keycloak: PR-Redirect-URI + Web-Origin eintragen ────────────────────────
;; Subdomain-Wildcards werden von Keycloak nicht unterstützt → pro PR per
;; Admin-REST eintragen (vgl. CD Review-Apps – Konzept & Vorgehen, Punkt 7).

(def kc-admin (read-env env-file "KC_ADMIN"))
(def kc-admin-password (read-env env-file "KC_ADMIN_PASSWORD"))
(def kc-redirect-uri (str "https://pr-" pr-num "." domain "/*"))
(def kc-web-origin (str "https://pr-" pr-num "." domain))

(defn kc-get-admin-token []
  (let [resp (http/post (str kc-base "/realms/master/protocol/openid-connect/token")
                         {:form-params {"client_id"  "admin-cli"
                                         "grant_type" "password"
                                         "username"   kc-admin
                                         "password"   kc-admin-password}
                          :throw       false})]
    (when-not (= 200 (:status resp))
      (log (str "Keycloak token request FAILED: " (:status resp) " " (:body resp)))
      (System/exit 1))
    (-> (:body resp) json/parse-string (get "access_token"))))

(defn kc-get-client [token]
  (let [resp (http/get (str kc-base "/admin/realms/" kc-realm "/clients")
                        {:headers      {"Authorization" (str "Bearer " token)}
                         :query-params {"clientId" kc-client-id}
                         :throw        false})]
    (when-not (= 200 (:status resp))
      (log (str "Keycloak client lookup FAILED: " (:status resp) " " (:body resp)))
      (System/exit 1))
    (let [clients (json/parse-string (:body resp))]
      (when (empty? clients)
        (log (str "Keycloak client " kc-client-id " not found in realm " kc-realm))
        (System/exit 1))
      (first clients))))

(defn add-pr-redirect! []
  (log "Registering Keycloak redirect URI / web origin...")
  (when (or (str/blank? kc-admin) (str/blank? kc-admin-password))
    (log (str "KC_ADMIN / KC_ADMIN_PASSWORD not found in " env-file))
    (System/exit 1))

  (let [token (kc-get-admin-token)
        client (kc-get-client token)
        client-uuid (get client "id")
        redirect-uris (conj (set (get client "redirectUris" [])) kc-redirect-uri)
        web-origins (conj (set (get client "webOrigins" [])) kc-web-origin)
        updated (assoc client
                       "redirectUris" (vec redirect-uris)
                       "webOrigins" (vec web-origins))
        resp (http/put (str kc-base "/admin/realms/" kc-realm "/clients/" client-uuid)
                        {:headers {"Authorization" (str "Bearer " token)
                                   "Content-Type" "application/json"}
                         :body    (json/generate-string updated)
                         :throw   false})]
    (if (#{200 204} (:status resp))
      (log (str "Keycloak: registered " kc-redirect-uri " / " kc-web-origin))
      (do
        (log (str "Keycloak client update FAILED: " (:status resp) " " (:body resp)))
        (System/exit 1)))))

;; ── active-prs.txt: PR-Tracking für Backup-Restart ──────────────────────────
;; Backup-Script stoppt (Step 02) alle laufenden Container, restartet (Step 06b)
;; aber nur, was hier gelistet ist. Ohne Eintrag bleibt die Review-App nach
;; einem Backup-Lauf down (vgl. CD Review-Apps – Konzept & Vorgehen, Punkt 1).

(def active-prs-file (str script-dir "/active-prs.txt"))

(defn register-active-pr! []
  (let [entry (str "pr-" pr-num)
        existing (if (.exists (java.io.File. active-prs-file))
                   (->> (slurp active-prs-file) str/split-lines (remove str/blank?) set)
                   #{})]
    (if (contains? existing entry)
      (log "active-prs.txt: entry already present")
      (do
        (spit active-prs-file (str entry "\n") :append true)
        (log "active-prs.txt: entry added")))))

(clone-db!)
(deploy-web)
(deploy-api)
(deploy-dozzle)
(add-pr-redirect!)
(register-active-pr!)

(log "Review deploy complete")
(spit logfile "----------------------------------------\n" :append true)
