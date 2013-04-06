(remove-ns 'macho.document)
(ns macho.document
  (:refer-clojure :exclude [replace]))

(defrecord Document [name])

(defn bind
  "Binds a document to a file."
  [doc path]
  (let [text   (slurp path)
        props  {:text (StringBuffer. text)
                :path path}]
    (merge doc props)))

(defn length
  "Returns the document content's length."
  [doc]
  (.length (:text doc)))

(defn text
  "Returns the document's content."
  [doc]
  (str (:text doc)))

(defn path
  "Returns the path for the binded file if any."
  [doc]
  (:pah doc))

(defn insert
  "Inserts s at the document's offset position.
  Returns the document."
  [doc offset s]
  (.insert (:text doc) offset s)
  doc)

(defn append
  "Appends s to the document's content.
  Returns the document."
  [doc s]
  (insert doc (length doc) s))

(defn delete
  "Delete the document's content from start to end position. 
  Returns the document."
  [doc start end]
  (.delete (:text doc) start end)
  doc)

(defn add-alternate
  "Adds an alternate model to the document."
  [x alt-name alt]
  {:pre [(map? x)
         (-> x :alternates alt-name nil?)]}
  (let [alts (-> x :alternates (assoc alt-name alt))]
    (assoc x :alternates alts)))

(defn alternate
  [doc alt-name]
  (-> doc :alternates alt-name))

(defn all-alternatives
  [doc alt-name]
  (-> doc :alternates))

(defn make-document
  "Creates a new document using the name and alternate models provided."
  [doc-name & alts]
  {:pre [(not (nil? doc-name))]}
  (let [doc   (Document. doc-name)
        props {:alternates alts
               :modified   false
               :text       (StringBuffer.)}]
    (merge doc props)))

(defmacro !
  "Applies f to the atom x using the supplied arguments.
  Convenience macro."
  [f x & args]
  `(swap! ~x ~f ~@args))

(defn attach-view
  "Attaches a view to the document. x should be 
  an agent/atom/var/ref reference. (Maybe it should
  be declared in macho.view)"
  [x view]
  (view :init x)
  (add-watch x :update view))
;;---------------------------------
;; Wishful coding.
;;---------------------------------
(comment
  ;; Usage from control
  (let [doc     (make-document "bla")
        view    (default-document-view)
        control (default-document-control)]
    (attach-view doc view)
    (attach-control doc view control)
    (add-document doc)
    (add-view main-view view))
  
  ;; Handle state change from the view
  (fn [op & args] ;; Dispatch function generated by defview in macho.view
    (case op
      :init (apply init args)
      :update (apply update args)
      ,,,))
  
  ;;-----------------------------------------
  ;; Some preliminary tests.
  ;;-----------------------------------------
  (def doc (atom (with-meta (doc/Document. nil) {:doc true}) :meta {:atom true}))
  (println (meta doc) (meta @doc))
  
  (! open doc)
  (! append doc "bla")
  (! append doc " ")
  (! append doc "ble")
  
  (defn alternate [entity f]
    (if (instance? clojure.lang.Atom entity)
      nil))
    
  (alternate doc nil)
)