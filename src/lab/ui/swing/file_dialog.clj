(ns lab.ui.swing.file-dialog
  (:import  [javax.swing JFileChooser])
  (:use     [lab.ui.protocols :only [Visible abstract]])
  (:require [lab.ui.core :as ui]))

(ui/definitializations
  :file-dialog JFileChooser)

(extend-protocol Visible
  javax.swing.JFileChooser
  (visible? [this] (.isVisible this))
  (hide [this] (.setVisible this false))
  (show [this]
    (let [c           (abstract this)
          dialog-type (ui/get-attr c :-type)
          result      (case dialog-type
                        :open   (.showOpenDialog this nil)
                        :save   (.showSaveDialog this nil)
                        :custom (.showSaveDialog this nil (ui/get-attr c :accept-label)))
          chosen      (.getSelectedFile this)]
      (condp = result
        nil [:invalid-type nil]
        JFileChooser/CANCEL_OPTION   [:cancel chosen]
        JFileChooser/APPROVE_OPTION  [:accept chosen]
        JFileChooser/ERROR_OPTION    [:error  chosen]))))
