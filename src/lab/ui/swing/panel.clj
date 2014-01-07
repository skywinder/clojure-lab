(ns lab.ui.swing.panel
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util])
  (:use     [lab.ui.protocols :only [Component impl]])
  (:import [javax.swing JPanel JSplitPane JScrollPane JButton]
           [java.awt BorderLayout]))

(ui/definitializations
  :split       JSplitPane
  :panel       JPanel
  :scroll      JScrollPane)

(ui/defattributes
  :split
    (:resize-weight [c _ v]
      (.setResizeWeight ^JSplitPane (impl c) v))
    (:divider-location [c _ value]
      (.setDividerLocation ^JSplitPane (impl c) value))
    (:orientation [c attr value]
      (.setOrientation ^JSplitPane (impl c) (util/split-orientations value)))
  :scroll
    (:vertical-increment [c _ v]
      (.. (impl c) (getVerticalScrollBar) (setUnitIncrement 16)))
    (:margin-control [c _ v]
      (.setRowHeaderView (impl c) (impl v))))

(extend-protocol Component
  JSplitPane
  (add [this child]
    ; Assume that if the top component is a button then 
    ; it is because it was never set
    (if (instance? JButton (.getTopComponent this))
      (.setTopComponent this child)
      (.setBottomComponent this child))
    this)

  JScrollPane
  (add [this child]
    (.. this getViewport (add child nil))
    this))
