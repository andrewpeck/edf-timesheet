(ns plot-timesheets
  (:require [applied-science.darkstar :as ds]
            [clojure.data.json :as json]
            [clojure.data.csv :as csv]
            [clojure.string :as str]))

;; at the REPL
(use 'clojure.string)
(use '[clojure.pprint :only (pprint)])
(use '[clojure.java.shell :only [sh]])

(defn plot! [file spec]
  (->> spec
       json/write-str
       ds/vega-lite-spec->svg
       (spit file)))

(defn read-tsv [file]
  (with-open
    [rdr (clojure.java.io/reader file)]
    (->> (line-seq rdr)
         (map (fn [x] (str/split x #"\s+")))
         (mapv vec))))

(defn map-work-row [row]
  {:Date (nth row 1)
   :Project (nth row 0)
   :Hours (nth row 2)})

(def tsv
  (->>
   (read-tsv  "accruals.txt")
   rest
   (map map-work-row)))

(def stacked-bar
  {:data {:values tsv}
   :mark "bar"
   :width 800
   :height 600
   :encoding {:x {:field "Date"
                  :type "ordinal"}
              :y {:field "Hours"
                  :aggregate "sum"
                  :type "quantitative"}
              :color {:field "Project"
                      :type "nominal"}}})

(plot! "timesheettotals.svg" stacked-bar)
