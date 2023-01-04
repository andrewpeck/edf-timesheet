;;------------------------------------------------------------------------------
;; Deps + Constants
;;------------------------------------------------------------------------------

(ns plot-timesheets
  (:require [applied-science.darkstar :as ds]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(use '[clojure.pprint :only (pprint)])
(use '[clojure.java.shell :only [sh]])
(use '[clojure.string :only [split]])

(def plt-width 800)
(def plt-height 600)

;;------------------------------------------------------------------------------
;; Utility Functions
;;------------------------------------------------------------------------------

(defn plot! [file spec]
  (->> spec json/write-str ds/vega-lite-spec->svg (spit file)))

(defn read-tsv [file]
  (with-open
    [rdr (clojure.java.io/reader file)]
    (->> (line-seq rdr)
         (map (fn [x] (split x #"\s+")))
         (mapv vec))))

(defn map-work-row [row]
  {:Date (nth row 1) :Project (nth row 0) :Hours (nth row 2)})

(defn sum-project [project data]
  (->> (filter (fn [x] (= project (:Project x))) data)
       (map :Hours)
       (map edn/read-string)
       (reduce +)))

(defn sum-date [date data]
  (->> (filter (fn [x] (= date (:Date x))) data)
       (map :Hours)
       (map edn/read-string)
       (reduce +)))

(defn get-year [x]
  (->> (split x #"-")
       first))

(defn normalize [data]
  (->> data
       (map
        (fn [entry]
          (update entry :Hours
                  (fn [hour] (/ (edn/read-string hour)
                                (sum-date (:Date entry)
                                          data))))))))

(defn extract-data [file]
  (->> (rest (read-tsv file))
       (map map-work-row)))

;;------------------------------------------------------------------------------
;; Data
;;------------------------------------------------------------------------------

(def work-data
  "EDF workload by day"
  (extract-data "accruals.txt"))

(def work-data-by-year
  "EDF workload by year"
  (map (fn [x] (update x :Date (fn [x] (get-year x)))) work-data))

(def projects
  "List of all projects"
  (distinct (map :Project work-data)))

(def total-data
  "EDF workload summed by project (all years combined)"
  (map (fn [prj]
         {:Project prj
          :Hours  (sum-project prj work-data)}) projects ))

;;------------------------------------------------------------------------------
;; Plots Functions
;;------------------------------------------------------------------------------

(defn pie-chart [title x y data]
  "Return a vega-lite spec for a pie-chart."
  (let [oradius (/ plt-width 3)
        iradius (- oradius 40)]
    {:title title
     :width plt-width
     :height plt-height
     :config {:style {:cell {:stroke "transparent"}}}
     :data {:values data}
     :layer [{:mark {:type "arc" :outerRadius iradius}}
             {:mark {:type "text" :radius oradius :fontSize 20},
              :encoding {:text {:field x, :type "nominal"}}}]
     :encoding {:theta {:field y :type "quantitative" :stack true}
                :color {:field x :type "nominal" :legend nil}}}))

(defn bar-chart [x y data]
  "Return a vega-lite spec for a bar-chart."
  {:data {:values data}
   :mark "bar"
   :width plt-width
   :height plt-height
   :encoding {:x {:field x :type "ordinal"}
              :y {:field y :aggregate "sum" :type "quantitative"}
              :color {:field "Project" :type "nominal"}}})

;;------------------------------------------------------------------------------
;; Plots
;;------------------------------------------------------------------------------

(plot! "timesheet_pie.svg" (pie-chart "Cumulative EDF Work" "Project" "Hours" total-data))
(plot! "timesheetmonthly.svg" (bar-chart  "Date" "Hours" work-data))
(plot! "timesheetmonthlynormal.svg" (bar-chart  "Date" "Hours" (normalize work-data)))
(plot! "timesheetyearly.svg" (bar-chart  "Date" "Hours" work-data-by-year))
(plot! "timesheetyearlynormal.svg" (bar-chart  "Date" "Hours" (normalize work-data-by-year)))
