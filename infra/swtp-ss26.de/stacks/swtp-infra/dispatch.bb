#!/usr/bin/env bb
;; /opt/stacks/swtp-infra/dispatch.clj
;;
;; Forced command for the deploy SSH key in authorized_keys:
;;   command="/opt/stacks/swtp-infra/dispatch.clj" ssh-ed25519 ...
;;
;; Routes on SSH_ORIGINAL_COMMAND:
;;   (no command)                     -> deploy-dev (default)
;;   deploy-dev                       -> deploy swtp-dev stack
;;   deploy-main                      -> deploy swtp-main stack
;;   review-deploy <namespace> <pr>   -> spin up review environment
;;   review-teardown <namespace> <pr> -> tear down review environment

(require '[babashka.process :refer [exec]]
         '[clojure.string   :as    str])

;; =============================================================================
;; Config
;; =============================================================================
(def stacks-dir "/opt/stacks")
(def infra-dir  (str stacks-dir "/swtp-infra"))
(def logfile    (str infra-dir "/deploy.log"))   ; shared with review-deploy/-teardown

;; =============================================================================
;; Audit logging
;; =============================================================================
(def date-fmt
  "DateTimeFormatter for log timestamps: yyyy-MM-dd HH:mm:ss"
  (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))

(defn- now-str
  "Returns current timestamp as yyyy-MM-dd HH:mm:ss string."
  []
  (.format (java.time.LocalDateTime/now) date-fmt))

(defn- log
  "Best-effort timestamped audit line to the shared deploy log. Never throws:
   a failing audit write must not block a dispatch."
  [msg]
  (try
    (spit logfile (str "[" (now-str) "] [dispatch] " msg "\n") :append true)
    (catch Exception _ nil)))

;; =============================================================================
;; Argument handling
;; =============================================================================
(defn- tokenize
  "Splits an SSH command string into non-blank whitespace-separated tokens."
  [s]
  (->> (str/split (str/trim (or s "")) #"\s+")
       (remove str/blank?)))

(defn- review-args
  "Validates positional `<namespace> <pr>` and translates them into the flag
   form the review-*.clj scripts expect. Throws ex-info on bad shape."
  [subcommand tokens]
  (let [[org pr & extra] tokens]
    (when-not (and org (re-matches #"[A-Za-z0-9._-]+" org)
                   pr  (re-matches #"\d+" pr)
                   (empty? extra))
      (throw (ex-info (str "usage: " subcommand " <namespace> <pr-number>")
                      {:subcommand subcommand :tokens tokens})))
    ["--namespace" org "--pr" pr]))

;; =============================================================================
;; Routing
;; =============================================================================
(defn- route
  "Maps an SSH_ORIGINAL_COMMAND string to a [program & args] vector.
   Throws ex-info if the command is not in the allowlist."
  [cmd]
  (let [[head & rest] (tokenize cmd)]
    (case head
      (nil "deploy-dev") [(str stacks-dir "/swtp-dev/deploy.bb")]
      "deploy-main"      [(str stacks-dir "/swtp-main/deploy.bb")]
      "review-deploy"    (into [(str infra-dir "/review-deploy.clj")]
                               (review-args "review-deploy" rest))
      "review-teardown"  (into [(str infra-dir "/review-teardown.clj")]
                               (review-args "review-teardown" rest))
      (throw (ex-info (str "Unknown command: " (pr-str cmd)) {:cmd cmd})))))

;; =============================================================================
;; Main
;; =============================================================================
(defn- -main
  "Reads SSH_ORIGINAL_COMMAND, resolves it against the allowlist, and execs the target."
  [& _]
  (let [cmd (System/getenv "SSH_ORIGINAL_COMMAND")]
    (try
      (let [[program & args] (route cmd)]
        (log (str "dispatch: " (pr-str cmd) " -> " program
                  (when (seq args) (str " " (str/join " " args)))))
        (apply exec program args))        ; replaces this process, never returns
      (catch Exception e
        (log (str "REJECTED: " (pr-str cmd) " -- " (ex-message e)))
        (binding [*out* *err*]
          (println (ex-message e)))
        (System/exit 1)))))

;; Boot Guard
(when (= *file* (System/getProperty "babashka.file"))
  (-main))
