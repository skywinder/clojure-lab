(ns lab.ui.swing.panel
  (:use     [lab.ui.protocols :only [Component impl]])
  (:require [lab.ui.core :as ui]
            [lab.ui.util :refer [defattributes definitializations]]
            [lab.ui.swing.util :as util])
  (:import [javax.swing JPanel JSplitPane JScrollPane JButton JComponent]
           [java.awt BorderLayout]
           [javax.swing.plaf.basic BasicSplitPaneDivider BasicSplitPaneUI]))

(util/set-prop "scrollbar" (util/color 0xCC0000))
(util/set-prop "ScrollBar.background" (util/color 0xCCCCCC))
(util/set-prop "ScrollBar.darkShadow" (util/color 0xCCCCCC))
(util/set-prop "ScrollBar.foreground" (util/color 0xCCCCCC))
(util/set-prop "ScrollBar.highlight" (util/color 0xCCCCCC))
(util/set-prop "ScrollBar.shadow" (util/color 0xCCCCCC))

(util/set-prop "ScrollBar.track" (util/color 0xCCCCCC))
(util/set-prop "ScrollBar.trackForeground" (util/color 0xCCCCCC))
(util/set-prop "ScrollBar.trackHighlight" (util/color 0xCCCCCC))
(util/set-prop "ScrollBar.trackHighlightForeground" (util/color 0xCCCCCC))

(util/set-prop "ScrollBar.thumb" (util/color 0xCCCCCC))
(util/set-prop "ScrollBar.thumbDarkShadow" (util/color 0xCCCCCC))
(util/set-prop "ScrollBar.thumbHighlight" (util/color 0xCCCCCC))
(util/set-prop "ScrollBar.thumbShadow" (util/color 0xCCCCCC))

(util/set-prop "ScrollBar.width" (int 15))
(util/set-prop "ScrollBar.height" (int 15))

(extend-protocol Component
  JSplitPane
  (add [this child]
    ; Assume that if the top component is a button then 
    ; it is because it was never set
    (if (instance? JButton (.getTopComponent this))
      (.setTopComponent this child)
      (.setBottomComponent this child))
    (util/remove-focus-traversal child)
    this)

  JScrollPane
  (add [this child]
    (.. this getViewport (add ^java.awt.Container child nil))
    (util/remove-focus-traversal child)
    this)
  (remove [this child]
    (.. this getViewport (remove ^java.awt.Container child))
    this))

(defn- find-divider [^JSplitPane split]
  (->> split 
    .getComponents 
    (filter (partial instance? BasicSplitPaneDivider))
    first))

(defn- init-split
  "Create the split pane and replace the UI implementation for a
  bare one, so that the divider and the rest of its properties can be
  set, regardless of the Look & Feel used."
  [c]
  (doto (JSplitPane.)
    (.setUI (BasicSplitPaneUI.))))

(definitializations
  :split       init-split
  :panel       JPanel
  :scroll      JScrollPane)

(defattributes
  :split
    (:divider-background [c _ v]
      (.setBackground ^BasicSplitPaneDivider (find-divider (impl c)) (util/color v)))
    (:border [c _ v]
      (let [v       (if (sequential? v) v [v])
            border  (apply util/border v)
            split   ^JSplitPane (impl c)
            divider ^BasicSplitPaneDivider (find-divider split)]
        (.setBorder split border)
        (.setBorder divider border)))
    (:resize-weight [c _ v]
      (.setResizeWeight ^JSplitPane (impl c) v))
    (:divider-location [c _ v]
      (if (integer? v)
        (.setDividerLocation ^JSplitPane (impl c) ^int v)
        (.setDividerLocation ^JSplitPane (impl c) ^float v)))
    (:divider-location-right [c _ v]
      (let [split   ^JSplitPane (impl c)
            orientation (.getOrientation split)
            size    (if (= orientation (util/split-orientations :horizontal))
                       (.getWidth split)
                       (.getHeight split))]
        (if (float? v)
          (.setDividerLocation split (float (- 1 (/ v size))))
          (ui/attr c :divider-location (- size v)))))
    (:divider-size [c _ v]
      (.setDividerSize ^JSplitPane (impl c) v))
    (:orientation [c _ v]
      (.setOrientation ^JSplitPane (impl c) (util/split-orientations v)))
  :scroll
    (:layout [c _ v]
      (throw (ex-info "Can't change the layout of a :scroll component:" {:layout v})))
    (:vertical-increment [c _ v]
      (.. ^JScrollPane (impl c) getVerticalScrollBar (setUnitIncrement 16)))
    (:margin-control [c _ v]
      (.setRowHeaderView ^JScrollPane (impl c) (impl v))))
