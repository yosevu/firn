(ns firn.markup
  (:require [hiccup.core :as h]
            [clojure.string :as s]))


;; Helpers


(defn- parse-file-link
  "FIXME move this to regex.
  AND FIXME: add a base_path into config? (with a partial?) "
  [s]
  (str "./" (-> s (s/split #":") (second) (s/split #"\.") (first))))


;; Renderers
;;
(defn- src-block->html
  [{:keys [contents language arguments] :as src-block}]
  [:pre contents])



(defn link->html
  "Parses links from the org-tree.
  Checks if a link is an HTTP link or File link.
  TODO: add regex for image files that show up as `<./link/foo.png>`"
  [v]
  (let [local-img-regex #"(file:)(.*)\.(jpg|JPG|gif|GIF|png)"
        local-img-path-regex #"(file:)(.*)\.(org)"
        local-org-regex #"(file:)(.*)\.(org)"
        http-img-regex  #"(http:\/\/|https:\/\/)(.*)\.(jpg|JPG|gif|GIF|png)"
        http-link-regex #"https?:\/\/(?![^\" ]*(?:jpg|png|gif))[^\" ]+"
        img-path        #(str (nth %  2) "." (nth % 3))
        file-path       #(str "./" (nth %  2))
        link-val        (get v :desc "missing")
        link-href       (get v :path "missing href")]
    (cond
      (re-matches local-img-regex link-href)
      [:img {:src (img-path (re-matches local-img-regex link-href))}]

      (re-matches local-org-regex link-href)
      [:a {:href (file-path (re-matches local-org-regex link-href))} link-val]

      (re-matches http-link-regex link-href)
      [:a {:href link-href} link-val]

      :default
      [:a {:href link-href} link-val])))

;; (link->html {:type "link", :path "file:test-img.png"})
;; [:img {:src "test-img.png"}]


;; (link->html {:type "link", :path "./test-img.png"})
;; [:a {:href "./test-img.png"} "missing"]

;; (link->html {:type "link",
;;              :path "https://docs.cider.mx/cider/usage/misc_features.html",
;;              :desc "Miscellaneous Features :: CIDER Docs"})


(defn- title->html
  "Constructs titles - which can have additional values (keywords, priorities, etc)
  That aren't found in the `children` values and so need special parsing."
  [v]
  (let [level      (v :level)
        typ        (v :type)
        children   (v :children)
        raw        (v :raw)
        keywrd     (v :keyword)
        priority   (v :priority)
        value      (v :value)
        h-level    (case level 1 :h1 2 :h2 3 :h3 4 :h4 5 :h5 :h6)
        make-child #(into [%] (map title->html children))]
    (case typ
      "headline"  (make-child :div)
      "title"     [h-level
                   [:span.heading-keyword (str keywrd " ")]
                   [:span.heading-priority (str priority " ")]
                   (make-child :span)]
      "text"      [:span value]
      "cookie"    [:span.heading-cookie value]
      "timestamp" [:span.heading-teimstamp value]
      "code"      [:code value]
      "verbatim"  [:code value]
      "link"      (link->html v)
      "underline" (make-child :i)
      "italic"    (make-child :em)
      "bold"      (make-child :strong)
      [:span " - ERR! Val Missing -"])))

(defn to-html
  "Recursively Parses the org-tree tree-seq into hiccup.
  Some values don't get parsed (drawers) - yet. They return empty strings.
  Don't destructure! - it can create uneven maps from possible nil vals on `V`"
  [v]
  (let [type       (v :type)
        children   (v :children)
        value      (v :value)
        ordered    (v :ordered) ;; for lists
        val        (if value (s/trim-newline value) value)
        make-child #(into [%] (map to-html children))]
    (case type
      "document"      (make-child :main)
      "headline"      (make-child :div)
      "title"         (title->html v)
      "section"       (make-child :section)
      "paragraph"     (make-child :p)
      "underline"     (make-child :i)
      "italic"        (make-child :em)
      "bold"          (make-child :strong)
      "list"          (make-child (if ordered :ol :ul))
      "list-item"     (make-child :li)
      "quote-block"   (make-child :div.quote-block)
      "table"         (make-child :table)
      "table-row"     (make-child :tr)
      "table-cell"    (make-child :td)
      "source-block"  (src-block->html v)
      "link"          (link->html v)
      "code"          [:code val]
      "verbatim"      [:code val]
      "rule"          [:hr]
      "cookie"        [:span.cookie val]
      "text"          [:span val]
      "timestamp"     [:span val] ;; TODO html constructor.
      "keyword"       ""          ;; Don't parse
      "comment-block" ""          ;; Don't parse
      "drawer"        ""          ;; Don't parse
      ;; default value.
      [:span (str "{missing type!}!!" type " val is " value)]))

  (defn template
    [org-tree]
    (h/html [:html
             [:head
              [:link {:rel "stylesheet" :href "./assets/styles/main.css"}]]
             [:body {} (h/html (to-html org-tree))]])))
