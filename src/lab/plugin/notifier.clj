(ns lab.plugin.notifier
  (:require [lab.core.plugin :as plugin]
            [lab.ui.core :as ui]
            [lab.ui.templates :as tmplt]
            [clojure.repl :as repl]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; UI Error Handler

;; TODO: add link to files when displaying the stack trace.

(defn- show-error-info
  [app ex]
  (let [ui     (@app :ui)
        sw     (java.io.StringWriter.)
        _      (.printStackTrace ex (java.io.PrintWriter. sw))
        title  (str "Error - " (or (.getMessage ex) ex))
        tab    (-> app
                 (tmplt/tab title)
                 (ui/add [:text-area {:text (str sw) :read-only true :caret-position 0}]))]
    (ui/update! ui :#bottom-controls ui/add tab)))

(defn- default-error-handler
  [app]
  (let [handler (proxy [Thread$UncaughtExceptionHandler] []
                  (uncaughtException [thread ex]
                    (try (#'show-error-info app ex)
                    (catch Exception new-ex (println ex))))
                  (handle [ex]
                    (#'show-error-info app ex)))
       class-name (-> handler class .getName)]
    ;(System/setProperty "sun.awt.exception.handler" class-name)
    (Thread/setDefaultUncaughtExceptionHandler handler)))

(defn- init! [app]
  (default-error-handler app))

(plugin/defplugin lab.plugin.notifier
  :init! #'init!)