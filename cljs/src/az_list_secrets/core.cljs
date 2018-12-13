(ns az-list-secrets.core
  (:require [clojure.string :as str]
            [clojure.pprint :refer [pprint]]))

(def child-process (js/require "child_process"))
(def on-exit (js/require "signal-exit"))

(defn -spawn
  "Thin wrapper around NodeJS function.
   Spawns the process in the background. Returns the created ChildProcess instance."
  [cmd args options]
  (let [name (str cmd " " (str/join " " args))]
    (when (:print-name options)
      (println "ᐅ" name))
    (-> (.spawn child-process
                cmd
                (clj->js (sequence args))
                (clj->js (merge {:stdio "pipe" :shell true} options)))

        (.on "error" (fn [err] (throw (ex-info (str "Error executing a process: " err) {}))))

        (.on "exit"
             (fn [code signal]
               (when (not (= code 0))
                 (throw (ex-info (str "Process " (pr-str name) " exited unsuccessfully. "
                                      "Code = " (pr-str code)
                                      ", signal = " (pr-str signal) ".") {}))))))))

(defn spawn
  "Spawns the process in the background.
   Will not make app wait for process to exit.
   Will kill the process if parent application exits.
   Returns the created ChildProcess instance."
  [cmd args options]
  (let [process (-spawn cmd args options)]
    (.unref process)
    (on-exit (fn [_code _signal] (.kill process)))
    process))

(defn spawn-sync
  "Spawns the process synchronously."
  [cmd args options]
  (let [name (str cmd " " (str/join " " args))]
    (when (:print-name options)
      (println "ᐅ" name))
    (let [result (.spawnSync child-process
                             cmd
                             (clj->js (sequence args))
                             (clj->js (merge {:stdio "pipe" :shell true} options)))]
      (when (some? (.-error result))
        (throw (ex-info (str "Error executing a process: " (.-error result)) {})))

      (when (not (= (.-status result) 0))
        (throw (ex-info (str "Process " (pr-str name) " exited with error code. Code = " (.-status result) ".") {})))
      (-> (js->clj result :keywordize-keys true)
          (update :output #(into [] (map str %)))
          (update :stdout str)
          (update :stderr str)))))

(defn json-exec [cmd args]
  (let [out (:stdout (spawn-sync cmd args {}))]
    (js->clj (.parse js/JSON out) :keywordize-keys true)))

(defn list-secrets [vault-name]
  (json-exec "az" ["keyvault" "secret" "list" "--vault-name" vault-name]))

(defn show-secret [vault-name secret-id]
  (json-exec "az" ["keyvault" "secret" "show" "--vault-name" vault-name "--id" secret-id]))

(defn -main [vault-name]
  (println (str "Connecting to Azure Key Vault: " vault-name))
  (let [listing (list-secrets vault-name)]
    (print "\nSecrets:\n\n")
    (doseq [entry listing]
      (let [secret-id (:id entry)
            secret (show-secret vault-name secret-id)
            secret-name (last (str/split secret-id "/"))]
        (print (str "Name  | " secret-name "\n"
                    "Value | " (:value secret) "\n\n"))))))
