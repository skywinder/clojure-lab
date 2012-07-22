(ns com.cleasure.ui.text.undo-redo
	(:import 
		[javax.swing.undo UndoManager AbstractUndoableEdit]
		[javax.swing.text DefaultStyledDocument$AttributeUndoableEdit]
		[javax.swing.event UndoableEditListener]))

(defn on-undoable [doc undo-mgr]
	(let [handler (proxy [UndoableEditListener] []
			(undoableEditHappened [e]
				(let [edit (.getEdit e)]
					(when-not (= "style change" (.getPresentationName edit))
						(. undo-mgr addEdit edit)))))]
		(. doc addUndoableEditListener handler)))