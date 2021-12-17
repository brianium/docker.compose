(ns docker.compose
  "A simple utility for using docker compose (ideally from the repl)."
  (:require [clojure.java.shell :as sh]
            [clojure.string :as string]
            [clojure.core.async :as async])
  (:import (java.net ServerSocket)))

(defn is-running?
  "Check if the named container is running - i.e it has a status of \"running\" provided
   by docker ps"
  [container-name]
  (-> (sh/sh "docker" "ps" "--filter" (str "name=" container-name) "--filter" "status=running")
      :out
      (string/index-of container-name)
      (some?)))

(defn is-port-bound?
  [port]
  (try
    (with-open [_ (ServerSocket. port)]
      false)
    (catch Exception _
      true)))

(defn compose-up []
  (sh/sh "docker" "compose" "up" "-d"))

(defn compose-down []
  (sh/sh "docker" "compose" "down"))

(defn wait-for
  "Wait for a given predicate to become true. Returns true if the
   predicate returns true, otherwise returns false when the timeout
   is reached"
  ([pred interval-ms timeout-ms wait-ms]
   (async/go-loop [true?    (pred)
                   interval (async/timeout interval-ms)
                   timeout  (async/timeout timeout-ms)]
     (if true?
       (do
         (async/<! (async/timeout wait-ms)) ;;; Arbitrary wait after verified condition
         true)
       (let [[_ p] (async/alts! [interval timeout])]
         (cond
           (= p timeout) false

           (= p interval)
           (recur (pred) (async/timeout interval-ms) timeout))))))
  ([pred interval-ms timeout-ms]
   (wait-for pred interval-ms timeout-ms 3000)))

(defn wait-for-container
  "Waits for the given container name to reach a state of \"running\""
  ([container-name interval-ms timeout-ms]
   (async/<!! (wait-for #(is-running? container-name) interval-ms timeout-ms)))
  ([container-name interval-ms]
   (wait-for-container container-name interval-ms (* 1000 60 3)))
  ([container-name]
   (wait-for-container container-name 1000)))

(defn wait-for-port
  "Waits for the given port to be bound"
  ([port interval-ms timeout-ms]
   (async/<!! (wait-for #(is-port-bound? port) interval-ms timeout-ms)))
  ([port interval-ms]
   (wait-for-port port interval-ms (* 1000 60 3)))
  ([port]
   (wait-for-port port 1000)))
