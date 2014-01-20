(ns lab.ui.templates
  (:require [lab.ui.core :as ui]))

(defn- close-tab-button
  [app e]
  (let [ui  (:ui @app)
        id  (-> (:source e) (ui/attr :stuff) :tab-id)
        tab (ui/find @ui (ui/selector# id))]
    (ui/update! ui (ui/parent id) ui/remove tab)))

(defn tab
  "Creates a tab with a tab header as a panel that
includes a label and a closing button."
  [app]
  (ui/with-id id
    (-> [:tab {:header [:panel {:transparent false :background 0x333333}
                          [:label {:color 0xFFFFFF}]
                            [:button {:icon         "close-tab.png"
                                      :border       :none
                                      :transparent  true
                                      :stuff        {:tab-id id}
                                      :on-click     ::close-tab-button}]]}]
      (ui/apply-stylesheet (:styles @app)))))

(defn confirm
  [title message]
  [:option-dialog {:title   title
                   :message message
                   :options :yes-no-cancel
                   :visible true}])