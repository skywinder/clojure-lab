(ns lab.ui.swing.core
  (:require [lab.ui.core :as ui]
            [lab.ui.swing.util :as util])
  (:import [javax.swing SwingUtilities UIManager]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; First define macro to perform ui actions
;; and alter the root binding of lab.ui.core/ui-action-macro.

(defmacro swing-action
  "Queues an action to the event queue."
  [& body]
  `(SwingUtilities/invokeLater
    (fn [] ~@body)))

(ui/register-action-macro! #'swing-action)

;;;;;;;;;;;;;;;;;;;;;;
;; Swing L&F

(UIManager/setLookAndFeel "javax.swing.plaf.metal.MetalLookAndFeel")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Then require components implementations

(ns lab.ui.swing
  (:refer-clojure :exclude [remove])
  (:require [lab.ui.protocols :as p]
            [lab.ui.core :as ui]
            [lab.ui.util :refer [defattributes definitializations]]
            [lab.ui.swing [util :as util]
                          window
                          panel
                          dialog
                          tree
                          menu
                          text
                          tab
                          misc-control
                          event
                          console
			  toolbar])
  (:import [javax.swing JComponent AbstractAction SwingUtilities]
           javax.swing.border.CompoundBorder
           java.awt.Component))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Register fonts

(util/register-font "fonts/Inconsolata.otf")
(util/register-font "fonts/ABeeZee-Regular.otf")

;;;;;;;;;;;;;;;;;;;;;;
;; UI protocols implementation
  
(extend-protocol p/Implementation
  JComponent
  (abstract 
    ([this]
      (.getClientProperty this :abstract))
    ([this the-abstract] 
      (.putClientProperty this :abstract the-abstract)
      this)))

(extend-protocol p/Component
  java.awt.Container
  (children [this]
    (.getComponents this))
  (add [this child]
    (.add this ^java.awt.Component child)
    (.validate this)
    (util/remove-focus-traversal child)
    this)
  (remove [this child]
    (.remove this ^java.awt.Component child)
    this)
  (focus [this]
    (.requestFocus this))
    
  JComponent
  (children [this]
    (.getComponents this))
  (add [this child]
    (.add this ^JComponent child)
    (.validate this)
    (util/remove-focus-traversal child)
    this)
  (remove [this ^JComponent child]
    (.remove this child)
    this)
  (focus [this]
    (.requestFocusInWindow this)))

;;;;;;;;;;;;;;;;;;;;;;
;; Common attributes for all components

(defattributes
  :component
  (:transparent [c _ v]
    (.setOpaque ^JComponent (p/impl c) (not v)))
  (:layout [c _ v]
    (let [v (if (sequential? v) v [v])
          c ^JComponent (p/impl c)]
      (.setLayout c (util/layout c v))))
  (:border [c _ v]
    (let [v       (if (sequential? v) v [v])
          border  (apply util/border v)
          x       ^JComponent (p/impl c)
          current (.getBorder x)]
      (if-not (util/compound-border? current)
        (.setBorder x border)
        (.setBorder x (util/compound-border border (.getInsideBorder ^CompoundBorder current))))))
  (:padding [c _ v]
    (let [x       ^JComponent (p/impl c)
          current (.getBorder x)]
      (.setBorder x (util/compound-border 
                      (if (util/compound-border? current)
                        (.getOutsideBorder ^CompoundBorder current)
                        current)
                      (util/padding v)))))
  (:background [c _ v]
    (.setBackground ^Component (p/impl c) (util/color v)))
  (:color [c _ v]
    (.setForeground ^Component (p/impl c) (util/color v)))
  (:font [c _ v]
    (.setFont ^Component (p/impl c) (util/font v)))
  (:size [c _ [w h :as v]]
    (.setPreferredSize ^Component (p/impl c) (util/dimension w h)))
  (:width [c _ w]
    (ui/attr c :size [w (.getHeight ^Component (p/impl c))]))
  (:height [c _ h]
    (ui/attr c :size [(.getWidth ^Component (p/impl c)) h]))
  (:location [c _ [x y]]
    (.setLocation ^Component (p/impl c) x y))
  (:visible [c _ v]
    (.setVisible ^Component (p/impl c) v))
  (:align [c _ v]
    (.setAlignmentX ^JComponent (p/impl c) (util/alignment v))))

;;;;;;;;;;;;;;;;;
;; Key

(defmethod p/listen [:component :key]
  [c evt f]
  (let [listener (util/create-listener c evt f)]
    (.addKeyListener ^JComponent (p/impl c) listener)
    listener))

(defmethod p/ignore [:component :key]
  [c _ listener]
  (.removeKeyListener ^JComponent (p/impl c) listener))

;;;;;;;;;;;;;;;;;
;; Focus/Blur

(defmethod p/listen [:component :focus]
  [c evt f]
  (let [listener (util/create-listener c evt f)]
    (.addFocusListener ^JComponent(p/impl c) listener)
    listener))

(defmethod p/ignore [:component :focus]
  [c _ listener]
  (.removeFocusListener ^JComponent (p/impl c) listener))

(defmethod p/listen [:component :blur]
  [c evt f]
  (let [listener (util/create-listener c evt f)]
    (.addFocusListener ^JComponent (p/impl c) listener)
    listener))

(defmethod p/ignore [:component :blur]
  [c _ listener]
  (.removeFocusListener ^JComponent (p/impl c) listener))

;;;;;;;;;;;;;;;;;
;; Mouse

(defmethod p/listen [:component :click]
  [c evt f]
  (let [listener (util/create-listener c evt f)]
    (.addMouseListener ^JComponent (p/impl c) listener)
    listener))

(defmethod p/ignore [:component :click]
  [c _ listener]
  (.removeMouseListener ^JComponent (p/impl c) listener))

(defmethod p/listen [:component :mouse-press]
  [c evt f]
  (let [listener (util/create-listener c evt f)]
    (.addMouseListener ^JComponent (p/impl c) listener)
    listener))

(defmethod p/ignore [:component :mouse-press]
  [c _ listener]
  (.removeMouseListener ^JComponent (p/impl c) listener))

(defmethod p/listen [:component :mouse-release]
  [c evt f]
  (let [listener (util/create-listener c evt f)]
    (.addMouseListener ^JComponent (p/impl c) listener)
    listener))

(defmethod p/ignore [:component :mouse-release]
  [c _ listener]
  (.removeMouseListener ^JComponent (p/impl c) listener))

(defmethod p/listen [:component :mouse-enter]
  [c evt f]
  (let [listener (util/create-listener c evt f)]
    (.addMouseListener ^JComponent (p/impl c) listener)
    listener))

(defmethod p/ignore [:component :mouse-enter]
  [c _ listener]
  (.removeMouseListener ^JComponent (p/impl c) listener))

(defmethod p/listen [:component :mouse-exit]
  [c evt f]
  (let [listener (util/create-listener c evt f)]
    (.addMouseListener ^JComponent (p/impl c) listener)
    listener))

(defmethod p/ignore [:component :mouse-exit]
  [c _ listener]
  (.removeMouseListener ^JComponent (p/impl c) listener))

(defmethod p/listen [:component :mouse-wheel]
  [c evt f]
  (let [listener (util/create-listener c evt f)]
    (.addMouseWheelListener ^JComponent (p/impl c) listener)
    listener))

(defmethod p/ignore [:component :mouse-wheel]
  [c _ listener]
  (.removeMouseWheelListener ^JComponent (p/impl c) listener))

(defmethod p/listen [:component :mouse-move]
  [c evt f]
  (let [listener (util/create-listener c evt f)]
    (.addMouseMotionListener ^JComponent (p/impl c) listener)
    listener))

(defmethod p/ignore [:component :mouse-move]
  [c _ listener]
  (.removeMouseMotionListener ^JComponent (p/impl c) listener))

(defmethod p/listen [:component :mouse-drag]
  [c evt f]
  (let [listener (util/create-listener c evt f)]
    (.addMouseMotionListener ^JComponent (p/impl c) listener)
    listener))

(defmethod p/ignore [:component :mouse-drag]
  [c _ listener]
  (.removeMouseMotionListener ^JComponent (p/impl c) listener))


