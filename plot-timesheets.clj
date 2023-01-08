;;------------------------------------------------------------------------------
;; Deps + Constants
;;------------------------------------------------------------------------------

(ns plot-timesheets
  (:require [applied-science.darkstar :as ds]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            ;; [clojure.set :as set]
            ;; [java-time.api :as jt]
            ;; [clojure.pprint :only (pprint)]
            ;; [clojure.java.shell :only [sh]]
            ))
;; (jt/as (jt/local-date "yyyy-MM-dd" "2015-09-28") :year)

;;------------------------------------------------------------------------------
;; Constants
;;------------------------------------------------------------------------------

(def plt-width 800)
(def plt-height 600)

;;------------------------------------------------------------------------------
;; Utility Functions
;;------------------------------------------------------------------------------

(defn to-float
  "Convert a string to float. Returns nil if the conversion fails."
  [s] (try (Float/parseFloat s)
           (catch NumberFormatException _ false)))

(defn to-int
  "Convert a string to integer. Returns nil if the conversion fails."
  [s] (try (Integer/parseInt s)
           (catch NumberFormatException _ false)))

(defn plot!
  "Plot a vega-lite SPEC and save it as svg to the provided FILE"
  [file spec]
  (spit file
        (->> spec
             json/write-str
             ds/vega-lite-spec->svg)))

(defn read-delimited
  "Read a tsv into a vector."
  [file delimiter]
  (with-open
   [rdr (io/reader file)]
    (->> (line-seq rdr)
         (map (fn [x] (str/split x delimiter)))
         (mapv vec))))

(defn read-tsv
  "Read the tsv FILE into a vector."
  [file] (read-delimited file #"\s+"))

(defn read-csv
  "Read the csv FILE into a vector."
  [file] (read-delimited file #"\s*,\s*"))

(defn map-work-row
  "Convert a timesheet row into a map"
  [row] (let [[prj date hrs] row]
          {:Project prj :Date date :Hours (to-float hrs)}))

(defn sum-key
  "e.g. sum-key :Date '2021-01'"
  [key val data]
  (reduce +
          (for [x data :when (= val (key x))]
            (:Hours x))))

(defn sum-project
  "sum-key for a project"
  [project data]
  (sum-key :Project project data))

(defn sum-weekday
  "sum-key for a weekday"
  [weekday data]
  (sum-key :Day weekday data))

(defn sum-date
  "sum-key for a date"
  [date data]
  (sum-key :Date date data))

(defn split-date
  "Split on - or /"
  [x] (str/split x #"[-/]"))

(defn get-year
  "Extract the year from a string a la 2022-01-02"
  [x] (first (split-date x)))

(defn get-month
  "Extract the month from a string a la 2022-01-02"
  [x] (second (split-date x)))

(defn normalize
  "Normalize a timesheet to be a fraction of the total instead of the actual value."
  [data]
  (letfn [(updater [entry]
            (fn [hour] (/ hour
                          (sum-date (:Date entry) data))))
          (normalize-row [entry]
            (update entry :Hours (updater entry)))]

    (map normalize-row data)))

(defn extract-data
  "Extract data from the awk produced tsv files"
  [file]
  (->> (rest (read-tsv file))
       (map map-work-row)
       (filter #(number? (:Hours %)))))

(defn get-csv-file-names
  "Get the names of all the csv timesheet files."
  []
  (->> (file-seq (clojure.java.io/file "csv/"))
       (filter #(.isFile %))
       (mapv str)
       (filter #(re-matches #"csv/[0-9]{4}-[0-9]{2}.csv" %))
       (sort)))

(defn slurp-timesheet
  "Slurp a csv timesheet into a collection of maps"
  [fname]
  (let [[year month]
        (map to-int
             (subvec (str/split fname #"[^A-z0-9]") 1 3))
        rows (->> fname read-csv (remove empty?))]
    (for [row rows
          :let [rowmap {:Date (to-int (nth row 0 ""))
                        :Time (nth row 1 "")
                        :Project (nth row 2 "")
                        :Task (nth row 3 "")
                        :Day (nth row 4 "")
                        :Hours (to-float (nth row 5 ""))}]
          :when (and (not (str/blank? (:Project rowmap)))
                     (not (str/blank? (:Time rowmap)))
                     (number? (:Hours rowmap))
                     (number? (:Date rowmap)))]
      (update rowmap :Date
              #(format "%04d-%02d-%02d" year month %)))))

 ;; #((jt/local-date "yyyy-MM-dd" (format "%04d-%02d-%02d" year month %))))

(defn standardize-project-names
  "Replace the names in the timesheets with the standard database names."
  [data]
  (letfn [(subst [x]
            (-> x
                (str/upper-case)
                (str/replace #".*ETL.*" "ETL")
                (str/replace #".*ME0.*" "GEM")
                (str/replace #".*GE21.*"  "GEM")
                (str/replace #".*GE11.*"  "GEM")
                (str/replace #".*ATLAS.*"  "L0MDT")
                (str/replace #".*L0MDT.*"  "L0MDT")
                (str/replace #".*IPMC.*"  "APOLLO")
                (str/replace #".*APOLLO-IPMC.*"  "APOLLO")
                (str/replace #".*TRACKER.*"  "APOLLO")
                (str/replace #".*CSC.*"  "OTHER PRJ")
                (str/replace #".*EMPHATIC.*"  "OTHER PRJ")
                (str/replace #".*SCOTT-LAB.*"  "OTHER PRJ")
                (str/replace #"ADMIN"  "EDF")
                (str/replace #"DEVEL"  "EDF")
                (str/replace #".*VAC.*"  "--")
                (str/replace #".*SICK.*"  "--")
                (str/replace #".*HOLIDAY.*"  "VAC/SICK/HOL")))]
    (remove (fn [x] (= "--" (:Project x)))
            (for [item data]
              (update item :Project #(subst %))))))

;;------------------------------------------------------------------------------
;; Data From CSVs
;;------------------------------------------------------------------------------

(def all-work-data
  "All the work data directly from CSV"
  (standardize-project-names
   (apply concat (mapv slurp-timesheet (get-csv-file-names)))))

(def projects
  "List of all projects"
  (set (map :Project all-work-data)))

(def total-data
  "EDF workload summed by project (all years combined)"
  (map (fn [prj]
         {:Project prj
          :Hours  (sum-project prj all-work-data)}) projects))

(def work-data-by-year
  "EDF workload by year"
  (map (fn [x] (update x :Date (fn [x] (get-year x)))) all-work-data))

(def data-by-weekday
  "Data binned by day of the week"
  (map (fn [weekday]
         {:Day weekday
          :Hours  (sum-weekday weekday all-work-data)})
       ["MON" "TUE" "WED" "THU" "FRI" "SAT" "SUN"]))

(defn bin-by-day
  "Take in the raw timesheet data and bin it into sums by day,
  where all projects in a given day are totaled"
  [data]
  (let [dates (distinct (for [date data] (:Date date)))
        sums (reductions +  (for [date dates]
                              (sum-date date data)))]
    (list dates sums)))

(defn running-sum-by-day
  "Compute a running sum (by day) for a given YEAR.
  If the YEAR is not supplied then compute the total for all years."
  [& {:keys [year]}]

  (letfn [(year-match? [x]
            (or (not year)
                (= year (get-year (:Date x)))))]

    (let [[dates hours]
          (->> all-work-data
               (filter year-match?)
               (bin-by-day))]
      (map (fn [date hour] {:Project (if year year "Total")
                            :Date (str/join "/" (rest (str/split date #"-")))
                            :Hours hour}) dates hours))))

(defn get-first-day-from-each-month
  "Get the first day entered for each month in a given YEAR.
  If no YEAR is supplied then default to 2021"
  ([] (get-first-day-from-each-month "2021"))

  ([year]
   (->> (for [day (running-sum-by-day :year year)
              :let [month (get-month (:Date day))]]
          (assoc day :Month month))
        (group-by :Month)
        (vals)
        (map first)
        (map :Date)
        (sort))))

(defn strip-date [data]
  (for [item data]
    (update item :Date
            (fn [x]
              (str/join "-" (subvec  (str/split x #"-") 0 2))))))

;;------------------------------------------------------------------------------
;; Plots Functions
;;------------------------------------------------------------------------------

(defn pie-chart
  "Return a vega-lite spec for a pie-chart."
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

(defn bar-chart
  "Return a vega-lite spec for a bar-chart."
  [x y data]
  {:data {:values data}
   :mark "bar"
   :width plt-width
   :height plt-height
   :encoding {:x {:field x :type "ordinal"}
              :y {:field y :aggregate "sum" :type "quantitative"}
              :color {:field "Project" :type "nominal"}}})

(defn bar-chart-day
  "Return a vega-lite spec for a bar-chart."
  [x y data]
  {:data {:values data}
   :mark "bar"
   :width plt-width
   :height plt-height
   :encoding {:x {:field x
                  :type "ordinal"
                  :sort ["MON" "TUE" "WED" "THU" "FRI" "SAT" "SUN"]}
              :y {:field y :aggregate "sum" :type "quantitative"}}})

(defn line-chart

  "Return the vega spec for  a line chart from the
  X key and Y key of a given dataset,
  where the dataset is a collection of maps a la
  ({:key 1 :val 1} {:key 2 :val 2})"

  [x y data]
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json",
   :width plt-width
   :height plt-height
   :data {:values data}
   :mark {:type "line"}
   :encoding {:x {:field x :type "ordinal"
                  :axis {:ticks 12
                         :tickCount 12
                         :labelValues (range 1 13)
                         :values (get-first-day-from-each-month)
                         :labelExpr "parseInt(slice(toString(datum.label), 0, 2), 10)"}}
              :y {:field y :aggregate "sum" :type "quantitative"},
              :color {:field "Project", :type "nominal"}}})

;;------------------------------------------------------------------------------
;; Plots
;;------------------------------------------------------------------------------

(plot! "timesheetdaily.svg"         (bar-chart-day  "Day"      "Hours" data-by-weekday))
(plot! "timesheetday.svg"           (bar-chart      "Date"     "Hours" (strip-date all-work-data)))
(plot! "timesheetmonthly.svg"       (bar-chart      "Date"     "Hours" (strip-date all-work-data)))
(plot! "timesheetmonthlynormal.svg" (bar-chart      "Date"     "Hours" (normalize (strip-date all-work-data))))
(plot! "timesheetyearly.svg"        (bar-chart      "Date"     "Hours" work-data-by-year))
(plot! "timesheetyearlynormal.svg"  (bar-chart      "Date"     "Hours" (normalize work-data-by-year)))

(plot! "timesheet_pie.svg"          (pie-chart      "EDF Work" "Project" "Hours" total-data))

(plot! "timesheetdayrunning.svg"
       (line-chart  "Date" "Hours"
                    (concat  (running-sum-by-day :year "2021")
                             (running-sum-by-day :year "2022")
                             (running-sum-by-day :year "2023"))))
