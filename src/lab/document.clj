(ns lab.document)

(defn document [& [path]]
  {:id (hash path) :path path})

(defn save [& xs])

(defn close [& xs])