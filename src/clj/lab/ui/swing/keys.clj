(ns lab.ui.swing.keys
  (:import java.awt.event.KeyEvent))

(def swing-keys
{KeyEvent/VK_O :o,
 KeyEvent/VK_EURO_SIGN :euro_sign,
 KeyEvent/VK_N :n,
 KeyEvent/VK_CANCEL :cancel,
 KeyEvent/VK_MULTIPLY :multiply,
 KeyEvent/VK_ALT_GRAPH :alt_graph,
 KeyEvent/VK_HIRAGANA :hiragana,
 KeyEvent/VK_ALL_CANDIDATES :all_candidates,
 KeyEvent/VK_KANJI :kanji,
 KeyEvent/VK_RIGHT :right,
 KeyEvent/VK_CUT :cut,
 KeyEvent/VK_Q :q,
 KeyEvent/VK_PROPS :props,
 KeyEvent/VK_MINUS :minus,
 KeyEvent/VK_DEAD_CIRCUMFLEX :dead_circumflex,
 KeyEvent/VK_KP_RIGHT :kp_right,
 KeyEvent/VK_DOWN :down,
 KeyEvent/VK_P :p,
 KeyEvent/VK_JAPANESE_ROMAN :japanese_roman,
 KeyEvent/VK_FULL_WIDTH :full_width,
 KeyEvent/VK_J :j,
 KeyEvent/VK_DELETE :delete,
 KeyEvent/VK_F1 :f1,
 KeyEvent/VK_JAPANESE_HIRAGANA :japanese_hiragana,
 KeyEvent/VK_TAB :tab,
 KeyEvent/VK_PAGE_UP :page_up,
 KeyEvent/VK_K :k,
 KeyEvent/VK_DEAD_SEMIVOICED_SOUND :dead_semivoiced_sound,
 KeyEvent/VK_COMPOSE :compose,
 KeyEvent/VK_L :l,
 KeyEvent/VK_DEAD_CARON :dead_caron,
 KeyEvent/VK_F3 :f3,
 KeyEvent/VK_ADD :add,
 KeyEvent/VK_DIVIDE :divide,
 KeyEvent/VK_M :m,
 KeyEvent/VK_PASTE :paste,
 KeyEvent/VK_RIGHT_PARENTHESIS :right_parenthesis,
 KeyEvent/VK_F2 :f2,
 KeyEvent/VK_NUMBER_SIGN :number_sign,
 KeyEvent/VK_DEAD_CEDILLA :dead_cedilla,
 KeyEvent/VK_F6 :f6,
 KeyEvent/VK_JAPANESE_KATAKANA :japanese_katakana,
 KeyEvent/VK_F20 :f20,
 KeyEvent/VK_BACK_SLASH :back_slash,
 KeyEvent/VK_NONCONVERT :nonconvert,
 KeyEvent/VK_Y :y,
 KeyEvent/VK_PREVIOUS_CANDIDATE :previous_candidate,
 KeyEvent/VK_UP :up,
 KeyEvent/VK_DEAD_DIAERESIS :dead_diaeresis,
 KeyEvent/VK_NUMPAD1 :numpad1,
 KeyEvent/VK_BRACELEFT :braceleft,
 KeyEvent/VK_META :meta,
 KeyEvent/VK_DEAD_OGONEK :dead_ogonek,
 KeyEvent/VK_F7 :f7,
 KeyEvent/VK_F21 :f21,
 KeyEvent/VK_PLUS :plus,
 KeyEvent/VK_X :x,
 KeyEvent/VK_HALF_WIDTH :half_width,
 KeyEvent/VK_NUMPAD2 :numpad2,
 KeyEvent/VK_BACK_QUOTE :back_quote,
 KeyEvent/VK_F19 :f19,
 KeyEvent/VK_DEAD_TILDE :dead_tilde,
 KeyEvent/VK_W :w,
 KeyEvent/VK_F4 :f4,
 KeyEvent/VK_F5 :f5,
 KeyEvent/VK_FINAL :final,
 KeyEvent/VK_DEAD_BREVE :dead_breve,
 KeyEvent/VK_BRACERIGHT :braceright,
 KeyEvent/VK_STOP :stop,
 KeyEvent/VK_NUMPAD0 :numpad0,
 KeyEvent/VK_V :v,
 KeyEvent/VK_PAUSE :pause,
 KeyEvent/VK_INVERTED_EXCLAMATION_MARK :inverted_exclamation_mark,
 KeyEvent/VK_F15 :f15,
 KeyEvent/VK_T :t,
 KeyEvent/VK_ALT :alt,
 KeyEvent/VK_NUMPAD6 :numpad6,
 KeyEvent/VK_BEGIN :begin,
 KeyEvent/VK_ESCAPE :escape,
 KeyEvent/VK_F24 :f24,
 KeyEvent/VK_U :u,
 KeyEvent/VK_F16 :f16,
 KeyEvent/VK_AMPERSAND :ampersand,
 KeyEvent/VK_NUMPAD5 :numpad5,
 KeyEvent/VK_DEAD_GRAVE :dead_grave,
 KeyEvent/VK_LEFT_PARENTHESIS :left_parenthesis,
 KeyEvent/VK_CLOSE_BRACKET :close_bracket,
 KeyEvent/VK_F9 :f9,
 KeyEvent/VK_KANA_LOCK :kana_lock,
 KeyEvent/VK_F23 :f23,
 KeyEvent/VK_F17 :f17,
 KeyEvent/VK_DEAD_DOUBLEACUTE :dead_doubleacute,
 KeyEvent/VK_KP_LEFT :kp_left,
 KeyEvent/VK_DEAD_ABOVEDOT :dead_abovedot,
 KeyEvent/VK_AT :at,
 KeyEvent/VK_PAGE_DOWN :page_down,
 KeyEvent/VK_NUMPAD4 :numpad4,
 KeyEvent/VK_R :r,
 KeyEvent/VK_QUOTEDBL :quotedbl,
 KeyEvent/VK_F8 :f8,
 KeyEvent/VK_F22 :f22,
 KeyEvent/VK_EXCLAMATION_MARK :exclamation_mark,
 KeyEvent/VK_LESS :less,
 KeyEvent/VK_F18 :f18,
 KeyEvent/VK_KATAKANA :katakana,
 KeyEvent/VK_S :s,
 KeyEvent/VK_NUMPAD3 :numpad3,
 KeyEvent/VK_9 :9,
 KeyEvent/VK_PRINTSCREEN :printscreen,
 KeyEvent/VK_ENTER :enter,
 KeyEvent/VK_INPUT_METHOD_ON_OFF :input_method_on_off,
 KeyEvent/VK_8 :8,
 KeyEvent/VK_DEAD_IOTA :dead_iota,
 KeyEvent/VK_F10 :f10,
 KeyEvent/VK_UNDERSCORE :underscore,
 KeyEvent/VK_NUM_LOCK :num_lock,
 KeyEvent/VK_END :end,
 KeyEvent/VK_UNDO :undo,
 KeyEvent/VK_CIRCUMFLEX :circumflex,
 KeyEvent/VK_BACK_SPACE :back_space,
 KeyEvent/VK_SEMICOLON :semicolon,
 KeyEvent/VK_KP_DOWN :kp_down,
 KeyEvent/VK_SHIFT :shift,
 KeyEvent/VK_7 :7,
 KeyEvent/VK_CONTROL :control,
 KeyEvent/VK_WINDOWS :windows,
 KeyEvent/VK_DOLLAR :dollar,
 KeyEvent/VK_6 :6,
 KeyEvent/VK_CONTEXT_MENU :context_menu,
 KeyEvent/VK_NUMPAD7 :numpad7,
 KeyEvent/VK_COLON :colon,
 KeyEvent/VK_4 :4,
 KeyEvent/VK_F14 :f14,
 KeyEvent/VK_NUMPAD8 :numpad8,
 KeyEvent/VK_SUBTRACT :subtract,
 KeyEvent/VK_F13 :f13,
 KeyEvent/VK_KP_UP :kp_up,
 KeyEvent/VK_5 :5,
 KeyEvent/VK_COMMA :comma,
 KeyEvent/VK_OPEN_BRACKET :open_bracket,
 KeyEvent/VK_MODECHANGE :modechange,
 KeyEvent/VK_NUMPAD9 :numpad9,
 KeyEvent/VK_F12 :f12,
 KeyEvent/VK_A :a,
 KeyEvent/VK_2 :2,
 KeyEvent/VK_DEAD_ABOVERING :dead_abovering,
 KeyEvent/VK_HOME :home,
 KeyEvent/VK_F11 :f11,
 KeyEvent/VK_INSERT :insert,
 KeyEvent/VK_SPACE :space,
 KeyEvent/VK_FIND :find,
 KeyEvent/VK_AGAIN :again,
 KeyEvent/VK_3 :3,
 KeyEvent/VK_Z :z,
 KeyEvent/VK_D :d,
 KeyEvent/VK_GREATER :greater,
 KeyEvent/VK_LEFT :left,
 KeyEvent/VK_E :e,
 KeyEvent/VK_SEPARATOR :separator,
;; KeyEvent/VK_SEPARATER :separater, ;; This has the same code as the above
 KeyEvent/VK_DECIMAL :decimal,
 KeyEvent/VK_ACCEPT :accept,
 KeyEvent/VK_1 :1,
 KeyEvent/VK_B :b,
 KeyEvent/VK_QUOTE :quote,
 KeyEvent/VK_DEAD_VOICED_SOUND :dead_voiced_sound,
 KeyEvent/VK_ALPHANUMERIC :alphanumeric,
 KeyEvent/VK_DEAD_MACRON :dead_macron,
 KeyEvent/VK_UNDEFINED :undefined,
 KeyEvent/VK_KANA :kana,
 KeyEvent/VK_DEAD_ACUTE :dead_acute,
 KeyEvent/VK_0 :0,
 KeyEvent/VK_SCROLL_LOCK :scroll_lock,
 KeyEvent/VK_SLASH :slash,
 KeyEvent/VK_C :c,
 KeyEvent/VK_I :i,
 KeyEvent/VK_COPY :copy,
 KeyEvent/VK_CAPS_LOCK :caps_lock,
 KeyEvent/VK_H :h,
 KeyEvent/VK_HELP :help,
 KeyEvent/VK_PERIOD :period,
 KeyEvent/VK_CODE_INPUT :code_input,
 KeyEvent/VK_G :g,
 KeyEvent/VK_ROMAN_CHARACTERS :roman_characters,
 KeyEvent/VK_CLEAR :clear,
 KeyEvent/VK_F :f,
 KeyEvent/VK_CONVERT :convert,
 KeyEvent/VK_EQUALS :equals,
 KeyEvent/VK_ASTERISK :asterisk})

(comment
;; Code used to generate the above map.
(->> KeyEvent 
  .getFields 
  seq
  (filter #(.startsWith (.getName %) "VK_"))
  (map #(vector (->> (.getName %) (str "KeyEvent/") symbol)
        (-> % .getName (.replace "VK_" "") .toLowerCase keyword)))
  (sort-by first)
  (into {})
  clojure.pprint/pprint)

;; Code used to figure out repeated key.
(->> KeyEvent 
  .getFields 
  seq
  (filter #(.startsWith (.getName %) "VK_"))
  (map #(->> (.getName %) (str "KeyEvent/") symbol))
  (map eval)
  frequencies
  (filter #(-> % second (> 1)))
  clojure.pprint/pprint)

)