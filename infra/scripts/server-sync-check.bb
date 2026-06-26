#!/usr/bin/env bb
;; Checks which infra stack files on the server differ from the repo.
;; Repo dir `infra/swtp-ss26.de/stacks/` mirrors `/opt/stacks` on the server
;; (see infra/CLAUDE.md). Lists files that need updating (repo -> server).
;; Read-only: no writes, no sync — just the diff.
;;
;; Run from anywhere inside the repo:
;;   bb infra/scripts/server-sync-check.bb
;; Override SSH target via env if needed:
;;   SYNC_SSH_HOST=user@host SYNC_SSH_PORT=2105 bb ...

(require '[babashka.process :refer [sh]])
(require '[clojure.string :as str])

(def repo-root  (-> (sh ["git" "rev-parse" "--show-toplevel"]) :out str/trim))
(def remote-dir "/opt/stacks")
(def ssh-host   (or (System/getenv "SYNC_SSH_HOST") "tosch@mail.schalz.de"))
(def ssh-port   (or (System/getenv "SYNC_SSH_PORT") "2105"))

(def excludes #{"swtp-infra/dashboard/data.json"})

(defn rel [repo-path] (str/replace-first repo-path #"^infra/swtp-ss26\.de/stacks/" ""))

;; only git-tracked files under the stacks dir (no untracked / build noise),
;; minus runtime-written files that always differ and aren't sync targets.
;; `git -C repo-root` so the pathspec resolves from the repo root regardless
;; of the caller's cwd (running ./server-sync-check.bb from infra/scripts/
;; would otherwise yield an empty list and make shasum read stdin → hang).
(def tracked
  (->> (sh ["git" "-C" repo-root "ls-files" "infra/swtp-ss26.de/stacks/"])
       :out str/split-lines (remove str/blank?)
       (remove #(excludes (rel %)))))

(when (empty? tracked)
  (binding [*out* *err*]
    (println "no tracked infra stack files found under infra/swtp-ss26.de/stacks/"))
  (System/exit 1))

;; one shasum call for all local files
(defn parse-hashes [out strip-prefix]
  (into {}
    (for [line (str/split-lines out)
          :let [[hash path] (str/split line #"\s+" 2)]
          :when path]
      [(-> path str/trim (str/replace-first strip-prefix "")) hash])))

(def local
  (parse-hashes
    (:out (apply sh (into ["shasum" "-a" "256"]
                            (map #(str repo-root "/" %) tracked))))
    (re-pattern (str "^" repo-root "/infra/swtp-ss26.de/stacks/"))))

(def ssh-key (System/getenv "SYNC_SSH_KEY"))
(def ssh-args
  (into ["ssh" "-o" "BatchMode=yes" "-o" "StrictHostKeyChecking=no" "-p" ssh-port]
        (when-let [k ssh-key] ["-i" k])))

(def remote-paths (map #(str remote-dir "/" %) (keys local)))
(def remote-cmd   (str "sha256sum " (str/join " " (map #(str "'" % "'") remote-paths))))

;; one ssh round-trip; BatchMode=yes fails fast instead of hanging on a password/hostkey prompt.
;; timeout as a safety net.
;; NOTE: use a normal admin key with a real shell (your default/agent key),
;; NOT the deploy_key — that is server-pinned to dispatch.bb (forced command)
;; and can only run whitelisted deploy commands, never an arbitrary sha256sum.
(printf "== infra stack sync check (repo -> server) ==\nchecking %d files via ssh...\n" (count local))
(printf "ssh: %s %s <sha256sum ...>\n" (str/join " " ssh-args) ssh-host)
(flush)
(def ssh-result (sh (conj ssh-args ssh-host remote-cmd) {:err :string :timeout 30000}))
(when-not (str/blank? (:err ssh-result))
  (binding [*out* *err*] (printf "ssh stderr:\n%s\n" (:err ssh-result))))
(def remote
  (parse-hashes (:out ssh-result) (re-pattern (str "^" remote-dir "/"))))

(let [differs (atom 0) missing (atom 0) ok (atom 0)]
  (doseq [r (sort (keys local))]
    (cond
      (not (contains? remote r)) (do (printf "  MISSING   %s\n" r) (swap! missing inc))
      (not= (remote r) (local r)) (do (printf "  DIFFERS   %s\n" r) (swap! differs inc))
      :else                       (do (printf "  MATCHES   %s\n" r) (swap! ok inc))))
  (println "--")
  (printf "ok=%d differs=%d missing=%d  (total %d)\n"
          @ok @differs @missing (count local))
  (if (zero? (+ @differs @missing))
    (println "all in sync")
    (printf "needs update: %d file(s)\n" (+ @differs @missing))))

(when-not (zero? (:exit ssh-result))
  (binding [*out* *err*]
    (println "(ssh exited non-zero — some paths may be missing on the server)")))