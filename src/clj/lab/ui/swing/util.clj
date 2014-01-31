(ns lab.ui.swing.util
  (:require [lab.ui.core :as ui]
            [lab.ui.util :as util]
            [lab.ui.protocols :as uip]
            [clojure.java.io :as io])
  (:import [java.awt Dimension Color Font Toolkit Image GraphicsEnvironment GraphicsDevice Window
                     BorderLayout CardLayout FlowLayout GridBagLayout GridLayout
                     KeyboardFocusManager]
           [javax.swing BorderFactory JSplitPane KeyStroke ImageIcon JComponent
                        BoxLayout GroupLayout SpringLayout]
           [javax.swing.text StyleConstants SimpleAttributeSet DefaultHighlighter$DefaultHighlightPainter]))

(def ^Toolkit toolkit (Toolkit/getDefaultToolkit))

;;;;;;;;;;;;;;;;;;;;;;;;
;; SplitPane Orientations

(def split-orientations
  "Split pane possible orientations."
  {:vertical JSplitPane/VERTICAL_SPLIT :horizontal JSplitPane/HORIZONTAL_SPLIT})

;;;;;;;;;;;;;;;;;;;;;;;;
;; Dimension

(defn dimension 
  [w h]
  (Dimension. w h))

;;;;;;;;;;;;;;;;;;;;;;;;
;; Color

(defn color
  "Takes a map of RGB values (i.e. {:r 0 :g 0 :b 0}), an integer
  representing the three values (0xFFFFFF) or a Color instance,
  and returns a java.awt.Color."
  ([x] 
    (cond (map? x)
            (let [{:keys [r g b]} x]
              (color r g b))
          (integer? x)
            (color (util/int-to-rgb x))
          (instance? Color x)
            x
          :else
            (throw)))
  ([r g b]
    (Color. ^int r ^int g ^int b)))
    
;;;;;;;;;;;;;;;;;;;;;;;;
;; Font

(def ^:private font-styles 
  {:bold    Font/BOLD
   :italic  Font/ITALIC
   :plain   Font/PLAIN})

(defn- font-style
  "If style is a vector then a value for the combination of 
the supplied styles is returned, otherwise a value for the
single style is generated."
  [style]
  (if (sequential? style)
    (apply bit-or (map #(font-styles % 0) style))
    (font-styles style 0)))

(defn font
  "Takes name, size and style (a vector with :bold and/or :italic)
all optional and creates a Font."
  ^{:arglists '([string] [:name :size :style])}
  [& [x & xs :as args]]
  (cond (sequential? x)
          (apply font x)
        (string? x)
          (font :name x)
        :else
          (let [{:keys [name size style] :or {size 14 style :plain}} (apply hash-map args)]
            (Font. name (font-style style) size))))

;;;;;;;;;;;;;;;;;;;;;;;;
;; Document Style

(def style-constants {:bold       StyleConstants/Bold,
                      :underline  StyleConstants/Underline,
                      :italic     StyleConstants/Italic,
                      :size       StyleConstants/Size,
                      :family     StyleConstants/Family,
                      :background StyleConstants/Background,
                      :color      StyleConstants/Foreground})

(defn- color-attr?
  "Predicate that checks if the attribute is a color."
  [attr]
  (#{:color :background} (key attr)))

(defn- parse-attrs [style]
  "Parses the attribute definition, replacing RGB values
with Color instances."
  (let [rgb-to-color (fn [[k v]] [k (color v)])
        attrs        (->> style (filter color-attr?) (mapcat rgb-to-color))]
    (if (seq attrs)
      (apply assoc style attrs)
      style)))

(defn make-style* [style]
  "Creates a new style with the given style map."
  (let [attr-set (SimpleAttributeSet.)
        att      (parse-attrs style)]
    (doseq [[k v] att]
      (.addAttribute attr-set (k style-constants) v))
    attr-set))

(def make-style (memoize make-style*))

;;;;;;;;;;;;;;;;;;;;;;;;
;; Image

(defn ^Image image
  "Load an image from a resource file."
  [rsrc]
  (->> rsrc io/resource (.createImage toolkit)))

(defn ^ImageIcon icon
  "Load an image from a resource file."
  [rsrc]
  (->> rsrc image (ImageIcon.)))

;;;;;;;;;;;;;;;;;;;;;;;;
;; Layout

(def ^:private box-layouts
  {:x    BoxLayout/X_AXIS
   :y    BoxLayout/Y_AXIS
   :line BoxLayout/LINE_AXIS
   :page BoxLayout/PAGE_AXIS})

(defn layout [c [x & xs]]
  (case x
    :border   (BorderLayout.)
    :box      (BoxLayout. c (-> xs first box-layouts))
    :card     (CardLayout.)
    :flow     (FlowLayout.)
    :grid     (GridLayout.)
    :grid-bag (GridBagLayout.)
    :group    (GroupLayout. c)
    :spring   (SpringLayout.)))

;;;;;;;;;;;;;;;;;;;;;;;;
;; Border

(defn border 
  "Takes a style of border and additional arguments according
  to the style:
    :none
    :line color width
    :matte resource
    :titled string"
  [style & [x y & xs]]
  (assert (#{:none :line :matte :titled} style) "Invalid line type.")
  (case style
    :none
      (BorderFactory/createEmptyBorder)
    :line
      (BorderFactory/createLineBorder (color (or x 0)) (or y 1))
    :titled
      (BorderFactory/createTitledBorder ^String x)))

;;;;;;;;;;;;;;;;;;;;;;;;
;; KeyStroke

(defn keystroke
  "Returns a swing key stroke based on the string provided."
  [^String s]
  (let [s (->> (.split s " ")
            (map #(if-not (#{"ctrl" "shift" "alt"} %) (.toUpperCase %) %))
            (interpose " ")
            (apply str))]
    (KeyStroke/getKeyStroke s)))

;;;;;;;;;;;;;;;;;;;;;;;;
;; Fullscreen

(def ^GraphicsDevice device (-> (GraphicsEnvironment/getLocalGraphicsEnvironment) .getScreenDevices first))

(defn fullscreen
  "Sets the window that will show in fullscreen mode. If the argument
is null, no window is set and the current one (if any) shows fullscreen
no more."
  [^Window window]
  (.setFullScreenWindow device window))

;;;;;;;;;;;;;;;;;;;;;;;;
;; Highlighter

(defn highlighter [c]
  (DefaultHighlighter$DefaultHighlightPainter. (color c)))

;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Bindings

(def input-map-modes [JComponent/WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
                      JComponent/WHEN_FOCUSED
                      JComponent/WHEN_IN_FOCUSED_WINDOW])

(defn- all-input-maps [ctrl]
  (->> input-map-modes
    (map #(.getInputMap ctrl %))
    (filter (comp not nil?))))

(defn remove-key-binding [ctrl key-stroke]
  (when (instance? JComponent ctrl)
    (let [ks     (KeyStroke/getKeyStroke key-stroke)
          delete (fn [x] (when x (.remove x ks)))]
      (loop [ims (all-input-maps ctrl)]
        (when (seq ims)
          (doall (map delete ims))
          (recur (mapcat #(when % [(.getParent %)]) ims)))))))

;;;;;;;;;;;;;;;;;;;;;;;;
;; Focus Traversal Keys

(defn all-parents
  "Returns a set of all parents of the component,
including itself."
  [^JComponent x]
  (loop [ps #{x}
         p  (.getParent x)]
    (if (and p (not (contains? ps p)))
      (recur (conj ps p) (.getParent x))
      ps)))

(defn remove-focus-traversal
  "Remove all focus traversal key binginds for this
component and its parents."
  [^JComponent x]
  (doseq [x (all-parents x)]
    (remove-key-binding x "ctrl TAB")
    (.setFocusTraversalKeys x KeyboardFocusManager/FORWARD_TRAVERSAL_KEYS #{})
    (.setFocusTraversalKeys x KeyboardFocusManager/BACKWARD_TRAVERSAL_KEYS #{})))
