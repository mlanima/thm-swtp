#!/usr/bin/env bb
;; /opt/stacks/swtp-infra/review-teardown.clj --namespace <namespace> --pr <pr-number>
;;
;; Tears down a PR review environment. Called when a PR is closed or merged.
;;
;; Best-effort: every step is independent and wrapped so a failure in one
;; (Keycloak unreachable, DB already gone, ...) is logged and the rest still run.
;;
;; Steps:
;; - removes the per-PR web / api / dozzle containers
;; - removes the per-PR images to free disk space
;; - drops the per-PR database (swtp_pr_<n>)
;; - removes the PR's redirect URI + web origin from Keycloak
;; - removes the PR from active-prs.txt
;; - removes the PR entry from the dashboard JSON

(require '[babashka.cli         :as    cli]
         '[babashka.process     :refer [sh]]
         '[babashka.http-client :as    http]
         '[cheshire.core        :as    json]
         '[clojure.string       :as    str])

;; =============================================================================
;; Config
;; =============================================================================
(def config
  "Static configuration."
  {:domain          "review.swtp-ss26.de"
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
  (let [opts   (:opts (cli/parse-args args))
        org    (:namespace opts)
        pr-num (let [s (str (:pr opts))]
                 (when-not (re-matches #"\d+" s)
                   (throw (ex-info (str "--pr must be a number, got: " s)
                            {:usage "review-teardown.clj --namespace <namespace> --pr <pr-number>"})))
                 s)]
    (when (or (not (string? org)) (str/blank? org) (nil? pr-num))
      (throw (ex-info "Missing required args"
               {:usage "review-teardown.clj --namespace <namespace> --pr <pr-number>"})))
    {:org    org
     :pr-num pr-num}))

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
;; Helpers
;; =============================================================================
(defn- subdomain
  "Builds a PR-scoped hostname: pr-<n>[-<suffix>].<domain>
   Pass nil suffix for the bare frontend subdomain."
  [pr-num suffix]
  (let [domain (:domain config)]
    (str "pr-" pr-num (when suffix (str "-" suffix)) "." domain)))

(defn- assert-status!
  "Throws ex-info if HTTP response status is not in accepted (a set).
   context describes the operation for the error message."
  [resp context accepted]
  (when-not (accepted (:status resp))
    (throw (ex-info (str context " FAILED: " (:status resp) " " (:body resp))
                    {:response resp :context context}))))

(defn- attempt
  "Runs the thunk f, logging any exception as a non-fatal teardown failure and continuing."
  [label f]
  (try
    (f)
    (catch Exception e
      (log (str label " FAILED: " (ex-message e)
                (when-let [d (ex-data e)] (str " " (pr-str d))))))))

;; =============================================================================
;; Docker cleanup
;; =============================================================================
(defn- remove-container!
  "Force-removes a container, logging whether it existed.
   A non-zero exit just means it was already gone, which is fine."
  [name]
  (let [{:keys [exit]} (sh ["sudo" "docker" "rm" "-f" name])]
    (if (zero? exit)
      (log (str name " removed"))
      (log (str name " not found (already gone?)")))))

(defn- remove-containers!
  "Force-removes this PR's web, api and dozzle containers."
  []
  (doseq [name [(str "swtp-web-pr-"  *pr-num*)
                (str "swtp-api-pr-"  *pr-num*)
                (str "swtp-logs-pr-" *pr-num*)]]
    (remove-container! name)))

(defn- remove-images!
  "Best-effort removal of this PR's images to free disk space."
  [org]
  (doseq [image [(str "ghcr.io/" org "/swtp-web:pr-" *pr-num*)
                 (str "ghcr.io/" org "/swtp-api:pr-" *pr-num*)]]
    (sh ["sudo" "docker" "rmi" image])
    (log (str "Image cleanup attempted: " image))))

;; =============================================================================
;; DB drop (swtp_pr_<n>)
;; =============================================================================
(defn- drop-db!
  "Drops the per-PR database. Uses MYSQL_PWD so the password never appears
   in ps aux. Throws ex-info on failure (caught by the surrounding attempt)."
  [db-name db-root-pw]
  (let [{:keys [exit err]} (sh ["sudo" "docker" "exec"
                                "-e" (str "MYSQL_PWD=" db-root-pw)
                                "-i" "swtp-db" "mysql" "-uroot"
                                "-e" (str "DROP DATABASE IF EXISTS `" db-name "`;")])]
    (if (zero? exit)
      (log (str "DB dropped (" db-name ")"))
      (throw (ex-info (str "DB drop FAILED: " err) {:db db-name})))))

;; =============================================================================
;; Keycloak: remove PR-Redirect-URI + Web-Origin
;; =============================================================================
;; Keycloak does not support subdomain wildcards, so each PR is registered (and
;; here de-registered) individually via the Admin REST API.
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
  "Looks up the swtp-frontend client by clientId; returns its representation. Throws if not found."
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

(defn- remove-pr-redirect!
  "Removes this PR's redirect URI and web origin from the Keycloak client."
  [kc-admin kc-admin-password kc-redirect-uri kc-web-origin]
  (log "Removing Keycloak redirect URI / web origin ...")
  (let [{:keys [kc-base kc-realm]} config
        token       (kc-get-admin-token kc-admin kc-admin-password)
        client      (kc-get-client token)
        client-uuid (get client "id")
        updated     (-> client
                        (update "redirectUris" #(vec (disj (set %) kc-redirect-uri)))
                        (update "webOrigins"   #(vec (disj (set %) kc-web-origin))))
        resp        (http/put (str kc-base "/admin/realms/" kc-realm "/clients/" client-uuid)
                              {:headers {"Authorization" (str "Bearer " token)
                                         "Content-Type"  "application/json"}
                               :body    (json/generate-string updated)
                               :throw   false})]
    (assert-status! resp "Keycloak client update" #{200 204})
    (log (str "Keycloak: removed " kc-redirect-uri " / " kc-web-origin))))

;; =============================================================================
;; active-prs.txt -- remove PR from backup-restart tracking
;; =============================================================================
(defn- deregister-active-pr!
  "Removes this PR from active-prs.txt so the backup restart script no longer
   tries to bring it back up."
  []
  (let [{:keys [active-prs-file]} config
        entry (str "pr-" *pr-num*)]
    (if (.exists (java.io.File. active-prs-file))
      (let [remaining (->> (slurp active-prs-file) str/split-lines
                           (remove str/blank?)
                           (remove #(= % entry)))]
        (spit active-prs-file (str (str/join "\n" remaining) (when (seq remaining) "\n")))
        (log "active-prs.txt: entry removed"))
      (log "active-prs.txt: file not found, skipping"))))

;; =============================================================================
;; Dashboard
;; =============================================================================
(defn- deregister-dashboard!
  "Removes this PR's entry from the dashboard JSON."
  []
  (let [{:keys [dashboard-file]} config
        data    (if (.exists (java.io.File. dashboard-file))
                  (json/parse-string (slurp dashboard-file))
                  {})
        updated (update data "prs" dissoc *pr-num*)]
    (spit dashboard-file (json/generate-string updated {:pretty true}))
    (log "Dashboard: PR entry removed")))

;; =============================================================================
;; Main
;; =============================================================================
(defn -main
  "Entry point. Parses args, then runs each teardown step best-effort.
   .env is loaded lazily so container/image/file cleanup still runs even if
   .env is missing (only the DB and Keycloak steps need it)."
  [& args]
  (try
    (let [{:keys [pr-num org]} (parse-args args)
          env     (delay (load-env!))
          db-name (str "swtp_pr_" pr-num)
          origin  (str "https://" (subdomain pr-num nil))]

      (binding [*pr-num* pr-num]
        (log (str "Teardown triggered (org: " org ")"))

        (attempt "container removal"  remove-containers!)
        (attempt "image cleanup"      #(remove-images! org))
        (attempt "DB drop"            #(drop-db! db-name (env-get @env "MYSQL_ROOT_PASSWORD")))
        (attempt "Keycloak cleanup"   #(remove-pr-redirect!
                                         (env-get @env "KC_ADMIN")
                                         (env-get @env "KC_ADMIN_PASSWORD")
                                         (str origin "/*") origin))
        (attempt "active-prs cleanup" deregister-active-pr!)
        (attempt "dashboard cleanup"  deregister-dashboard!)

        (log "Teardown complete")
        (log "----------------------------------------")))

    (catch Exception e
      ;; Only reached for failures outside the per-step attempts (e.g. arg parsing).
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
