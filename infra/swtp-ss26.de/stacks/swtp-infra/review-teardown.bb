#!/usr/bin/env bb
;; /opt/stacks/swtp-infra/review-teardown.bb <namespace> <pr-number>
;;
;; Removes containers and images for a PR review environment.
;; Called automatically when a PR is closed or merged.

(require '[babashka.process :refer [sh]]
         '[clojure.string :as str])

;; Parse args
(def args (filter #(not (str/blank? %)) *command-line-args*))
(def namespace (first args))
(def pr-num (second args))
(def registry (str "ghcr.io/" namespace))
(def logfile "/opt/stacks/swtp-infra/deploy.log")
(def script-dir (-> (sh "dirname" (System/getProperty "babashka.file")) :out str/trim))

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

(log "Teardown complete")
(spit logfile "----------------------------------------\n" :append true)
