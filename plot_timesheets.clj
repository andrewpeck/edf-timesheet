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

(def plt-width 800)
(def plt-height 600)

(defn plot! [file spec]
  (->> spec json/write-str ds/vega-lite-spec->svg (spit file)))

(defn read-tsv [file]
  (with-open
    [rdr (clojure.java.io/reader file)]
    (->> (line-seq rdr)
         (map (fn [x] (str/split x #"\s+")))
         (mapv vec))))

(defn map-work-row [row]
  {:Date (nth row 1) :Project (nth row 0) :Hours (nth row 2)})

(defn sum-project [project]
  (->>
   (filter (fn [x] (= project (:Project x))) tsv)
   (map :Hours)
   (map edn/read-string)
   (reduce +)))

(def tsv (->> (read-tsv  "accruals.txt")
              rest
              (map map-work-row)))

(def projects
  "List of all projects"
  (distinct (map :Project tsv)))

(def pie-data
  (map (fn [prj]
         {:Project prj
          :Hours  (sum-project prj)}) projects ))

(plot! "timesheet_pie.svg"
       (let [oradius (/ plt-width 3)
             iradius (- oradius 40)]
         {:title "Cumulative EDF Work"
          :width plt-width
          :height plt-height
          :config {:style {:cell {:stroke "transparent"}}}
          :data {:values pie-data}
          :layer [{:mark {:type "arc" :outerRadius iradius}}
                  {:mark {:type "text" :radius oradius :fontSize 20},
                   :encoding {:text {:field "Project", :type "nominal"}}}]
          :encoding {:theta {:field "Hours" :type "quantitative" :stack true}
                     :color {:field "Project" :type "nominal" :legend nil}}}))

(plot! "timesheettotals.svg"
       {:data {:values tsv}
        :mark "bar"
        :width plt-width
        :height plt-height
        :encoding {:x {:field "Date"
                       :type "ordinal"}
                   :y {:field "Hours"
                       :aggregate "sum"
                       :type "quantitative"}
                   :color {:field "Project"
                           :type "nominal"}}})
