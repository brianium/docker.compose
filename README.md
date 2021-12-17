# docker.compose

[![Clojars Project](https://img.shields.io/clojars/v/com.github.brianium/docker.compose.svg)](https://clojars.org/com.github.brianium/docker.compose)


> I just want to run docker compose from the repl y'all

Just a simple utility for running (and waiting for the availability of) services
via [docker compose](https://docs.docker.com/compose/) via the REPL.

## Rationale

`docker compose` is a useful tool for running infrastructure services used during development - something like a database. I don't want to leave the REPL to start or stop these services. It is also handy to include containers as dependencies of a system built with something like [integrant](https://github.com/weavejester/integrant).

## Usage

**compose-up and compose-down**

The simplest functions just run `docker compose up -d` and `docker compose down` for us:

```clojure
(ns dev
  (:require [docker.compose :as docker]))

(docker/compose-up) ;;; use the docker-compose.yml in the same dir as deps.edn

(docker/compose-down) ;;; tear it all down
```

**wait-for-container and wait-for-port**

These functions are useful for making sure a service is ready. It can also allow you to effectively
specify running services from docker as dependencies in a system created by something like integrant:

```clojure
(defmethod ig/init-key ::containers [_ _]
  (info "Starting containers...")
  (docker/compose-up)
  (when-not (and (docker/wait-for-container "my_database") (docker/wait-for-port 3360))
    (throw (ex-info "Failed to wait for container" {:container "my_database"}))))
```

Here we are waiting for both the container AND port in order to have a degree of confidence
that our database is ready for use.

`wait-for-container` and `wait-for-port` support optional arguments to control the interval at which they are checked and the timeout desired.
