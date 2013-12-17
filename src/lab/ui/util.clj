(ns lab.ui.util)

(defn int-to-rgb
  "Converts a single int value int a RGB triple."
  [n]
  (let [r (-> n (bit-and 0xFF0000) (bit-shift-right 16))
        g (-> n (bit-and 0x00FF00) (bit-shift-right 8))
        b (-> n (bit-and 0x0000FF))]
    {:r r :g g :b b}))

(defn rgb-to-int [{:keys [r g b]}]
  "Converts a RGB triple to a single int value."
  (int (+ (* r 65536) (* g 256) b)))

(defrecord FontStyle [])

(defn font-style 
  "Creates a map that represents a font style to apply
to document text. Valid keys are:
  :bold        bool
  :underline   bool
  :italic      bool
  :size        int
  :family      string
  :background  [0xFFFFFF] | [255 255 255]
  :color       [0xFFFFFF] | [255 255 255]"
  [style]
  (map->FontStyle style))
