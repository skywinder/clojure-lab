(ns lab.ui.core
  "Provides the API to create and manipulate UI component 
abstractions as Clojure records. Components can be declared as 
maps or in a hiccup format. Existing tags are defined in an ad-hoc 
hierarchy which can be extended as needed.

Implementation of components is based on the definitialize and defattribute
multi-methods. The former should return an instance of the underlying UI object,
while the latter is used for setting its attributes' value defined in the 
abstract specification (or explicitly through the use of `attr`).

Example: the following code creates a 300x400 window with a \"Hello!\" button
         and shows it on the screen.

  (-> [:window {:size [300 400]} [:button {:text \"Hello!\"}]]
    init
    show)"
  (:refer-clojure :exclude [find remove])
  (:require [lab.util :as util]
            [lab.ui.protocols :as p]
            [lab.ui.select :as sel]
            [lab.ui.hierarchy :as h]))

(declare init initialized? attr)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Convenience macros for multimethod implementations

(defmacro definitializations
  "Generates all the multimethod implementations
for each of the entries in the map destrcutured
from its args.
  
  :component-name ClassName or init-fn"
  [& {:as m}]
  `(do
    ~@(for [[k c] m]
      (if (and (not (seq? c)) (-> c resolve class?))
        `(defmethod p/initialize ~k [c#]
          (new ~c))
        `(defmethod p/initialize ~k [x#]
          (~c x#))))))

(defmacro defattributes
  "Convenience macro to define attribute setters for each component type. 

The method implemented returns the first argument (which is the component 
itself), UNLESS the `^:modify` metadata flag is true for the argument vector, 
in which case the value from the last expression in the body is returned.

  *attrs-declaration
  
Where each attrs-declaration is:

  component-tag *attr-declaration
    
And each attr-declaration is:

  (attr-name [c attr v] & body)"
  [& body]
  (let [comps (->> body 
                (partition-by keyword?) 
                (partition 2) 
                (map #(apply concat %)))
        f     (fn [tag & mthds]
                (for [[attr [c _ _ :as args] & body] mthds]
                  (let [x (gensym)]
                    `(defmethod p/set-attr [~tag ~attr]
                      ~args
                      (let [~x (do ~@body)]
                        ~(if (-> args meta :modify) x c))))))]
    `(do ~@(mapcat (partial apply f) comps))))

(defn- update-abstraction
  "Takes a component and set its own value as the abstraction
of its implementation."
  [c]
  (let [impl (p/abstract (p/impl c) c)]
    (p/impl c impl)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Abstract UI Component record

(defrecord UIComponent [tag attrs content]
  p/Abstract
  (impl [component]
    (-> component meta :impl))
  (impl [component implementation]
    (vary-meta component assoc :impl implementation))

  p/Selected
  (selected [this]
    (p/selected (p/impl this)))
  (selected [this selected]
    (p/selected (p/impl this) (p/impl selected)))
  
  p/Text
  (text [this]
    (p/text (p/impl this)))
  (apply-style [this start length style]
    (p/apply-style (p/impl this) start length style)))

;; Have to use this since remove is part of the java.util.Map interface.
(extend-type UIComponent
  p/Component 
  (children [this]
    (:content this))
  (add [this child]
    (let [this  (init this)
          child (init child)]
      (-> this
        (update-in [:content] conj child)
        update-abstraction
        (p/impl (p/add (p/impl this) (p/impl child))))))
  (remove [this child]
    (let [i (.indexOf ^java.util.List (p/children this) child)]
      (-> this
        (update-in [:content] util/remove-at i)
        update-abstraction
        (p/impl (p/remove (p/impl this) (p/impl child))))))
  (add-binding [this ks f]
    (let [this    (init this)
          implem  (p/impl this)]
      (p/impl this (p/add-binding implem ks f))))
  (remove-binding [this ks]
    (let [implem (p/impl this)]
      (p/impl this (p/remove-binding implem ks)))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Expose Protocol Functions

(defn children [c] (p/children c))
(defn add [c child] (p/add c child))
(defn remove [c child] (p/remove c child))

(defn text [c] (p/text c))
(defn apply-style [c start length style]
  (p/apply-style c start length style))

(defn selected
  ([c] (p/selected c))
  ([c s] (p/selected c s)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private supporting functions

(defn- component?
  "Returns true if its arg is a component."
  [x]
  (or (instance? UIComponent x)
      (and (vector? x)
           (isa? h/hierarchy (first x) :component))))

(defn- hiccup->component
  "Used to convert huiccup syntax declarations to map components.
x should be a vector with the content [tag-keyword attrs-map? children*]"
  [x]
    (if (vector? x)
      (let [[tag & [attrs & ch :as children]] x]
        (->UIComponent
          tag 
          (if-not (component? attrs) attrs {})
          (mapv hiccup->component (if (component? attrs) children ch))))
      (update-in x [:content] (partial mapv hiccup->component))))

(def ^:private initialized?
  "Checks if the component is initialized."
  (comp not nil? p/impl))

(defn- init-content
  "Initialize all content (children) for this component."
  [{content :content :as component}]
  (let [content   (map init content)
        component (assoc component :content [])]
    (reduce p/add component content)))

(defn- set-attrs
  "Called when initializing a component. Gets all defined
attributes and sets their corresponding values."
  [{attrs :attrs :as component}]
  (let [f (fn [c [k v]]
            (attr c k (if (component? v) (init v) v)))]
    (reduce f component attrs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialization and Attributes Access

(defn attr
  "Uses the set-attr multimethod to set the attribute value 
for the implementation and updates the abstract component
as well."
  ([c k]
    (if-let [v (get-in c [:attrs k])]
      v
      nil #_(p/get-attr c k)))
  ([c k v]
    (-> c
      (assoc-in [:attrs k] v)
      (p/set-attr k v)
      update-abstraction)))

(defn init
  "Initializes a component, creating the implementation for 
each child and the attributes that have a component as a value."
  [c]
  {:post [(component? c)]}
  (let [c (hiccup->component c)]
    (if (initialized? c) ; do nothing if it's already initiliazed
      c
      (-> c
        (p/impl (p/initialize c))
        set-attrs
        init-content
        update-abstraction))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Finding and Updating

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utils

(def genid
  "Generates a unique id string."
  #(name (gensym)))

(defmacro with-id
  "Assigns a unique id to the component which can be
used in the component's definition (e.g. in event handlers)."
  [x component]
  `(let [~x (genid)]
    (assoc-in (#'lab.ui.core/hiccup->component ~component) [:attrs :id] ~x)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Stylesheet

(defn- apply-class
  [ui [selector attrs]]
  (reduce (fn [ui [attr-name value]]
            (update ui selector attr attr-name value))
          ui
          attrs))

(defn apply-stylesheet
  "Takes an atom with the root of a (initialized abstract UI) component and
  a stylesheet (map where the key is a selector and the values a map of attributes
  and values) and applies it to the matching components."
  [ui stylesheet]
  (reduce apply-class ui stylesheet))
