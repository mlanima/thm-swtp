#!/usr/bin/env bb
;; /opt/stacks/swtp-infra/review-teardown.bb <namespace> <pr-number>
;;
;; Removes containers and images for a PR review environment.
;; Called automatically when a PR is closed or merged.

(require '[babashka.process :refer [sh]]
         '[babashka.http-client :as http]
         '[cheshire.core :as json]
         '[clojure.string :as str])

;; Parse args
(def args (filter #(not (str/blank? %)) *command-line-args*))
(def namespace (first args))
(def pr-num (second args))
(def registry (str "ghcr.io/" namespace))
(def logfile "/opt/stacks/swtp-infra/deploy.log")
(def script-dir (-> (sh "dirname" (System/getProperty "babashka.file")) :out str/trim))
(def domain "review.swtp-ss26.de")
(def kc-base "https://auth.swtp-ss26.de")
(def kc-realm "swtp")
(def kc-client-id "swtp-frontend")

(defn now-str []
  (-> (sh "date" "+%Y-%m-%d %H:%M:%S") :out str/trim))

(defn log [msg]
  (spit logfile (str "[" (now-str) "] [PR-" pr-num "] " msg "\n") :append true))

(when (or (nil? namespace) (nil? pr-num))
  (binding [*out* *err*]
    (println "Usage: review-teardown.bb <namespace> <pr-number>"))
  (System/exit 1))

(log "Teardown triggered")

(let [fe-name (str "swtp-web-pr-" pr-num)
      be-name (str "swtp-api-pr-" pr-num)]

  ;; Remove containers
  (let [{:keys [exit]} (sh "sudo" "docker" "rm" "-f" fe-name)]
    (if (= 0 exit)
      (log "Frontend container removed")
      (log "Frontend container not found (already gone?)")))

  (let [{:keys [exit]} (sh "sudo" "docker" "rm" "-f" be-name)]
    (if (= 0 exit)
      (log "Backend container removed")
      (log "Backend container not found (already gone?)")))

  ;; Clean up PR-tagged images to free disk space
  (sh "sudo" "docker" "rmi" (str registry "/swtp-web:pr-" pr-num))
  (sh "sudo" "docker" "rmi" (str registry "/swtp-api:pr-" pr-num)))

;; Remove Dozzle container (shared image, not removed)
(let [{:keys [exit]} (sh "sudo" "docker" "rm" "-f" (str "swtp-logs-pr-" pr-num))]
  (if (= 0 exit)
    (log "Dozzle container removed")
    (log "Dozzle container not found (already gone?)")))

;; Drop PR database (swtp_pr_<n>)
(let [db-name (str "swtp_pr_" pr-num)
      env-file (str script-dir "/.env")
      read-env (fn [path key]
                 (->> (slurp path)
                      str/split-lines
                      (keep #(let [[k v] (str/split % #"=" 2)]
                               (when (= (some-> k str/trim) key) (some-> v str/trim))))
                      first))
      root-pw (read-env env-file "MYSQL_ROOT_PASSWORD")]
  (if (str/blank? root-pw)
    (log (str "DB drop FAILED: MYSQL_ROOT_PASSWORD not found in " env-file))
    (let [{:keys [exit err]} (sh "sudo" "docker" "exec" "-i" "swtp-db" "mysql" "-uroot" (str "-p" root-pw)
                                  "-e" (str "DROP DATABASE IF EXISTS `" db-name "`;"))]
      (if (zero? exit)
        (log "DB dropped")
        (log (str "DB drop FAILED: " err))))))

;; ── Keycloak: PR-Redirect-URI + Web-Origin entfernen ────────────────────────
(let [env-file (str script-dir "/.env")
      read-env (fn [path key]
                 (->> (slurp path)
                      str/split-lines
                      (keep #(let [[k v] (str/split % #"=" 2)]
                               (when (= (some-> k str/trim) key) (some-> v str/trim))))
                      first))
      kc-admin (read-env env-file "KC_ADMIN")
      kc-admin-password (read-env env-file "KC_ADMIN_PASSWORD")
      kc-redirect-uri (str "https://pr-" pr-num "." domain "/*")
      kc-web-origin (str "https://pr-" pr-num "." domain)]
  (if (or (str/blank? kc-admin) (str/blank? kc-admin-password))
    (log (str "Keycloak cleanup FAILED: KC_ADMIN / KC_ADMIN_PASSWORD not found in " env-file))
    (let [token-resp (http/post (str kc-base "/realms/master/protocol/openid-connect/token")
                                 {:form-params {"client_id"  "admin-cli"
                                                 "grant_type" "password"
                                                 "username"   kc-admin
                                                 "password"   kc-admin-password}
                                  :throw       false})]
      (if-not (= 200 (:status token-resp))
        (log (str "Keycloak token request FAILED: " (:status token-resp) " " (:body token-resp)))
        (let [token (-> (:body token-resp) json/parse-string (get "access_token"))
              client-resp (http/get (str kc-base "/admin/realms/" kc-realm "/clients")
                                     {:headers      {"Authorization" (str "Bearer " token)}
                                      :query-params {"clientId" kc-client-id}
                                      :throw        false})]
          (if-not (= 200 (:status client-resp))
            (log (str "Keycloak client lookup FAILED: " (:status client-resp) " " (:body client-resp)))
            (let [clients (json/parse-string (:body client-resp))]
              (if (empty? clients)
                (log (str "Keycloak client " kc-client-id " not found in realm " kc-realm))
                (let [client (first clients)
                      client-uuid (get client "id")
                      redirect-uris (disj (set (get client "redirectUris" [])) kc-redirect-uri)
                      web-origins (disj (set (get client "webOrigins" [])) kc-web-origin)
                      updated (assoc client
                                     "redirectUris" (vec redirect-uris)
                                     "webOrigins" (vec web-origins))
                      update-resp (http/put (str kc-base "/admin/realms/" kc-realm "/clients/" client-uuid)
                                             {:headers {"Authorization" (str "Bearer " token)
                                                        "Content-Type" "application/json"}
                                              :body    (json/generate-string updated)
                                              :throw   false})]
                  (if (#{200 204} (:status update-resp))
                    (log (str "Keycloak: removed " kc-redirect-uri " / " kc-web-origin))
                    (log (str "Keycloak client update FAILED: " (:status update-resp) " " (:body update-resp)))))))))))))

;; ── active-prs.txt: PR-Tracking für Backup-Restart entfernen ────────────────
(let [active-prs-file (str script-dir "/active-prs.txt")
      entry (str "pr-" pr-num)]
  (if (.exists (java.io.File. active-prs-file))
    (let [remaining (->> (slurp active-prs-file)
                          str/split-lines
                          (remove str/blank?)
                          (remove #(= % entry)))]
      (spit active-prs-file (str (str/join "\n" remaining) (when (seq remaining) "\n")))
      (log "active-prs.txt: entry removed"))
    (log "active-prs.txt: file not found, skipping")))

(log "Teardown complete")
(spit logfile "----------------------------------------\n" :append true)
