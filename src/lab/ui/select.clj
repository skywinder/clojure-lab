(ns lab.ui.select
  (:refer-clojure :exclude [compile])
  (:require [clojure.zip :as zip]
            [clojure.set :as set]))

;; Selector functions

(defn tag=
  "Returns a predicate that indicates whether its 
  argument has the specified tag name."
  [tag]
  (with-meta #(= tag (:tag %)) {:tag tag}))

(defn id=
  "Returns a predicate that indicates wheter its
  argument has the same id as the provided."
  [id]
  (with-meta #(= id (-> % :attrs :id)) {:id id}))

(defn attr= [attr v]
  "Returns a predicate that indicates whether its
  argument has the value provided in the attribute specified."
  (with-meta
    #(-> % :attrs attr (= v))
    {:attr attr :value v}))

(defn attr? [attr]
  "Returns a predicate that indicates whether its
  argument has a truthy value in the attribute specified."
  (with-meta
    #(-> % :attrs attr)
    {:attr attr}))

;; Parsing functions

(defn- literal-selector?
  "Indicates whether x is a literal selector (keyword or string), 
  as opposed to a function selector, by returning (name x) if it 
  is or false otherwise"
  [x]
  (and (or (string? x) (keyword? x)) 
       (name x)))

(defn- id?
  "Returns true if the string begins with a
hash (#) sign which indicates its an id selector."
  [s]
  (when-let [[x & _] (literal-selector? s)]
    (= x \#)))

(defn- tag?
  "Returns true if the string doesn't begin with a
hash (#)."
  [s]
  (when-let [[x & _] (literal-selector? s)]
    (not= x \#)))

(defn- parse
  "Takes a selector (keyword) and parses it identifying
its type and value, returning it in a vector.
For example:
  :#main [:id \"main\"]
  :label [:tag :label]"
  [s]
  (cond (id? s)
          [:id (->> s name rest (apply str))]
        (tag? s)
          [:tag s]
        (fn? s)
          [:fn s]))

;; Path finding

(defn- compile
  "Takes a selector and returns a single arg predicate."
  [selector]
  (if (sequential? selector)
    ; Conjunction predicate
    (let [predicates (map compile selector)]
      (fn [x]
        (reduce #(and % (%2 x)) true predicates)))
    ; Simple predicate
    (let [[t v] (parse selector)]
      (condp = t
        :id  (id= v)
        :tag (tag= v)
        :fn  v))))

(def memoized-compile (memoize compile))

(defn- find-path
  "Returns the path to the child component that satisfies (pred component)."
  [pred component]
  (if (pred component)
    []
    (if-let [path (->> component
                    :content
                    (map-indexed #(vector %1 (find-path pred %2)))
                    (filter second)
                    first)]
      (-> [:content] (concat path) flatten vec))))

(defn- chain
  "Reducer function used for applying selectors (pred) in a chain.
  Uses find-path on root to get the next level in the of the path."
  [[path root] pred]
  (let [path' (find-path pred root)
        path  (when path' (concat path path'))
        root  (get-in root path)]
    [path root]))

(defn select
  "Takes a selection expression and returns the path for the
  first matching component from root, which must be a component.
  
  The format of the selector mimics enlive selectors, the following
  is the syntax for each type of selector:
  
  Id          :#value-id
  Tag         :tag-name
  Unary pred  (fn [c] true)"
  [root selector]
  (when selector
    (let [selector   (if (sequential? selector) selector [selector])
          predicates (map memoized-compile selector)
          [path _]   (if (-> predicates count pos?)
                       (reduce chain [nil root] predicates)
                       [[] nil])]
      path)))

(defn- path-from-root
  "Takes a node from a zipper and finds the vector path
  to it by traversing the tree backwards."
  [node]
  (loop [path []
         node node]
    (if-let [parent (zip/up node)]
      (recur (into [:content (-> node zip/lefts count)] path)
             parent)
        path)))

(defn- parents-match? [node [p & preds]]
  (if p
    (if (and node (p (zip/node node)))
      (recur (zip/up node) preds)
      false)
    true))

(defn- find-all-paths
  "Traverses the tree using a zipper and merging the results
  in a map where the component is the key and the zipper node
  is the value."
  [node preds]
  (loop [node                node
         [p & ps :as preds]  (reverse preds)
         result              #{}]
     (if (and p (not (zip/end? node)))
       (let [x      (zip/node node)
             result (if (and (p x) (parents-match? (zip/up node) ps))
                      (conj result (path-from-root node))
                      result)]
         (recur (zip/next node) preds result))
       result)))

(defn select-all
  "Searches the whole component tree from the root and returns
  a sequence of paths to matches."
  [root selector]
  (when selector
    (let [selector   (if (sequential? selector) selector [selector])
          predicates (map memoized-compile selector)
          root       (zip/zipper map? :content identity root)
          result     (if (-> predicates count pos?)
                       (find-all-paths root predicates)
                       #{[]})]
      result)))
  