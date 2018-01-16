(set-env! :resource-paths #{"src/clj"}
          :source-paths   #{"test/clj"}
          :dependencies   '[[org.clojure/clojure "1.9.0" :scope "provided"]

                            [adzerk/bootlaces  "0.1.13" :scope "test"]
                            [adzerk/boot-test  "1.2.0"  :scope "test"]])

(require '[adzerk.boot-test :refer [test]]
         '[adzerk.bootlaces :refer :all]
         '[boot.git         :refer [last-commit]])

(def project 'oconn/fsm)
(def +version+ "0.0.1-SNAPSHOT")

(task-options!
 pom {:project     project
      :version     +version+
      :description "Clojure library for working with and visualizing finite-state-machines"
      :url         "https://github.com/oconn/fsm"
      :scm         {:url "https://github.com/oconn/fsm"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}}
 push {:repo "deploy-clojars"
       :ensure-clean true
       :ensure-tag (last-commit)
       :ensure-version +version+})

(bootlaces! +version+ :dont-modify-paths? true)

(deftask install-local
  "Build and install the project locally."
  []
  (comp (pom)
        (jar)
        (install)))

(deftask dev
  "Watches for changes and then installs locally"
  []
  (comp (watch)
        (install-local)))

(deftask deploy-snapshot
  "Deploys a new build to clojars"
  []
  (comp (build-jar)
        (push-snapshot)))

(deftask deploy-release
  "Deploys a release build to clojars"
  []
  (comp (build-jar)
        (push-release)))
