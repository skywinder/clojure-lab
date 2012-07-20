(ns com.cleasure.ui.high-lighter
	(:import
		[javax.swing.text StyleContext SimpleAttributeSet StyleConstants]
		[java.awt Color]))

(def style-constants {
	:bold			StyleConstants/Bold, 
	:foreground		StyleConstants/Foreground, 
	:font-size		StyleConstants/FontSize,
	:font-family	StyleConstants/FontFamily})

(defn defstyle [attrs]
	(let [style (SimpleAttributeSet.)]
		(doseq [[k v] attrs]
			(. style addAttribute (k style-constants) v))
		style))

(def styles {	:keywords	(defstyle {:bold true :foreground Color/blue :font-family "Consolas" :font-size (int 14)})
				:delimiters	(defstyle {:bold true :foreground Color/red :font-family "Consolas" :font-size (int 14)})
				:default	(defstyle {:bold false :foreground Color/black :font-family "Consolas" :font-size (int 14)})})

(def keywords #{"def" "defn" "fn" "ns" "in-ns" "all-ns" "doseq"})
(def delimiters #{"(" ")" "{" "}" "[" "]"})

(def styles-map {	:keywords	keywords
					:delimiters	delimiters})

(defn all-index-of [text ptrn]
	"Finds all indexes where ptrn is matched in text and
	return a list with the intervals in which de matches are
	located."
	(let [	len (.length ptrn)]
		(loop [	start	0
				idx 	(. text indexOf ptrn start)
				idxs	[]]
			(if (= idx -1) 
				idxs
				(recur
					idx 
					(. text indexOf ptrn (+ idx len))
					(conj idxs idx))))))

(defn high-light [txt-pane]
	(let [	doc (. txt-pane getStyledDocument)
			text (. txt-pane getText)
			stripped text] ;(.. text (replace "\n" ""))]
		(. doc setCharacterAttributes 0 (. text length) (:default styles) true)
		(doseq [[s kws] styles-map]
			(doseq [kw kws]
				;(println " for " kw " found " (all-index-of stripped kw))
				(doseq [idx (all-index-of stripped kw)]
					(. doc setCharacterAttributes idx (.length kw) (s styles) true))))))
