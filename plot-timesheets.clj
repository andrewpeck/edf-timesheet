;;------------------------------------------------------------------------------
;; Deps + Constants
;;------------------------------------------------------------------------------

(ns plot-timesheets
  (:require [applied-science.darkstar :as ds]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.java.io :as io]

            [clojure.pprint :only (pprint)]
            ;; [clojure.java.shell :only [sh]]
            ))

(def plt-width 800)
(def plt-height 600)

;;------------------------------------------------------------------------------
;; Utility Functions
;;------------------------------------------------------------------------------

(defn get-one-day-from-each-month [data]
  (->> (for [day (running-sum-by-day :year "2021")
             :let [month (first (str/split (:Date day) #"/"))]]
         (assoc day :Month month))
       (group-by :Month)
       (vals)
       (map first)
       (map :Date)
       (sort)))

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
  {:Project (nth row 0)
   :Date (nth row 1)
   :Hours (to-float (nth row 2))})

(defn sum-project [project data]
  (->> (filter (fn [x] (= project (:Project x))) data)
       (map :Hours)
       (reduce +)))

(defn sum-key
  "e.g. sum-key :Date '2021-01'"
  [key val data]
  (reduce +
          (for [x data :when (= val (key x))]
            (:Hours x))))

(defn sum-weekday [weekday data]
  (sum-key :Day weekday data))

(defn sum-date [date data]
  (sum-key :Date date data))

(defn get-year [x]
  (first (str/split x #"-")))

(defn normalize [data]
  (letfn [(updater [entry]
            (fn [hour] (/ hour
                          (sum-date (:Date entry) data))))
          (normalize-row [entry]
            (update entry :Hours (updater entry)))]

    (map normalize-row data)))

(defn extract-data [file]
  (->> (rest (read-tsv file))
       (map map-work-row)
       (filter #(number? (:Hours %)))))

(defn clock-to-float
  "Convert a clock time (e.g. 12:30) to a float (e.g. 12.5)"
  [time]

  (let [split (str/split time #":")
        hours (edn/read-string (first split))
        minutes  (if (second split)
                   (edn/read-string (second split))
                   0)]
    (+ hours (/ minutes 60.0))))
(clock-to-float "12:30")

(defn range-to-time
  "Convert a 12hr clock time range time (e.g. 1-2:30) to a float amount of time (1.5)"
  [range]
  (let [start (clock-to-float (first (str/split range #"-")))
        end (clock-to-float (second (str/split range #"-")))
        wrap (if (> start end) (+ 12 end) end)]
    (- wrap start)))

(defn get-csv-file-names []
  (->> (file-seq (clojure.java.io/file "csv/"))
       (filter #(.isFile %))
       (mapv str)
       (filter #(re-matches #"csv/[0-9]{4}-[0-9]{2}.csv" %))
       (sort)))

(defn slurp-timesheet [fname]
  (let [[year month]
        (map #(Integer/parseInt %)
             (subvec (str/split fname #"[^A-z0-9]") 1 3))
        rows (->> fname read-csv (remove empty?))]
      (for [row rows
            :let [rowmap {:Date (edn/read-string (nth row 0 ""))
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

;;------------------------------------------------------------------------------
;; Data From Accruals.txt
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
          :Hours  (sum-project prj work-data)}) projects))

;;------------------------------------------------------------------------------
;; Data From CSVs
;;------------------------------------------------------------------------------

(def all-work-data
  "All the work data directly from CSV"
  (apply concat (mapv slurp-timesheet (get-csv-file-names))))

(def data-by-weekday
  "Data binned by day of the week"
  (map (fn [weekday]
         {:Day weekday
          :Hours  (sum-weekday weekday all-work-data)})
       ["MON" "TUE" "WED" "THU" "FRI" "SAT" "SUN"] ))

(defn bin-by-day [data]
  (let
      [dates (distinct (for [date data] (:Date date)))
       sums (reductions +  (for [date dates]
                             (sum-date date data)))]
    (list dates sums)))

(prn (bin-by-day all-work-data))

(defn running-sum-by-day [& {:keys [year month] :as opts} ]

  (letfn [(year-match? [x]
            (or (not year)
                (= year (first (str/split (:Date x) #"-")))))]

    (let [[dates hours]
          (->> all-work-data
               (filter year-match?)
               (bin-by-day))]
      (map (fn [date hour] {:Project (if year year  "Total")
                            :Date (str/join "/" (rest (str/split date #"-")))
                            :Hours hour}) dates hours))))

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

(defn line-chart ""
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
                         :values (get-one-day-from-each-month data)
                         :labelExpr "parseInt(slice(toString(datum.label), 0, 2), 10)"}}
              :y {:field y :aggregate "sum" :type "quantitative"},
              :color {:field "Project", :type "nominal"}}})

;;------------------------------------------------------------------------------
;; Plots
;;------------------------------------------------------------------------------

;; work data
;; 0. { :Project "APOLLO", :Date "2021-01", :Hours 22.0 }
;;
;; all-work-data
;; 0. { :Date "2021-03-01", :Time "8:30-9:30", :Project "ETL", :Task "Slides for Ted", :Day "MON", ... } ;;

(plot! "timesheetdaily.svg" (bar-chart-day  "Day" "Hours" data-by-weekday))
(plot! "timesheet_pie.svg" (pie-chart "EDF Work" "Project" "Hours" total-data))
(plot! "timesheetday.svg" (bar-chart  "Date" "Hours" all-work-data))
(plot! "timesheetmonthly.svg" (bar-chart  "Date" "Hours" work-data))
(plot! "timesheetmonthlynormal.svg" (bar-chart  "Date" "Hours" (normalize work-data)))
(plot! "timesheetyearly.svg" (bar-chart  "Date" "Hours" work-data-by-year))
(plot! "timesheetyearlynormal.svg" (bar-chart  "Date" "Hours" (normalize work-data-by-year)))
(plot! "timesheetdayrunning.svg"
       (line-chart  "Date" "Hours"
                    (concat  (running-sum-by-day :year "2021")
                             (running-sum-by-day :year "2022")
                             (running-sum-by-day :year "2023"))))
