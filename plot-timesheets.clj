;;------------------------------------------------------------------------------
;; Deps + Constants
;;------------------------------------------------------------------------------

(ns plot-timesheets
  (:require [applied-science.darkstar :as ds]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]

            ;; [clojure.pprint :only (pprint)]
            ;; [clojure.java.shell :only [sh]]
            ))

(def plt-width 800)
(def plt-height 600)

;;------------------------------------------------------------------------------
;; Utility Functions
;;------------------------------------------------------------------------------

(defn to-float [s]
  (try (Float/parseFloat s)
       (catch NumberFormatException _ false)))


(defn plot! [file spec]
  (->> spec json/write-str ds/vega-lite-spec->svg (spit file)))

(defn read-tsv [file]
  (with-open
    [rdr (io/reader file)]
    (->> (line-seq rdr)
         (map (fn [x] (str/split x #"\s+")))
         (mapv vec))))

(defn read-csv [file]
  (with-open
    [rdr (io/reader file)]
    (->> (line-seq rdr)
         (map (fn [x] (str/split x #"\s*,\s*")))
         (mapv vec))))

(defn map-work-row [row]
  {:Date (nth row 1) :Project (nth row 0) :Hours (nth row 2)})

(defn sum-project [project data]
  (->> (filter (fn [x] (= project (:Project x))) data)
       (map :Hours)
       (map edn/read-string)
       (reduce +)))

(defn sum-key
  "e.g. sum-key :Date '2021-01'"
  [key val data]
  (->> (filter (fn [x] (= val (key x))) data)
       (map :Hours)
       (reduce +)))

(defn sum-weekday [weekday data]
  (->> (filter (fn [x] (= weekday (:Day x))) data)
       (map :Hours)
       (reduce +)))
(defn sum-date [date data]
  (->> (filter (fn [x] (= date (:Date x))) data)
       (map :Hours)
       (map edn/read-string)
       (reduce +)))

(defn get-year [x]
  (first (str/split x #"-")))

(defn normalize [data]
  (letfn [(updater [entry]
            (fn [hour] (/ (edn/read-string hour)
                          (sum-date (:Date entry) data))))
          (normalize-row [entry]
            (update entry :Hours (updater entry)))]

    (map normalize-row data)))

(defn extract-data [file]
  (->> (rest (read-tsv file))
       (map map-work-row)))

(defn get-csv-file-names []
  (->> (mapv str (filter #(.isFile %) (file-seq (clojure.java.io/file "csv/"))))
       (filter #(re-matches #"csv/[0-9]{4}-[0-9]{2}.csv" %))))

(defn slurp-timesheet [fname]
  (let [split (str/split fname #"[^A-z0-9]")
        year (Integer/parseInt (nth split 1))
        month (Integer/parseInt (nth split 2))]
    (->> fname
         read-csv
         (remove empty?)
         (map (fn [row]
                  {:Date (edn/read-string (nth row 0 ""))
                   :Time (nth row 1 "")
                   :Project (nth row 2 "")
                   :Task (nth row 3 "")
                   :Day (nth row 4 "")
                   :Hours (to-float (nth row 5 ""))}))
         (filter (fn [row]
                   (and
                    (not (str/blank? (:Project row)))
                    (not (str/blank? (:Time row)))
                    (number? (:Hours row))
                    (number? (:Date row)))))
         (map (fn [row] (update row :Date #(format "%04d-%02d-%02d" year month %)))))))

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

(def all-work-data
  "All the work data directly from CSV"
    (apply concat (mapv slurp-timesheet (get-csv-file-names))))

(def data-by-weekday
  "Data binned by day of the week"
  (map (fn [weekday]
         {:Day weekday
          :Hours  (sum-weekday weekday all-work-data)})
       ["MON" "TUE" "WED" "THU" "FRI" "SAT" "SUN"] ))

;;------------------------------------------------------------------------------
;; Plots Functions
;;------------------------------------------------------------------------------

(defn pie-chart "Return a vega-lite spec for a pie-chart."
  [title x y data]

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

(defn bar-chart "Return a vega-lite spec for a bar-chart."
  [x y data]

  {:data {:values data}
   :mark "bar"
   :width plt-width
   :height plt-height
   :encoding {:x {:field x :type "ordinal"}
              :y {:field y :aggregate "sum" :type "quantitative"}
              :color {:field "Project" :type "nominal"}}})

(defn bar-chart-day "Return a vega-lite spec for a bar-chart."
  [x y data]
  {:data {:values data}
   :mark "bar"
   :width plt-width
   :height plt-height
   :encoding {:x {:field x
                  :type "ordinal"
                  :sort ["MON" "TUE" "WED" "THU" "FRI" "SAT" "SUN"]}
              :y {:field y :aggregate "sum" :type "quantitative"}}})

;;------------------------------------------------------------------------------
;; Plots
;;------------------------------------------------------------------------------

(plot! "timesheetdaily.svg" (bar-chart-day  "Day" "Hours" data-by-weekday))
(plot! "timesheet_pie.svg" (pie-chart "EDF Work" "Project" "Hours" total-data))
(plot! "timesheetday.svg" (bar-chart  "Date" "Hours" work-data))
(plot! "timesheetmonthly.svg" (bar-chart  "Date" "Hours" work-data))
(plot! "timesheetmonthlynormal.svg" (bar-chart  "Date" "Hours" (normalize work-data)))
(plot! "timesheetyearly.svg" (bar-chart  "Date" "Hours" work-data-by-year))
(plot! "timesheetyearlynormal.svg" (bar-chart  "Date" "Hours" (normalize work-data-by-year)))
