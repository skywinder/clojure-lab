(defproject macho "1.0.0-SNAPSHOT"
  :description "macho (minimal advanced clojure hacking optimizer)"
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :aot [macho.ui.swing.UndoManager]
  :main macho.core)
