(ns macho.ui
  (:import [javax.swing JFrame JPanel JScrollPane JTextPane JTextArea 
            JTextField JButton JFileChooser JSplitPane JTree
            JTabbedPane JMenuBar JMenu JMenuItem KeyStroke
            JOptionPane JSeparator]
           [javax.swing.text DefaultStyledDocument DefaultHighlighter$DefaultHighlightPainter]
           [java.io OutputStream PrintStream File OutputStreamWriter]
           [java.awt BorderLayout Font Color]
           [java.awt.event KeyEvent])
  (:require [clojure.reflect :as r]
            [clojure.string :as str]
            [clojure.repl :as repl]
            [clojure.java.io :as io]
            [macho.ui.swing.core :as ui :reload true]
            [macho.ui.swing.highlighter :as hl :reload true]
            [macho.ui.swing.undo :as undo :reload true]
            [macho.ui.swing.text :as txt]
            [macho.ui.protocols :as proto]
            [macho.ui.swing.image :as img]))
;;------------------------------
(def app-name "macho")
(def new-doc-title "Untitled")
(def icons-paths ["./resources/icon-16.png"
                  "./resources/icon-32.png"
                  "./resources/icon-64.png"])
(def icons (for [path icons-paths] (img/image path)))
;;------------------------------
(def ^:dynamic *current-font* (Font. "Consolas" Font/PLAIN 14))
(def default-dir (atom (.getCanonicalPath (File. "."))))
;;------------------------------
(defn list-methods
  "Lists the methods for the supplied class."
  ([c] (list-methods c ""))
  ([c name]
    (let [members (:members (r/type-reflect c :ancestors true))
          methods (filter #(:return-type %) members)]
      (filter 
        #(or (.contains (str %) name) (empty? name))
        (sort (for [m methods] (:name m)))))))
;;------------------------------
(defn eval-code
  "Evaluates the code in the string supplied."
  [^String code]
  (try
    (println (load-string code))
    (catch Exception e
         (println e)
	(println (.getMessage e)))))
;;------------------------------
(defn check-key
  "Checks if the key and the modifier match the event's values"
  [evt k m]
  (and 
    (or (nil? k) (= k (.getKeyCode evt)))
    (or (nil? m) (= m (.getModifiers evt)))))
;;------------------------------
(defn current-txt [main]
  (let [tabs   (:tabs main)
        idx    (.getSelectedIndex tabs)
        scroll (.getComponentAt tabs idx)
        pnl    (.. scroll getViewport getView)
        txt    (.getComponent pnl 0)]
  txt))
;;------------------------------
(defn current-doc [main]
  (let [txt (current-txt main)]
    (.getDocument txt)))
;;------------------------------
(defn show-save-dialog []
  (let [dialog (JFileChooser. @default-dir)
        result (.showSaveDialog dialog nil)
        file   (.getSelectedFile dialog)]
    (when file (.getPath file))))
;;------------------------------
(defn current-path 
  "Finds the current working tab and shows a 
file chooser window if it's a new file."
  [main]
  (let [tabs (:tabs main)
        idx  (.getSelectedIndex tabs)
        path (.getTitleAt tabs idx)]
    (when (not= path new-doc-title)
      path)))
;;------------------------------
(defn save-src [main]
  (let [tabs     (:tabs main)
        txt-code (current-txt main)
        path     (or (current-path main) (show-save-dialog))
        content  (proto/text txt-code)]
    (when path 
      (spit path content)
      (.setTitleAt tabs (.getSelectedIndex tabs) path))))
;;------------------------------
(defn eval-src
  "Evaluates source code."
  [main]
  (if-let [path (current-path main)]
    (do (save-src main)
        (println "Loaded file" path)
        (println (load-file path)))
    (-> main current-txt proto/text eval-code)))
;;------------------------------
(defn remove-highlight 
  "Removes all highglights from the text control."
  ([txt] (.. txt getHighlighter removeAllHighlights))
  ([txt tag] (.. txt getHighlighter (removeHighlight tag))))
;;------------------------------
(defn add-highlight
  "Add a single highglight in the text control."
  ([txt pos len] (add-highlight txt pos len Color/YELLOW))
  ([txt pos len color]
    (let [doc  (.getDocument txt)
          hl   (.getHighlighter txt)
          pntr (DefaultHighlighter$DefaultHighlightPainter. color)]
      (.addHighlight hl pos (+ len pos) pntr))))
;;------------------------------
(defn find-src 
  "Shows the dialog for searching the source
  in the current tabs."
  [main]
  (let [txt  (current-txt main)
        s    (str/lower-case (proto/text txt))
        ptrn (JOptionPane/showInputDialog (:main main) "Enter search string:" "Find" JOptionPane/QUESTION_MESSAGE)
        lims (when ptrn (hl/limits (str/lower-case ptrn) s))]
    (remove-highlight txt)
    (doseq [[a b] lims] (add-highlight txt a (- b a)))))
;;------------------------------
(defn find-doc 
  "Uses the clojure.repl/find-doc function to
search for the selected text in the current docuement."
  [main]
  (let [txt  (current-txt main)
        slct (.getSelectedText txt)]
    (repl/find-doc slct)))
;;-------------------------------
(defn update-line-numbers [doc lines]
  (let [pos (.getLength doc)
        root (.getDefaultRootElement doc)
        n (.getElementIndex root pos)]
    (ui/queue-action
      #(doto lines
        (.setText (apply str (interpose "\n" (range 1 (+ n 2)))))
        (.updateUI)))))
;;------------------------------
(defn insert-text
  "Inserts the specified text in the document."
  ([^JTextPane txt s]
    (insert-text txt s true))
  ([^JTextPane txt s restore]
    (insert-text txt s restore (.getCaretPosition txt)))
  ([^JTextPane txt s restore pos]
    (let [doc (.getDocument txt)]
      (.insertString doc pos s nil)
      (when restore (.setCaretPosition txt pos)))))
;;------------------------------
(defn find-char 
  "Finds the next char in s, starting to look from
position cur, in the direction specified by dt (1 or -1)."
  [s cur f dt]

  (cond (or (neg? cur) (<= (.length s) cur)) -1
        (f (nth s cur)) cur
        :else (recur s (+ cur dt) f dt)))
;;------------------------------
(defn insert-tabs 
  "Looks for the first previous \\newline character 
and copies the indenting for the new line."
  [txt]
  (let [pos  (-> (.getCaretPosition txt) dec)
        s    (proto/text txt)
        prev (find-char s pos #(= \newline %) -1)
        end  (find-char s (inc prev) #(not= \space %) 1)
        dt   (dec (- end prev))
        t    (apply str (repeat dt " "))]
    (ui/queue-action #(insert-text txt t false))))
;;------------------------------
(defn handle-tab [txt e]
  (let [tab    "  "
        pos    (.getCaretPosition txt)
        text   (.getSelectedText txt)]
    (.consume e)
    (if-not text
      (ui/queue-action #(insert-text txt tab false))
      (let [start  (.getSelectionStart txt)
            pos    (->> text (map-indexed #(when (= \newline %2) (inc %1)))
                             (filter identity)
                             (concat [0])
                             sort
                             reverse)]
        (doseq [x pos] 
          (insert-text txt tab false (+ start x)))))))
;;------------------------------
(defn input-format 
  "Insert a closing parentesis."
  [^JTextPane txt e]
  (let [c (.getKeyChar e)
        k (.getKeyCode e)
        m (.getModifiers e)]
    (cond (= \( c)
            (ui/queue-action #(insert-text txt ")"))
          (= \{ c)
            (ui/queue-action #(insert-text txt "}"))
          (= \[ c)
            (ui/queue-action #(insert-text txt "]"))
          (= \" c)
            (ui/queue-action #(insert-text txt "\""))
          (and (zero? m) (= KeyEvent/VK_ENTER k))
            (insert-tabs txt)
          (= KeyEvent/VK_TAB k)
            (handle-tab txt e))))
;;------------------------------
(defn change-font-size [txts e]
  (when (check-key e nil KeyEvent/CTRL_MASK)
    (.consume e)
    (let [font *current-font*
          op   (if (neg? (.getWheelRotation e)) inc #(if (> % 1) (dec %) %))
          size (-> (.getSize font) op)]
      (def ^:dynamic *current-font* (.deriveFont font (float size)))
      (doseq [txt txts] (.setFont txt *current-font*)))))
;;------------------------------
(defn match-paren [s pos end delta]
  "Finds the matching endelimiter for"
  (loop [cur  (+ pos delta) 
         acum 0]
    (cond (neg? cur) nil
          (<= (.length s) cur) nil
          (= (nth s pos) (nth s cur))
            (recur (+ cur delta) (inc acum))
          (= (nth s cur) end) 
            (if (zero? acum) cur 
              (recur (+ cur delta) (dec acum)))
          :else (recur (+ cur delta) acum))))
;;------------------------------
(defn check-paren
  "Checks if the characters in the caret's current
position is a delimiter and looks for the closing/opening
delimiter."
  [txt]
  (let [tags (atom nil)]
    (fn [e]
      (when @tags
        (doseq [tag @tags] (remove-highlight txt tag)))
      (let [pos   (dec (.getDot e))
            s     (proto/text txt)
            c     (get-in s [pos])
            delim {\( {:end \), :d 1}, \) {:end \(, :d -1}
                   \{ {:end \}, :d 1}, \} {:end \{, :d -1}
                   \[ {:end \], :d 1}, \] {:end \[, :d -1}}]
        (when-let [{end :end dir :d} (delim c)]
          (when-let [end (match-paren s pos end dir)]
            (reset! tags 
                    (doall (map #(add-highlight txt % 1 Color/LIGHT_GRAY) [pos end])))))))))
;;------------------------------
(defn new-document
  "Adds a new tab to tabs and sets its title."
  ([main]
    (new-document main new-doc-title))
  ([main title] 
    (new-document main title nil))
  ([main title src]
    (let [tabs       (:tabs main)
          doc        (DefaultStyledDocument.)
          txt-code   (JTextPane. doc)
          undo-mgr   (undo/make-undo-mgr)
          pnl-code   (JPanel.)
          pnl-scroll (JScrollPane. pnl-code)
          txt-lines  (JTextArea.)]

      (doto pnl-code
        (.setLayout (BorderLayout.))
        (.add txt-code BorderLayout/CENTER))
        
      (doto pnl-scroll
         (.setRowHeaderView txt-lines))

      (doto txt-lines
        (.setFont *current-font*)
        (.setEditable false)
        (.setBackground Color/LIGHT_GRAY))
        
      ;; Eval: CTRL + Enter
      (ui/on :key-press txt-code
             (fn [_] (eval-code (.getSelectedText txt-code)))
               KeyEvent/VK_ENTER KeyEvent/CTRL_MASK)

      ; Undo/redo key events
      (ui/on :key-press txt-code
             (fn [_] (when (.canUndo undo-mgr) (.undo undo-mgr)))
               KeyEvent/VK_Z KeyEvent/CTRL_MASK)
      (ui/on :key-press txt-code
             (fn [_] (when (.canRedo undo-mgr) (.redo undo-mgr)))
               KeyEvent/VK_Y KeyEvent/CTRL_MASK)
               
      (ui/on :key-press txt-code #(input-format txt-code %))

      (ui/on :caret-update txt-code (check-paren txt-code))
      
      ;; Load the text all at once
      (when src (.setText txt-code src))

      ; High-light text after code edition.
      (ui/on :change 
             txt-code
             #(future (hl/high-light txt-code)
                      (update-line-numbers doc txt-lines)
                      ))
                  
      ;(ui/on :change txt-code #(println (proto/insertion? %) (proto/text %) %))
                  
      (ui/queue-action #(do (update-line-numbers doc txt-lines) 
                            (hl/high-light txt-code)))
      
      ; Add Undo manager
      (.setLimit undo-mgr -1)
      (ui/on :undoable doc #(undo/handle-edit undo-mgr %))

      ; Set the increment for each vertical scroll
      (.. pnl-scroll (getVerticalScrollBar) (setUnitIncrement 16))

      (ui/on :mouse-wheel pnl-scroll #(change-font-size [txt-code txt-lines] %))

      (doto tabs
        (.addTab title pnl-scroll)
        (.setSelectedIndex (dec (.getTabCount tabs))))

      (doto txt-code
        (.setFont *current-font*)
        (.setBackground Color/BLACK)
        (.setCaretPosition 0)
        (.grabFocus))
        
      txt-code)))
;;------------------------------
(defn open-src
  "Open source file."
  [main]
  (let [dialog (JFileChooser. @default-dir)
        result (.showOpenDialog dialog nil)
        file (.getSelectedFile dialog)
        path (if file (.getPath file) nil)]
    (when path
      (reset! default-dir (.getCanonicalPath (File. path)))
      (new-document main path (slurp path)))))
;;------------------------------
(defn clear-repl [main]
  (.setText (:repl main) nil))
;;------------------------------
(defn close
  "Close the current tab."
  [main]
  (let [tabs (:tabs main)
        idx (.getSelectedIndex tabs)]
    (.removeTabAt tabs idx)))
;;------------------------------
(def menu-options
  [{:name "File"
    :items [{:name "New" :action new-document :keys [KeyEvent/VK_N KeyEvent/CTRL_MASK]}
            {:name "Open" :action open-src :keys [KeyEvent/VK_O KeyEvent/CTRL_MASK]}
            {:name "Save" :action save-src :keys [KeyEvent/VK_S KeyEvent/CTRL_MASK]}
            {:name "Close" :action close :keys [KeyEvent/VK_W KeyEvent/CTRL_MASK]}
            {:separator true}
            {:name "Exit" :action #(do % (System/exit 0)) :keys [KeyEvent/VK_X KeyEvent/ALT_MASK]}]}
   {:name "Code"
    :items [{:name "Eval" :action eval-src :keys [KeyEvent/VK_E KeyEvent/CTRL_MASK]}
            {:name "Find" :action find-src :keys [KeyEvent/VK_F KeyEvent/CTRL_MASK]}
            {:name "Find doc" :action find-doc :keys [KeyEvent/VK_F KeyEvent/ALT_MASK]}
            {:name "Clear Log" :action clear-repl :keys [KeyEvent/VK_L KeyEvent/CTRL_MASK]}]}])
;;------------------------------
(defn build-menu
  "Builds the application's menu."
  [main]
  (let [menubar    (JMenuBar.)
        key-stroke #(KeyStroke/getKeyStroke %1 %2)]
    
    (doseq [{menu-name :name items :items} menu-options]
      (let [menu (JMenu. menu-name)]
        (.add menubar menu)
        (doseq [{item-name :name f :action kys :keys separator :separator} items]
          (let [menu-item (if separator
                            (JSeparator.)
                            (JMenuItem. item-name))]
            (when (not separator)
              (ui/on :click menu-item #(f main))
              (.setAccelerator menu-item (apply key-stroke kys)))
            (.add menu menu-item)))))
    menubar))
;;------------------------------
(defn redirect-out
  "Creates a PrintStream that writes to the
JTextArea instance provided and then replaces
System/out with this stream."
  [^JTextArea txt]
  (let [stream (proxy [OutputStream] []
                 (write
                   ([b off len] (.append txt (String. b off len)))
                   ([b] (.append txt (String. b)))))
        out (PrintStream. stream true)]
    (System/setOut out)
    (System/setErr out)))
;;------------------------------------------
(defn make-main 
  "Creates the main window and all
its controls."
  [name]
  (let [main (JFrame. name)
        txt-repl (txt/text-area)
        tabs (JTabbedPane.)
        txt-in (txt/text-area)
        pane-repl (JSplitPane.)
        pane-center-left (JSplitPane.)
        pane-all (JSplitPane.)
        files-tree (JTree. (to-array []))
        ui-main {:main main :tabs tabs :repl txt-repl}]

    ; Set controls properties
    (doto txt-repl
      (.setEditable false)
      (.setFont *current-font*))

    (doto pane-repl
      (.setOrientation JSplitPane/VERTICAL_SPLIT)
      (.setResizeWeight 0.8)
      (.setTopComponent (JScrollPane. txt-repl))
      (.setBottomComponent txt-in))

    (doto pane-center-left
      (.setOrientation JSplitPane/HORIZONTAL_SPLIT)
      (.setResizeWeight 0.8)
      (.setLeftComponent tabs)
      (.setRightComponent (JScrollPane. txt-repl)))

    (doto pane-all
      (.setOrientation JSplitPane/HORIZONTAL_SPLIT)
      (.setLeftComponent (JScrollPane. files-tree))
      (.setRightComponent pane-center-left))

    (.setDividerLocation pane-center-left 0.8)

    (.setDividerLocation pane-all 150)

    ; Redirect std out
    (redirect-out txt-repl)
    ; Modify the binding for the *out* var in clojure.core
    (intern 'clojure.core '*out* (java.io.OutputStreamWriter. System/out))

    (doto main
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setIconImages icons)
      (.setSize 800 600)
      (.setJMenuBar (build-menu ui-main))
      (.add pane-center-left BorderLayout/CENTER)
      (.setVisible true))

    ui-main))
;;------------------------------