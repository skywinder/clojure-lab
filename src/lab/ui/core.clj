(ns lab.ui.core
  (:refer-clojure :exclude [find])
  (:require [lab.util :as util]
            [lab.ui.select :as sel]
            [lab.ui.hierarchy :as h]
            [lab.ui.protocols :as p :reload true])
  (:use [lab.ui.protocols :only [Component add children
                                 Abstract impl
                                 Implementation abstract
                                 Visible visible? hide show
                                 Selected get-selected set-selected
                                 initialize]]))

(declare init initialized?)

;; Convenience macros for multimethod implementations

(defmacro definitializations
  "Generates all the multimethod implementations
  for each of the entries in the map destrcutured
  from its args.
  
  :component-name ClassName or init-fn"
  [& {:as m}]
  `(do
      ;(remove-all-methods initialize) ; this is useful for developing but messes up the ability to break implementations into namespaces
    ~@(for [[k c] m]
      (if (-> c resolve class?)
        `(defmethod p/initialize ~k [c#]
          (new ~c))
        `(defmethod p/initialize ~k [x#]
          (~c x#))))))

(defmacro defattributes
  "Convenience macro to define attribute setters for each
  component type.
  The method implemented always returns the first argument which 
  is supposed to be the component itself.

    *attrs-declaration
  
  Where each attrs-declaration is:
 
    component-keyword *attr-declaration
    
  And each attr-declaration is:

   (attr-name [c k v] & body)"
  [& body]
  (let [comps (->> body 
                (partition-by keyword?) 
                (partition 2) 
                (map #(apply concat %)))
        f     (fn [tag & mthds]
                (for [[attr [c _ _ :as args] & body] mthds]
                  `(defmethod p/set-attr [~tag ~attr]
                    ~args 
                    ~@body 
                    ~c)))]
    `(do ~@(mapcat (partial apply f) comps))))


;; Every abstract component is represented by a Clojure map.

(extend-type clojure.lang.IPersistentMap
  Component ; Extend Clojure maps so that adding children is transparent.
  (children [this]
    (:content this))
  (add [this child]
    (let [this  (init this)
          child (init child)]
      (-> this
        (impl (add (impl this) (impl child)))
        (update-in [:content] conj child))))
  (p/remove [this child]
    (let [i (.indexOf (children this) child)]
      (-> this
        (impl (p/remove (impl this) (impl child)))
        (update-in [:content] util/remove-at i))))

  Abstract
  (impl
    ([component]
      (-> component meta :impl))
    ([component implementation]
      (vary-meta component assoc :impl implementation)))

  Visible
  (visible? [this]
    (-> this impl visible?))
  (hide [this]
    (-> this impl hide))
  (show [this]
    (-> this impl show))

  Selected
  (get-selected [this]
    (-> this impl get-selected))
  (set-selected [this selected]
    (-> this impl (set-selected (impl selected)))))

(defn component? 
  "Returns true if its arg is a component."
  [x]
  (or (and (map? x)
           (x :tag))
      (and (vector? x)
           (isa? h/hierarchy (first x) :component))))

(defn hiccup->map
  "Used to convert huiccup syntax declarations to map components.
  
  x: [tag-keyword attrs-map? children*]"
  [x]
    (if (vector? x)
      (let [[tag & [attrs & ch :as children]] x]
        {:tag     tag 
         :attrs   (if-not (component? attrs) attrs {})
         :content (mapv hiccup->map (if (component? attrs) children ch))})
      x))


(def ^:private initialized?
  "Checks if the component is initialized."
  (comp not nil? impl))

(defn- init-content
  "Initialize all content (children) for this component."
  [{content :content :as component}]
  (let [content   (map init content)
        component (assoc component :content [])]
    (reduce add component content)))

(defn- abstract-attr?
  [k]
  (-> k name first #{\-}))

(def genid
  "Generates a unique id string."
  #(name (gensym)))

(defn set-attr
  "Uses the set-attr multimethod to set the attribute value 
  for the implementation and updates the abstract component
  as well."
  [c k v]
  (let [c (if (abstract-attr? k)
            c
            (p/set-attr c k v))]
    (assoc-in c [:attrs k] v)))

(defn get-attr
  "Returns the attribute k from the component."
  [c k]
  (-> c :attrs k))

(defn- set-attrs
  "Called when initializing a component. Gets all defined
  attributes and sets their corresponding values."
  [{attrs :attrs :as component}]
  (let [f (fn [c [k v]]
            (set-attr c k (if (component? v) (init v) v)))]
    (reduce f component attrs)))

(defn init
  "Initializes a component, creating the implementation for 
  each child and the attributes that have a component as a value."
  [component]
  {:post [(component? component)]}
  (let [component (hiccup->map component)]
    (if (initialized? component) ; do nothing if it's already initiliazed
      component
      (let [ctrl       (initialize component)
            component  (-> component
                         (impl ctrl)
                         set-attrs
                         init-content)
            ctrl       (abstract ctrl component)]
        (impl component ctrl)))))

(defn find
  "Returns the first component found."
  [root selector]
  (when-let [path (sel/select root selector)]
    (get-in root path)))

(defn update
  "Updates all the components that match the selector expression
  using (update-in root path-to-component f args)."
  [root selector f & args]
  (reduce (fn [x path]
            (if (empty? path)
              (apply f x args)
              (apply update-in x path f args)))
          root
          (sel/select-all root selector)))

(defn update!
  "Same as update but assumes root is an atom." 
  [root selector f & args]
  {:pre [(instance? clojure.lang.Atom root)]}
  (apply swap! root update selector f args))

(defn add-binding
  "Adds a key binding to this component."
  [c ks f]
  (let [c (init c)
        i (impl c)]
    (impl c (p/add-binding i ks f))))

(defn remove-binding
  "Adds a key binding to this component."
  [c ks]
  (let [i (impl c)]
    (impl c (p/remove-binding i ks))))
