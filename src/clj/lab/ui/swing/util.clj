(ns lab.ui.swing.util
  (:require [lab.ui.core :as ui]
            [lab.ui.util :as util]
            [lab.ui.hierarchy :as h]
            [lab.ui.protocols :as uip]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:import [java.awt Dimension Color Font Toolkit Image GraphicsEnvironment GraphicsDevice Window
                     BorderLayout CardLayout FlowLayout GridBagLayout GridLayout
                     KeyboardFocusManager Component Insets]
           [java.awt.event MouseAdapter FocusAdapter KeyListener ActionListener WindowAdapter]
           [javax.swing.event CaretListener DocumentListener ChangeListener]
           [javax.swing BorderFactory JSplitPane KeyStroke ImageIcon JComponent InputMap
                        BoxLayout GroupLayout SpringLayout
                        UIManager UIDefaults]
           [javax.swing.text StyleConstants SimpleAttributeSet DefaultHighlighter$DefaultHighlightPainter]))

(def ^Toolkit toolkit (Toolkit/getDefaultToolkit))

(defn set-prop [k v]
  (doto ^UIDefaults (UIManager/getDefaults)
    (.remove k)
    (.put k v))
  nil)

(defn list-props [query]
  (->> (UIManager/getDefaults)
    (filter #(-> % .getKey str (.contains query)))
    (sort-by #(-> % .getKey str))
    (map #(vector (.getKey % ) (.getValue %)))
    (map prn)))

;;;;;;;;;;;;;;;;;;;;;;;;
;; SplitPane Orientations

(defmulti create-listener
  "Creates a swing listener based on the component and the event."
  uip/tag-key-dispatch
  :hierarchy #'h/hierarchy)

(defmethod create-listener [:component :key]
  [c _ f]
  (proxy [KeyListener] []
    (keyPressed [e] (f e))
    (keyReleased [e] (f e))
    (keyTyped [e] (f e))))

(defmethod create-listener [:component :focus]
  [c _ f]
  (proxy [FocusAdapter] [] (focusGained [e] (f e))))

(defmethod create-listener [:component :blur]
  [c _ f]
  (proxy [FocusAdapter] [] (focusLost [e] (f e))))

(defmethod create-listener [:component :click]
  [c _ f]
  (proxy [MouseAdapter] [] (mouseClicked [e] (f e))))

(defmethod create-listener [:component :mouse-press]
  [c _ f]
  (proxy [MouseAdapter] [] (mousePressed [e] (f e))))

(defmethod create-listener [:component :mouse-release]
  [c _ f]
  (proxy [MouseAdapter] [] (mouseReleased [e] (f e))))

(defmethod create-listener [:component :mouse-enter]
  [c _ f]
  (proxy [MouseAdapter] [] (mouseEntered [e] (f e))))

(defmethod create-listener [:component :mouse-exit]
  [c _ f]
  (proxy [MouseAdapter] [] (mouseExited [e] (f e))))

(defmethod create-listener [:component :mouse-wheel]
  [c _ f]
  (proxy [MouseAdapter] [] (mouseWheelMoved [e] (f e))))

(defmethod create-listener [:component :mouse-move]
  [c _ f]
  (proxy [MouseAdapter] [] (mouseMoved [e] (f e))))

(defmethod create-listener [:component :mouse-drag]
  [c _ f]
  (proxy [MouseAdapter] [] (mouseDragged [e] (f e))))

(defmethod create-listener [:button :click]
  [c _ f]
  (proxy [ActionListener] [] (actionPerformed [e] (f e))))

(defmethod create-listener [:menu-item :click]
  [c _ f]
  (proxy [ActionListener] [] (actionPerformed [e] (f e))))

(defmethod create-listener [:text-field :caret]
  [c _ f]
  (proxy [CaretListener] [] (caretUpdate [e] (f e))))

(defmethod create-listener [:text-field :insert]
  [c _ f]
  (proxy [DocumentListener] []
    (insertUpdate [e] (f e))
    (removeUpdate [e])
    (changedUpdate [e])))

(defmethod create-listener [:text-field :delete]
  [c _ f]
  (proxy [DocumentListener] []
    (insertUpdate [e])
    (removeUpdate [e] (f e))
    (changedUpdate [e])))

(defmethod create-listener [:text-field :change]
  [c _ f]
  (proxy [DocumentListener] []
    (insertUpdate [e])
    (removeUpdate [e])
    (changedUpdate [e] (f e))))

(defmethod create-listener [:tabs :change]
  [c _ f]
  (proxy [ChangeListener] [] (stateChanged [e] (f e))))

(defmethod create-listener [:window :closed]
  [c _ f]
  (proxy [WindowAdapter] [] (windowClosed [e] (f e))))

(defmethod create-listener [:window :closing]
  [c _ f]
  (proxy [WindowAdapter] [] (windowClosing [e] (f e))))

(defmethod create-listener [:window :opened]
  [c _ f]
  (proxy [WindowAdapter] [] (windowOpened [e] (f e))))

(defmethod create-listener [:window :minimized]
  [c _ f]
  (proxy [WindowAdapter] [] (windowIconified [e] (f e))))

(defmethod create-listener [:window :restored]
  [c _ f]
  (proxy [WindowAdapter] [] (windowDeiconified [e] (f e))))

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
  "Takes a map of RGB values (i.e. {:r 0 :g 0 :b 0 :a 0}), an integer
  representing the three values (0xFFFFFF) or a Color instance,
  and returns a java.awt.Color."
  ([x] 
    (cond (map? x)
            (let [{:keys [r g b a]} x]
              (color r g b a))
          (vector? x)
            (let [[r g b a] x]
              (color r g b a))
          (integer? x)
            (color (util/int-to-rgb x))
          (instance? Color x)
            x
          :else
            (throw (ex-info "Invalid color format." {:value x}))))
  ([r g b]
    (color r g b nil))
  ([r g b a]
    (let [^int a (or a 255)]
      (Color. ^int r ^int g ^int b a))))
    
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

(defn register-font [font-path]
  (try
    (let [ge          (GraphicsEnvironment/getLocalGraphicsEnvironment)
          font-stream (-> font-path io/resource io/input-stream)
          font        (Font/createFont Font/TRUETYPE_FONT font-stream)]
      (.registerFont ge font))
    (catch Exception ex
      (log/error ex (format "The font %s could not be loaded" font-path)))))

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

(defn- parse-attrs
  "Parses the attribute definition, replacing RGB values
with Color instances."
  [style]
  (let [rgb-to-color (fn [[k v]] [k (color v)])
        attrs        (->> style (filter color-attr?) (mapcat rgb-to-color))]
    (if (seq attrs)
      (apply assoc style attrs)
      style)))

(defn make-style*
  "Creates a new style with the given style map."
  [style]
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

(defn- top-left-bottom-right [x]
  (if-let [n (and (sequential? x) (count x))]
    (let [[a b c d]  x]
      (case (int n)
        1 [a a a a]
        2 [a b a b]
        3 [a b a c]
        4 [a b c d]))
    [x x x x]))

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
      (let [[top left bottom right] (top-left-bottom-right (or y 1))]
        (BorderFactory/createMatteBorder ^int top ^int left ^int bottom ^int right ^Color (color (or x 0))))
    :titled
      (BorderFactory/createTitledBorder ^String x)))

(defn compound-border? [border]
  (instance? javax.swing.border.CompoundBorder border))

(defn compound-border
  "Creates a compound border with the inner and outer
borders provided."
  [outer inner]
  (BorderFactory/createCompoundBorder outer inner))

(defn padding
  "Creates an empty border with the specified padding, which
can be a single value or a collection specifying:
 [top-bottom-left-right]
 [top-bottom left-right]
 [top-bottom left right]
 [top left bottom right]"
  [x]
  (let [[top left bottom right] (top-left-bottom-right x)]
    (BorderFactory/createEmptyBorder top left bottom right)))

(defn insets 
  [x]
  (let [[top left bottom right] (top-left-bottom-right x)]
    (Insets. top left bottom right)))

;;;;;;;;;;;;;;;;;;;;;;;;
;; KeyStroke

(defn keystroke
  "Returns a swing key stroke based on the string provided."
  [^String s]
  (let [s (->> (.split s " ")
            (map #(if-not (#{"ctrl" "shift" "alt"} %) (.toUpperCase ^String %) %))
            (interpose " ")
            (apply str))]
    (KeyStroke/getKeyStroke ^String s)))

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

(def input-map-modes {:focused-ancestor JComponent/WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
                      :focused-self     JComponent/WHEN_FOCUSED
                      :focused-window   JComponent/WHEN_IN_FOCUSED_WINDOW})

(defn- all-input-maps [^JComponent ctrl]
  (->> (vals input-map-modes)
    (map #(.getInputMap ctrl %))
    (filter (comp not nil?))))

(defn remove-key-binding [ctrl ks]
  (when (instance? JComponent ctrl)
    (let [ks     (if (string? ks) (keystroke ks) ks)
          delete (fn [^InputMap x] (when x (.remove x ks)))]
      (loop [ims (all-input-maps ctrl)]
        (when (seq ims)
          (doseq [x ims] (delete x))
          (recur (mapcat #(when % [(.getParent ^InputMap %)]) ims)))))))

(defn register-key-binding [^JComponent ctrl ks f & [focus-scope]]
  (let [ks     (keystroke ks)
        action (proxy [javax.swing.AbstractAction] [] (actionPerformed [e] (f e)))
        scope  (or (input-map-modes focus-scope) JComponent/WHEN_FOCUSED)]
    (.put (.getInputMap ctrl scope) ks f)
    (.put (.getActionMap ctrl) f action)))

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
  "Remove all focus traversal key bindings for this
component and its parents."
  [^JComponent x]
  (let [ks (map keystroke ["ctrl TAB" "ctrl shift TAB"])]
    (doseq [^JComponent x (all-parents x)]
      (let [forward-keys  (set (.getFocusTraversalKeys x KeyboardFocusManager/FORWARD_TRAVERSAL_KEYS))
            backward-keys (set (.getFocusTraversalKeys x KeyboardFocusManager/BACKWARD_TRAVERSAL_KEYS))]
        (doseq [k ks] (remove-key-binding x k))
        (.setFocusTraversalKeys x KeyboardFocusManager/FORWARD_TRAVERSAL_KEYS (apply disj forward-keys ks))
        (.setFocusTraversalKeys x KeyboardFocusManager/BACKWARD_TRAVERSAL_KEYS (apply disj backward-keys ks))))))

;;;;;;;;;;;;;;;;;;;;
;; Alignment

(def ^:private alignment-map
  {:left   Component/LEFT_ALIGNMENT
   :right  Component/RIGHT_ALIGNMENT
   :center Component/CENTER_ALIGNMENT})

(defn alignment
  "Returns the alignment given the key."
  [v]
  (alignment-map v))
