#!/usr/bin/env bb
;; /opt/stacks/swtp/dispatch.bb
;;
;; Forced command for the deploy SSH key in authorized_keys:
;;   command="/opt/stacks/swtp/dispatch.bb" ssh-ed25519 ...
;;
;; Dispatches based on SSH_ORIGINAL_COMMAND:
;;   (no command)                     → deploy-dev (default)
;;   deploy-dev                       → deploy swtp-dev stack
;;   deploy-main                      → deploy swtp-main stack
;;   review-deploy <namespace> <pr>   → spin up review environment
;;   review-teardown <namespace> <pr> → tear down review environment

(require '[babashka.process :refer [exec]]
         '[clojure.string :as str])

(def cmd (System/getenv "SSH_ORIGINAL_COMMAND"))

(defn dispatch [s]
  (cond
    (or (nil? s) (= s "") (= s "deploy-dev"))
    (exec "/opt/stacks/swtp/deploy.bb" "swtp-dev")

    (= s "deploy-main")
    (exec "/opt/stacks/swtp/deploy.bb" "swtp-main")

    (str/starts-with? s "review-deploy ")
    (apply exec "/opt/stacks/swtp/review-deploy.bb"
           (str/split (subs s (count "review-deploy ")) #" "))

    (str/starts-with? s "review-teardown ")
    (apply exec "/opt/stacks/swtp/review-teardown.bb"
           (str/split (subs s (count "review-teardown ")) #" "))

    :else
    (do
      (binding [*out* *err*]
        (println "Unknown command:" s))
      (System/exit 1))))

(dispatch cmd)
