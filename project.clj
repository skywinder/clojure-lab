(defproject clojure-lab "0.0.1-SNAPSHOT"
  :description "Clojure Lab - A development environment for Clojure in Clojure."
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [net.cgrand/parsley "0.9.1"]
                 [popen "0.2.0"]
                 [leiningen-core "2.0.0"]
                 [swingrepl "1.3.0" :exclusions [org.clojure/clojure org.clojure/clojure-contrib]]]
  :main proto.main
  :manifest {"SplashScreen-Image" "logo.png"}
  :aliases  {"build" ["do" "clean," "uberjar"]
             "build-aot" ["with-profile" "aot" "do" "clean," "uberjar"]}
  :profiles {:aot {:aot [#".*"]}})
