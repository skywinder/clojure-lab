(ns com.cleasure.main
  (:import [javax.swing JFrame JPanel JScrollPane JTextPane JTextArea 
            JTextField JButton JFileChooser UIManager JSplitPane 
            SwingUtilities JTabbedPane JMenuBar JMenu JMenuItem KeyStroke]
           [javax.swing.text StyleContext DefaultStyledDocument]
           [javax.swing.undo UndoManager]
           [javax.swing.event DocumentListener]
           [java.io OutputStream PrintStream File OutputStreamWriter]
           [java.awt BorderLayout FlowLayout Font]
           [java.awt.event MouseAdapter KeyAdapter KeyEvent ActionListener])
  (:require [clojure.reflect :as r]
            [com.cleasure.ui.high-lighter :as hl]
            [com.cleasure.ui.text.undo-redo :as undo])
  (:use [clojure.java.io]))

(def app-name "Cleajure")
(def default-dir (.getCanonicalPath (File. ".")))

; Set native look & feel instead of Swings default
(UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))

(defn str-contains? [s ptrn]
  "Checks if a string contains a substring"
  (.contains (str s) ptrn))

(defn list-methods
  ([c] (list-methods  c ""))
  ([c name]
    (let [members (:members (r/type-reflect c :ancestors true))
          methods (filter #(:return-type %) members)]
      (filter 
        #(or (str-contains? % name) (empty name))
        (sort (for [m methods] (:name m)))))))

(defn queue-ui-action [f]
  (SwingUtilities/invokeLater (proxy [Runnable] [] (run [] (f)))))

(defn eval-code [code]
  (println (load-string code)))

(defn on-click [cmpt f]
  (.addActionListener cmpt 
    (proxy [ActionListener] []
      (actionPerformed [e] (f)))))

(defn check-key [evt k m]
	"Checks if the key and the modifier match with the event's values"
	(and 	(or (= k (.getKeyCode evt)) (not k))
			(or (= m (.getModifiers evt)) (not m))))

(defn on-keypress
	([cmpt f] (on-keypress cmpt f nil nil))
	([cmpt f key mask]
		(.addKeyListener cmpt
			(proxy [KeyAdapter] []
				(keyPressed [e] (when (check-key e key mask) (f)))))))

(defn on-keyrelease
	([cmpt f] (on-keyrelease cmpt f nil nil))
	([cmpt f key mask]
		(.addKeyListener cmpt
			(proxy [KeyAdapter] []
				(keyReleased [e] (when (check-key e key mask) (f)))))))

(defn on-changed [cmpt f]
	(let [doc (.getStyledDocument cmpt)]
		(.addDocumentListener doc
			(proxy [DocumentListener] []
				(changedUpdate [e] nil)
				(insertUpdate [e] (queue-ui-action f))
				(removeUpdate [e] (queue-ui-action f))))))

(defn current-txt [tabs]
  (let [idx (.getSelectedIndex tabs)
        scroll (.getComponentAt tabs  idx)
        pnl (.. scroll getViewport getView)
        txt (.getComponent pnl 0)]
  txt))

(defn current-path [tabs]
  (let [idx (.getSelectedIndex tabs)
        path (.getTitleAt tabs idx)]
     path))

(defn save-src [tabs]
	(let [	txt-code	(current-txt tabs)
			path		(current-path tabs)
			content		(.getText txt-code)]
		(with-open [wrtr (writer path)]
			(.write wrtr content))))

(defn new-document [a b] nil)

(defn open-src [tabs]
	(let [	dialog	(JFileChooser. default-dir)
		result	(.showOpenDialog dialog nil)
		file	(.getSelectedFile dialog)
		path	(if file (.getPath file) nil)]
		(when path
			(let [txt-code (new-document tabs path)]
				(.setText txt-code (slurp path))
				(hl/high-light txt-code)))))

(defn eval-src [tabs]
	(let [txt (current-txt tabs)]
		(eval-code (.getText txt))))

(defn new-document [tabs title]
	(let [	doc		(DefaultStyledDocument.)
		txt-code		(JTextPane. doc)
		undo-mgr		(UndoManager.)
		pnl-code		(JPanel.)
		pnl-scroll	(JScrollPane. pnl-code)]

		(doto pnl-code
			(.setLayout (BorderLayout.))
			(.add txt-code BorderLayout/CENTER))

		; Eval: CTRL + Enter
		(on-keypress txt-code #(eval-code (.getSelectedText txt-code))
			KeyEvent/VK_ENTER KeyEvent/CTRL_MASK)

		; Add Undo manager
		(undo/on-undoable doc undo-mgr)

		; Undo/redo key events
		(on-keypress txt-code #(when (.canUndo undo-mgr) (.undo undo-mgr))
			KeyEvent/VK_Z KeyEvent/CTRL_MASK)
		(on-keypress txt-code #(when (.canRedo undo-mgr) (.redo undo-mgr))
			KeyEvent/VK_Y KeyEvent/CTRL_MASK)

		; High-light text after key release.
		(on-changed txt-code #(hl/high-light txt-code))

		(.. pnl-scroll (getVerticalScrollBar) (setUnitIncrement 16));

		(doto tabs
			(.addTab title pnl-scroll)
			(.setSelectedIndex  (- (.getTabCount tabs) 1)))

		txt-code))

(defn redirect-out [txt]
	(let [	stream	(proxy [OutputStream] []
				(write
					([b off len] (.append txt (String. b off len)))
					([b] (.append txt (String. b)))))
		out	(PrintStream. stream true)]
		(System/setOut out)
		(System/setErr out)))

(defn build-menu [tabs]
	(let [	menubar		(JMenuBar.)
		menu		(JMenu. "File")
		item-new		(JMenuItem. "New")
		item-open	(JMenuItem. "Open")
		item-save	(JMenuItem. "Save")
		item-eval	(JMenuItem. "Eval")]

		(on-click item-new #(new-document tabs "Untitled"))
		(.setAccelerator item-new (KeyStroke/getKeyStroke KeyEvent/VK_N KeyEvent/CTRL_MASK))

		(on-click item-open #(open-src tabs))
		(.setAccelerator item-open (KeyStroke/getKeyStroke KeyEvent/VK_O KeyEvent/CTRL_MASK))

		(on-click item-save #(save-src tabs))
		(.setAccelerator item-save (KeyStroke/getKeyStroke KeyEvent/VK_S KeyEvent/CTRL_MASK))

		(on-click item-eval #(eval-src tabs))
		(.setAccelerator item-eval (KeyStroke/getKeyStroke KeyEvent/VK_E KeyEvent/CTRL_MASK))

		(.add menu item-new)
		(.add menu item-open)
		(.add menu item-save)
		(.add menu item-eval)

		(.add menubar menu)
		menubar))

(defn make-main [name]
  (let [main (JFrame. name)
        tabs (JTabbedPane.)
        txt-log (JTextArea.)
        split (JSplitPane.)]
    ; Set controls properties
    (.setEditable txt-log false)
    (redirect-out txt-log)

    (doto split
      (.setOrientation JSplitPane/HORIZONTAL_SPLIT)
      (.setResizeWeight 1.0)
      (.setLeftComponent tabs)
      (.setRightComponent (JScrollPane. txt-log)))

    (doto main
      ;(.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setSize 800 600)
      (.setJMenuBar (build-menu tabs))
      (.add split BorderLayout/CENTER)
      (.setVisible true))

    (doto split
      (.setDividerLocation 0.8))
      
    main))

(def main (make-main app-name))

(in-ns 'clojure.core)
(def ^:dynamic *out-custom* (java.io.OutputStreamWriter. System/out))
(def ^:dynamic *out-original* *out*)
(def ^:dynamic *out* *out-custom*)