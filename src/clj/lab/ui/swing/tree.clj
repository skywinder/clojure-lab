(ns lab.ui.swing.tree
  (:use     [lab.ui.protocols :only [Component Selection Implementation impl abstract to-map
                                     listen ignore]])
  (:require [lab.ui.core :as ui]
            [lab.ui.util :refer [defattributes definitializations]]
            [lab.ui.swing.util :as util])
  (:import  [javax.swing JTree JTree$DynamicUtilTreeNode]
            [javax.swing.tree TreeNode DefaultMutableTreeNode DefaultTreeModel 
                              TreePath DefaultTreeModel DefaultTreeCellRenderer]
            [javax.swing.event TreeSelectionListener TreeExpansionListener 
                               TreeExpansionEvent]
            [java.awt.event MouseAdapter KeyAdapter]))

(util/set-prop "Tree.paintLines" false)
(util/set-prop "Tree.line" (util/color 0xFFFFFF))
(util/set-prop "Tree.expandedIcon" (util/icon "expand.png"))
(util/set-prop "Tree.collapsedIcon" (util/icon "collapse.png"))

(defn- update-tree-from-node [^DefaultMutableTreeNode node]
  (let [root (.getRoot node)
        tree ^JTree (:tree (meta root))]
      (when (and tree (.getModel tree))
        (.reload ^DefaultTreeModel (.getModel tree) node))))

(defn- add-node
  [^DefaultMutableTreeNode parent ^DefaultMutableTreeNode child]
  (let [root (.getRoot parent)
        tree ^JTree (:tree (meta root))
        model (and tree (.getModel tree))]
    (if model
      (let [c       (abstract child)
            expand? (ui/attr c :expanded)
            path    (TreePath. ^objects (.getPath child))]
        (.insertNodeInto ^DefaultTreeModel model child parent (.getChildCount parent))
        (if expand?
          (.expandPath tree path)
          (.collapsePath tree path)))
      (do
        (.add parent child)
        (update-tree-from-node parent)))))

(defn- remove-node
  [^DefaultMutableTreeNode parent ^DefaultMutableTreeNode child]
  (let [root (.getRoot parent)
        tree ^JTree (:tree (meta root))
        model ^DefaultTreeModel (and tree (.getModel tree))]
    (if model
      (.removeNodeFromParent ^DefaultTreeModel model child)
      (do
        (.remove parent child)
        (update-tree-from-node parent)))))

(defn tree-node-init [c]
  (let [ab        (atom nil)
        meta-data (atom nil)
        children  (when-not (ui/attr c :leaf) (to-array []))]
    (proxy [JTree$DynamicUtilTreeNode
            lab.ui.protocols.Implementation
            clojure.lang.IObj]
           [(ui/attr c :item) children]
      (abstract
        ([] @ab)
        ([x] (reset! ab x) this))
      (meta [] @meta-data)
      (withMeta [x]
        (reset! meta-data x)
        this))))

(defn- node-expansion
  "Takes a TreeExpansionEvent, looks for the expanded node
and fires the :expansion handler in it, if there is one.
The handler should return falsey if the node was modified."
  [^TreeExpansionEvent e]
  (let [tree ^JTree (.getSource e)
        node (.. e getPath getLastPathComponent)
        abs  (abstract node)
        fns  (ui/listeners abs :expansion)
        e    (assoc (to-map e) :source abs)]
    (doseq [f fns]
      (when (and f (#'ui/event-handler f e))
        ;; notify the model to reload the modified node
        (update-tree-from-node node)))))

(defn- node-event
  "Event handler for either click or key.
event can be :click or :key."
  [event e]
  (let [tree ^JTree (.getSource ^java.util.EventObject e)
        node (.getLastSelectedPathComponent tree)
        abs  (and node (abstract node))
        fns  (ui/listeners abs event)
        e    (assoc (to-map e) :source abs)]
    (doseq [f fns]
      (when (and node f (#'ui/event-handler f e))
        ;; notify the model to reload the modified node
        (update-tree-from-node node)))))

(defn tree-init [c]
  (let [expansion (proxy [TreeExpansionListener] []
                    (treeCollapsed [e])
                    (treeExpanded [e] (#'node-expansion e)))
        click     (proxy [MouseAdapter] []
                       (mousePressed [e] (#'node-event :click e)))
        key       (proxy [KeyAdapter] []
                    (keyPressed [e] (#'node-event :key e))
                    (keyReleased [e] (#'node-event :key e))
                    (keyTyped [e] (#'node-event :key e)))]
    (doto (JTree.)
      (.setModel (DefaultTreeModel. nil))
      (.addTreeExpansionListener expansion)
      (.addMouseListener click)
      (.addKeyListener key))))

(definitializations
  :tree        tree-init
  :tree-node   tree-node-init)

(extend-type DefaultMutableTreeNode
  Component
  (add [this child]
    (add-node this child)
    this)
  (remove [this child]
    (remove-node this child)
    this)
  (children [this]
    (.children this))
  (focus [this] this)

  Implementation
  (abstract
    ([this] (.abstract ^lab.ui.protocols.Implementation this))
    ([this x]
      (.abstract ^lab.ui.protocols.Implementation this x)
      this)))

(extend-type JTree
  Component
  (add [this child]
    (let [model (DefaultTreeModel. (with-meta child {:tree this}))]
      (.setModel this model)
      this))
  (remove [this ^TreeNode child]
    (let [model ^DefaultTreeModel (.getModel this)]
      (if (nil? (.getParent child))
        (.setModel this nil)
        (.removeNodeFromParent model ^DefaultMutableTreeNode child)))
      this)
  (children [this]
    (.getComponents this))
  (focus [this]
    (.requestFocusInWindow this))

  Selection
  (selection
    ([this]
      (when-let [node ^lab.ui.protocols.Implementation (.getLastSelectedPathComponent this)]
        (-> node abstract (ui/attr :id))))
    ([this row]
      (.setSelectionRow this row)
      this)))

(defn- ^DefaultTreeCellRenderer cell-renderer [c]
  (.getCellRenderer ^JTree (impl c)))

(defattributes
  :tree
  (:hide-root [c _ v]
    (.setRootVisible ^JTree (impl c) (not v)))
  (:selected-node-background [c _ v]
    (doto (cell-renderer c)
      (.setBackgroundSelectionColor (util/color v))))
  (:unselected-node-background [c _ v]
    (doto (cell-renderer c)
      (.setBackgroundNonSelectionColor (util/color v))))
  (:selected-node-color [c _ v]
    (doto (cell-renderer c)
      (.setTextSelectionColor (util/color v))))
  (:unselected-node-color [c _ v]
    (doto (cell-renderer c)
      (.setTextNonSelectionColor (util/color v))))
  (:closed-icon [c _ v]
    (doto (cell-renderer c)
      (.setClosedIcon (util/icon v))))
  (:open-icon [c _ v]
    (doto (cell-renderer c)
      (.setOpenIcon (util/icon v))))
  (:leaf-icon [c _ v]
    (doto (cell-renderer c)
      (.setLeafIcon (util/icon v))))

  :tree-node
  (:leaf [c _ v])
  (:item [c attr item]
    (.setUserObject ^DefaultMutableTreeNode (impl c) item))
  (:info [c _ v])
  (:expanded [c _ v]))

;; Since the implementation of these events for the tree nodes
;; actually works through the tree events, there's nothing to do
;; here.
(defmethod listen [:tree-node :key] [c evt f])
(defmethod ignore [:tree-node :key] [c evt f])

(defmethod listen [:tree-node :expansion] [c evt f])
(defmethod ignore [:tree-node :expansion] [c evt f])

(defmethod listen [:tree-node :click] [c evt f])
(defmethod ignore [:tree-node :click] [c evt f])
