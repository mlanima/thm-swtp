#!/usr/bin/env bb
;; /opt/stacks/swtp-infra/review-deploy.bb --namespace <namespace> --pr <pr-number>
;;
;; Features:
;; - clones the template DB into a per-PR schema
;; - provisions a per-PR upload directory from the upload template
;; - spins up per-PR containers (web + api + dozzle) routed via Traefik
;; - registers the PR's redirect URI and web origin in Keycloak
;; - registers the PR in active-prs.txt for backup-restart tracking
;; - updates the dashboard JSON with the PR's URLs
;;
;; URLs:
;; - Frontend: https://pr-<n>.review.swtp-ss26.de
;; - Backend:  https://pr-<n>-api.review.swtp-ss26.de
;; - Logs:     https://pr-<n>-logs.review.swtp-ss26.de

(require '[babashka.cli         :as    cli]
         '[babashka.process     :refer [sh]]
         '[babashka.http-client :as    http]
         '[cheshire.core        :as    json]
         '[clojure.string       :as    str])

;; =============================================================================
;; Config
;; =============================================================================
(def config
  "Static deployment configuration."
  {:traefik-network "traefik-net"
   :certresolver    "letsencrypt-inwx"
   :domain          "review.swtp-ss26.de"
   :logfile         "/opt/stacks/swtp-infra/deploy.log"
   :dashboard-file  "/opt/stacks/swtp-infra/dashboard/data.json"
   :active-prs-file "/opt/stacks/swtp-infra/active-prs.txt"
   :kc-base         "https://auth.swtp-ss26.de"
   :kc-realm        "swtp"
   :kc-client-id    "swtp-frontend"})

;; =============================================================================
;; Args
;; =============================================================================
(defn- parse-args
  "Parses command-line arguments using babashka.cli; throws ex-info on bad input."
  [args]
  (let [opts     (:opts (cli/parse-args args))
        org      (:namespace opts)
        pr-num   (let [s (str (:pr opts))]
                    (when-not (re-matches #"\d+" s)
                      (throw (ex-info (str "--pr must be a number, got: " s)
                               {:usage "review-deploy.bb --namespace <namespace> --pr <pr-number>"})))
                    s)]
    (when (or (not (string? org)) (str/blank? org) (nil? pr-num))
      (throw (ex-info "Missing required args"
               {:usage "review-deploy.bb --namespace <namespace> --pr <pr-number>"})))
    {:org org
     :pr-num    pr-num}))

(def ^:dynamic *pr-num*
  "Current PR number, bound in -main."
  nil)

;; =============================================================================
;; Logging
;; =============================================================================
(def date-fmt
  "DateTimeFormatter for log timestamps: yyyy-MM-dd HH:mm:ss"
  (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))

(defn- now-str
  "Returns current timestamp as yyyy-MM-dd HH:mm:ss string."
  []
  (.format (java.time.LocalDateTime/now) date-fmt))

(defn- log
  "Appends a timestamped log line to the deploy log."
  [msg]
  (spit (:logfile config)
        (str "[" (now-str) "] [PR-" *pr-num* "] " msg "\n")
        :append true))

;; =============================================================================
;; Environment
;; =============================================================================
(defn- load-env!
    "Parses .env from the script's directory into a map. Throws ex-info with
   a helpful message if the file is missing."
  []
  (let [bb-file  (System/getProperty "babashka.file")
        env-path (str (if bb-file
                        (.getParent (java.io.File. bb-file))
                        ".")
                      "/.env")]
    (when-not (.exists (java.io.File. env-path))
      (throw (ex-info (str ".env not found at " env-path)
               {:path env-path})))
    (into {}
          (for [line  (str/split-lines (slurp env-path))
                :let  [trimmed (str/trim line)]
                :when (and (not (str/blank? trimmed))
                           (not (str/starts-with? trimmed "#")))]
            (let [[k rest] (str/split trimmed #"=" 2)
                  v (when rest
                      (let [stripped (str/trim (str/replace rest #"\s+#.*$" ""))]
                        (when-not (str/blank? stripped) stripped)))]
              [(str/trim k) v])))))
 
(defn- env-get
  "Reads key from an .env map; throws ex-info if blank or missing."
  [env k]
  (let [v (get env k)]
    (when (str/blank? v)
      (throw (ex-info (str k " not found in .env") {:key k})))
    v))

;; =============================================================================
;; Infra helpers
;; =============================================================================
(defn- subdomain
  "Builds a PR-scoped hostname: pr-<n>[-<suffix>].<domain>
   Pass nil suffix for the bare frontend subdomain."
  [pr-num suffix]
  (let [domain (:domain config)]
    (str "pr-" pr-num (when suffix (str "-" suffix)) "." domain)))

(defn- docker!
  "Runs sudo docker with args; throws ex-info on non-zero exit."
  [& args]
  (let [res (apply sh "sudo" "docker" args)]
    (when-not (zero? (:exit res))
      (throw (ex-info (str "docker " (str/join " " (take 3 args)) " ... FAILED: " (or (:err res) "no stderr"))
                      {:args args :result res})))
    res))

(defn- mysql!
  "Executes SQL inside the swtp-db container.
   Uses MYSQL_PWD env var so the password never appears in ps aux."
  [db-root-pw sql]
  (docker! "exec" "-e" (str "MYSQL_PWD=" db-root-pw) "-i" "swtp-db" "mysql" "-uroot" "-e" sql))

(defn- assert-status!
  "Throws ex-info if HTTP response status is not in accepted (a set).
   context describes the operation for the error message."
  [resp context accepted]
  (when-not (accepted (:status resp))
    (throw (ex-info (str context " FAILED: " (:status resp) " " (:body resp))
                    {:response resp :context context}))))

;; =============================================================================
;; Container deploy (generic)
;; =============================================================================
(defn- deploy-container!
  "Starts a container with standard Traefik labels.
   Required opts: :container-name :host :image :port
   Optional opts: :extra-opts (vec of docker args inserted right before the image)
   Uses *pr-num* for the pr= label."
  [{:keys [container-name host image port extra-opts]}]
  (let [{:keys [traefik-network certresolver domain]} config]
    (apply docker! "run" "-d"
           "--name"    container-name
           "--network" traefik-network
           "--restart" "unless-stopped"
           "--label"   (str "pr=" *pr-num*)
           "--label"   "traefik.enable=true"
           "--label"   (str "traefik.docker.network=" traefik-network)
           "--label"   (str "traefik.http.routers." container-name ".entrypoints=websecure")
           "--label"   (str "traefik.http.routers." container-name ".rule=Host(`" host "`)")
           "--label"   (str "traefik.http.routers." container-name ".tls=true")
           "--label"   (str "traefik.http.routers." container-name ".tls.certresolver=" certresolver)
           "--label"   (str "traefik.http.routers." container-name ".tls.domains[0].main=*." domain)
           "--label"   (str "traefik.http.services." container-name ".loadbalancer.server.port=" port)
           (concat extra-opts [image]))))

(defn- deploy-service!
  "Pulls image, removes old container, then deploys via deploy-container!."
  [{:keys [image container-name] :as opts}]
  (log (str "Pulling " image " ..."))
  (docker! "pull" image)
  (log (str "Starting " container-name " ..."))
  ;; Use sh directly: docker rm -f returns exit 1 when the container
  ;; doesn't exist yet (first deploy). docker! would throw ex-info; we
  ;; intentionally ignore the exit code here.
  (sh ["sudo" "docker" "rm" "-f" container-name])
  (deploy-container! opts))

;; =============================================================================
;; DB clone (swtp_template -> swtp_pr_<n>)
;; =============================================================================
(defn- clone-db!
  "Clones swtp_template into a per-PR database (CREATE + GRANT + mysqldump | mysql).
   MySQL has no CREATE DATABASE ... LIKE-with-data, so we dump and restore.
   Uses raw sh instead of docker! so we can inspect exit codes and pipe via :in."
  [db-name db-app-user template-db db-root-pw]
  (let [my! (partial mysql! db-root-pw)]
    (log (str "Cloning DB (" template-db " -> " db-name ") ..."))
    (my! (str "CREATE DATABASE IF NOT EXISTS `" db-name "`;"))
    (my! (str "GRANT ALL PRIVILEGES ON `" db-name "`.* TO '" db-app-user "'@'%'; FLUSH PRIVILEGES;"))

    (let [dump (sh ["sudo" "docker" "exec" "-e" (str "MYSQL_PWD=" db-root-pw)
                    "swtp-db" "mysqldump" "-uroot"
                    "--set-gtid-purged=OFF" "--hex-blob" "--default-character-set=binary"
                    template-db]
                   {:out :bytes})]
      (when-not (zero? (:exit dump))
        (throw (ex-info (str "mysqldump FAILED: " (:err dump)) {:step "mysqldump"})))
      (let [restore (sh ["sudo" "docker" "exec" "-e" (str "MYSQL_PWD=" db-root-pw)
                         "-i" "swtp-db" "mysql" "-uroot"
                         "--default-character-set=binary" db-name]
                        {:in (:out dump)})]
        (when-not (zero? (:exit restore))
          (throw (ex-info (str "mysql restore FAILED: " (:err restore)) {:step "mysql restore"})))))

    (log "DB clone done")))

;; =============================================================================
;; Upload dir provisioning (uploads/template -> uploads/pr-<n>)
;; =============================================================================
(defn- provision-upload-dir!
  "Copies the upload template into a fresh per-PR upload directory."
  []
  (let [template "/opt/stacks/swtp-infra/uploads/template"
        dir      (str "/opt/stacks/swtp-infra/uploads/pr-" *pr-num*)]
    (log (str "Provisioning upload dir from template -> " dir " ..."))
    (let [{:keys [exit err]} (sh ["sudo" "cp" "-r" template dir])]
      (when-not (zero? exit)
        (throw (ex-info (str "Upload dir provisioning FAILED: " err) {:dir dir}))))
    (log "Upload dir provisioned")
    dir))

;; =============================================================================
;; Container deploy (concrete)
;; =============================================================================
(defn- deploy-web
  "Deploys the frontend container for this PR."
  [org]
  (let [host (subdomain *pr-num* nil)]
    (deploy-service!
      {:container-name (str "swtp-web-pr-" *pr-num*)
       :host           host
       :image          (str "ghcr.io/" org "/swtp-web:pr-" *pr-num*)
       :port           4000})
    (log (str "Frontend live -> https://" host))))

(defn- deploy-api
  "Deploys the backend container for this PR (review_net + review.env)."
  [org db-name]
  (let [container-name (str "swtp-api-pr-" *pr-num*)
        host           (subdomain *pr-num* "api")
        upload-dir     (provision-upload-dir!)]
    (deploy-service!
      {:container-name container-name
       :host           host
       :image          (str "ghcr.io/" org "/swtp-api:pr-" *pr-num*)
       :port           8080
       :extra-opts     ["--network"  "review_net"
                        "--env-file" "/opt/stacks/swtp-infra/review.env"
                        "-v"         (str upload-dir ":/app/uploads")
                        "-e"         "APP_UPLOADS_DIR=/app/uploads"
                        "-e"         (str "SPRING_DATASOURCE_URL=jdbc:mysql://swtp-db:3306/" db-name)
                        "-e"         "SPRING_MAIL_HOST=maildev"
                        "-e"         "SPRING_MAIL_PORT=1025"]})
    (log (str "Backend live -> https://" host))))

(defn- deploy-dozzle
  "Deploys a per-PR Dozzle log viewer, filtered to this PR's containers."
  []
  (let [host (subdomain *pr-num* "logs")]
    (deploy-service!
      {:container-name (str "swtp-logs-pr-" *pr-num*)
       :host           host
       :image          "amir20/dozzle:latest"
       :port           8080
       :extra-opts     ["-v" "/var/run/docker.sock:/var/run/docker.sock:ro"
                        "-e" (str "DOZZLE_FILTER=label=pr=" *pr-num*)]})
    (log (str "Dozzle live -> https://" host))))

;; =============================================================================
;; Keycloak: PR-Redirect-URI + Web-Origin
;; =============================================================================
;; Keycloak does not support subdomain wildcards, so each PR must be
;; registered individually via Admin REST API.
(defn- kc-get-admin-token
  "Obtains a Keycloak admin access token via password grant."
  [kc-admin kc-admin-password]
  (let [{:keys [kc-base]} config
        resp (http/post (str kc-base "/realms/master/protocol/openid-connect/token")
                        {:form-params {"client_id"  "admin-cli"
                                       "grant_type" "password"
                                       "username"   kc-admin
                                       "password"   kc-admin-password}
                         :throw       false})]
    (assert-status! resp "Keycloak token request" #{200})
    (-> (:body resp) json/parse-string (get "access_token"))))

(defn- kc-get-client
  "Looks up the swtp-frontend client by clientId; returns its representation.
   Throws if not found."
  [token]
  (let [{:keys [kc-base kc-realm kc-client-id]} config
        resp (http/get (str kc-base "/admin/realms/" kc-realm "/clients")
                       {:headers      {"Authorization" (str "Bearer " token)}
                        :query-params {"clientId" kc-client-id}
                        :throw        false})]
    (assert-status! resp "Keycloak client lookup" #{200})
    (let [clients (json/parse-string (:body resp))]
      (when (empty? clients)
        (throw (ex-info (str "Keycloak client " kc-client-id " not found in realm " kc-realm)
                        {:realm kc-realm :client-id kc-client-id})))
      (first clients))))

(defn- add-pr-redirect!
  "Registers this PR's redirect URI and web origin on the Keycloak client
   via Admin REST API."
  [kc-admin kc-admin-password kc-redirect-uri kc-web-origin]
  (log "Registering Keycloak redirect URI / web origin ...")
  (let [{:keys [kc-base kc-realm]} config
        token       (kc-get-admin-token kc-admin kc-admin-password)
        client      (kc-get-client token)
        client-uuid (get client "id")
        updated     (-> client
                        (update "redirectUris" #(vec (conj (set %) kc-redirect-uri)))
                        (update "webOrigins"   #(vec (conj (set %) kc-web-origin))))
        resp        (http/put (str kc-base "/admin/realms/" kc-realm "/clients/" client-uuid)
                              {:headers {"Authorization" (str "Bearer " token)
                                         "Content-Type"  "application/json"}
                               :body    (json/generate-string updated)
                               :throw   false})]
    (assert-status! resp "Keycloak client update" #{200 204})
    (log (str "Keycloak: registered " kc-redirect-uri " / " kc-web-origin))))

;; =============================================================================
;; active-prs.txt -- PR-Tracking for Backup-Restart
;; =============================================================================
;; The backup script stops (Step 02) all running containers but only
;; restarts (Step 06b) those listed here. Without an entry, a review app
;; stays down after a backup cycle.
(defn- register-active-pr!
  "Adds this PR to active-prs.txt so the backup restart script knows to
   bring it back up after a backup cycle."
  []
  (let [{:keys [active-prs-file]} config
        entry    (str "pr-" *pr-num*)
        existing (if (.exists (java.io.File. active-prs-file))
                   (->> (slurp active-prs-file) str/split-lines
                        (remove str/blank?) set)
                   #{})]
    (if (contains? existing entry)
      (log "active-prs.txt: entry already present")
      (do
        (spit active-prs-file (str entry "\n") :append true)
        (log "active-prs.txt: entry added")))))

;; =============================================================================
;; Dashboard
;; =============================================================================
(defn- update-dashboard!
  "Deep-merges an entry into the dashboard JSON at the given path,
   adding an updated_at timestamp."
  [dashboard-file path entry]
  (let [data    (if (.exists (java.io.File. dashboard-file))
                  (json/parse-string (slurp dashboard-file))
                  {})
        updated (assoc-in data (mapv str path)
                          (assoc entry "updated_at" (now-str)))]
    (spit dashboard-file (json/generate-string updated {:pretty true}))))

(defn- fetch-pr-title
  "Fetches the PR title from the public GitHub API. Returns nil on failure."
  [org]
  (try
    (let [resp (http/get (str "https://api.github.com/repos/" org "/thm-swtp/pulls/" *pr-num*)
                         {:headers {"Accept"     "application/vnd.github+json"
                                    "User-Agent" "swtp-review-deploy"}
                          :throw false})]
      (when (= 200 (:status resp))
        (-> (:body resp) json/parse-string (get "title"))))
    (catch Exception _ nil)))

(defn- register-dashboard!
  "Writes this PR's frontend, backend, logs, and GitHub URLs into the
   dashboard data file."
  [org]
  (let [{:keys [dashboard-file]} config
        pr-title (fetch-pr-title org)]
    (update-dashboard!
        dashboard-file
        ["prs" *pr-num*]
        (cond-> {"fe"     (str "https://" (subdomain *pr-num* nil))
                 "be"     (str "https://" (subdomain *pr-num* "api") "/swagger-ui/index.html")
                 "logs"   (str "https://" (subdomain *pr-num* "logs"))
                 "pr_url" (str "https://github.com/" org "/thm-swtp/pull/" *pr-num*)}
          pr-title (assoc "pr_title" pr-title))))
  (log "Dashboard updated"))

;; =============================================================================
;; Main
;; =============================================================================
(defn -main
  "Entry point. Parses args, wires up dependencies, runs the deploy pipeline.
   All helpers throw ex-info on failure."
  [& args]
  (try
    (let [{:keys [pr-num org]} (parse-args args)
          env          (load-env!)
          db-root-pw   (env-get env "MYSQL_ROOT_PASSWORD")
          db-app-user  (or (get env "MYSQL_USER") "swtp")
          db-name      (str "swtp_pr_" pr-num)
          template-db  "swtp_template"
          kc-admin     (env-get env "KC_ADMIN")
          kc-admin-pw  (env-get env "KC_ADMIN_PASSWORD")
          pr-origin    (str "https://" (subdomain pr-num nil))]

      (binding [*pr-num* pr-num]
        (log (str "Review deploy triggered (org: " org ")"))

        (clone-db! db-name db-app-user template-db db-root-pw)
        (deploy-web org)
        (deploy-api org db-name)
        (deploy-dozzle)
        (add-pr-redirect! kc-admin kc-admin-pw
                          (str pr-origin "/*") pr-origin)
        (register-active-pr!)
        (register-dashboard! org)

        (log "Review deploy complete")
        (log "----------------------------------------")))

    (catch Exception e
      (let [data (ex-data e)
            msg  (str "FATAL: " (ex-message e) (when data (str " " (pr-str data))))]
        (when-let [usage (:usage data)]
          (binding [*out* *err*]
            (println (str "Usage: " usage))))
        (try
          (log msg)
          (catch Exception _
            (binding [*out* *err*]
              (println msg))))
        (System/exit 1)))))

;; Boot Guard
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
