set dotenv-load := true
project := "arcone"


help:
  @just --list


# Initialize dev setup:
init: build-images init-npm init-clojure
  @echo "\n\nReady"


# Run PostCSS complile or watch
postcss command='compile':
  docker run --rm --init -it                                                      \
    --name arcone-postcss                                                         \
    -v $(pwd)/src/css:/workspace/src/css                                          \
    -v $(pwd)/public:/workspace/public                                            \
    metosin.arcone.dev/postcss:latest                                             \
    npm run postcss:{{ command }}


# Build Docker images
build-images:
  docker build --tag=metosin.arcone.dev/postcss:latest ./docker/postcss


# Init NPM dependencies
init-npm:
  npm i


# Init Clojure dependencies
init-clojure:
  clojure -A:server:web:dev:test -P
  

# Run CLJS tests
cljs-test:
  @npx shadow-cljs compile test


# Run CLJ tests
clj-test focus=':unit' +opts="":
  @clojure -M:dev:test                                         \
           -m kaocha.runner                                    \
           --reporter kaocha.report/dots                       \
           --focus {{ focus }}                                 \
           {{ opts }}
  

# Run all tests
test: clj-test cljs-test
  @echo "All tests run"


# Run clj tests and watch for changes
watch:
  clojure -X:dev:test:watch-test


# psql:
psql +args='':
  docker run                                                                      \
    --rm -it                                                                      \
    -v $(pwd)/.psqlrc:/root/.psqlrc                                               \
    --network {{ project }}_default                                               \
    -e PGPASSWORD=${POSTGRES_PASSWORD}                                            \
    -e PGOPTIONS=--search_path=rockstore,api,priv                                 \
    postgres:16-bookworm                                                          \
    psql -h db                                                                    \
         -p 5432                                                                  \
         -U postgres                                                              \
         -d musicbrainz                                                           \
         {{ args }}


# Check for outdated deps
outdated:
  @clj -M:outdated 2>&1 | grep -v "WARNING:"


# Make a release, creates a tag and pushes it
@release version +message:
  git diff --quiet || (echo "Working directory is dirty"; false)
  git tag -a {{ version }} -m "{{ message }}"
  git push --tags
  bash -c 'echo -n "SHA: "'
  git rev-parse --short {{ version }}^{commit}


@current-release:
  #!/usr/bin/env bash
  TAG=$(git tag --sort=-taggerdate | head -n 1)
  SHA=$(git rev-parse --short ${TAG}^{commit})
  echo ":git/tag \"${TAG}\""
  echo ":git/sha \"${SHA}\""
