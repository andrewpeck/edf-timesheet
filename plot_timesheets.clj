(ns plot-timesheets
  (:require [applied-science.darkstar :as ds]
            [clojure.data.json :as json]
            [clojure.data.csv :as csv]
            [clojure.edn :as edn]
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

(def tsv (->> (read-tsv  "accruals.txt")
              rest
              (map map-work-row)))

(def projects
  "List of all projects"
  (distinct (map :Project tsv)))

(defn sum-project [project]
  (->>
   (filter (fn [x] (= project (:Project x))) tsv)
   (map :Hours)
   (map edn/read-string)
   (reduce +)))

(def pie-data
  '({:category 1 :value 4}
    {:category 2 :value 6}
    {:category 3 :value 10}
    {:category 4 :value 3}
    {:category 5 :value 7}
    {:category 6 :value 8}))

(println pie-data)

(def pie-data
  (map (fn [prj]
         {:Project prj
          :Hours  (sum-project prj)}) projects ))

(println pie-data)


(plot! "pie.svg"
  {:description "A simple pie chart with embedded data." ,
   :width 800
   :height 600
   :data {:values pie-data}
   :mark "arc"
   ;; :mark {:type "text" :radius 90}
   ;; :layer {:mark {:type "arc" :outerRadius 80}}
   :encoding {
              :theta {:field "Hours"
                      ;; :stack "true"
                      :type "quantitative"}
              :color {:field "Project"
                      :type "nominal"}}})

(plot! "timesheettotals.svg"
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
