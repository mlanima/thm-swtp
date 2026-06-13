#!/usr/bin/env bb
;; /opt/stacks/<stack>/deploy.bb <stack-name>
;; Usage: deploy.bb swtp-dev   or   deploy.bb swtp-main
;;
;; Production deploy for a specific stack — pull images, restart services.

(require '[babashka.process :refer [sh exec]]
         '[clojure.string :as str])

(def args (filter #(not (str/blank? %)) *command-line-args*))
(def stack (first args))

(when (nil? stack)
  (binding [*out* *err*]
    (println "Usage: deploy.bb <stack-name>"))
  (System/exit 1))

(def stack-dir (str "/opt/stacks/" stack))
(def logfile (str stack-dir "/deploy.log"))

(defn now-str []
  (-> (sh "date" "+%Y-%m-%d %H:%M:%S") :out str/trim))

(defn log [msg]
  (spit logfile (str "[" (now-str) "] " msg "\n") :append true))

(log (str "Deploy triggered (stack: " stack ")"))

;; Discover services from compose file
(let [{:keys [out exit]} (sh "sudo" "docker" "compose" "config" "--services" {:dir stack-dir})]
  (when (= 0 exit)
    (let [services (str/split-lines (str/trim out))
          {:keys [out pull-out]} (sh "sudo" "docker" "compose" "pull" {:dir stack-dir})]
      (doseq [svc services]
        (cond
          (str/includes? pull-out (str svc " Pulled"))
          (log (str svc ": updated"))

          (str/includes? pull-out (str svc " up to date"))
          (log (str svc ": up-to-date"))

          :else
          (log (str svc ": check output")))))))

(log "Restarting services")
(exec "sudo" "docker" "compose" "up" "-d" {:dir stack-dir})
