;; W = ( d + floor (2.6m - 0.2) - 2C + Y + floor(Y/4) + floor (C/4) ) mod 7
;; https://cs.uwaterloo.ca/~alopez-o/math-faq/node73.html
;; k is day (1 to 31)
;; m is month (1 = March, ..., 10 = December, 11 = Jan, 12 = Feb) Treat Jan & Feb as months of the preceding year
;; C is century (1987 has C = 19)
;; Y is year (1987 has Y = 87 except Y = 86 for Jan & Feb)
;; W is week day (0 = Sunday, ..., 6 = Saturday)

(defun ymd-to-weekday (C Y m d)

  ;; (1 = March, ..., 10 = December, 11 = Jan, 12 = Feb) Treat Jan & Feb as months of the preceding year
  (if (< m 3)
      (progn (setf Y (- Y 1))
             (setf m (+ m 10)))
    (setf m (- m 2)))

  ;; Return the day of the week
  (mod (+ d
          (floor (- (* m 2.6) 0.2))
          (- (* 2 C))
          Y
          (floor (/ Y 4.0))
          (floor (/ C 4.0))) 7))

(defun weekday-to-abbr (d)
  (aref ["SUN" "MON" "TUE" "WED" "THU" "FRI" "SAT"] d))

(defun month-to-number (m)
  (pcase m
    ("January"   1)
    ("February"  2)
    ("March"     3)
    ("April"     4)
    ("May"       5)
    ("June"      6)
    ("July"      7)
    ("August"    8)
    ("September" 9)
    ("October"   10)
    ("November"  11)
    ("December"  12)
    (_ -1)))

;; org-get-outline-path t
(defun get-day-of-week (title day)
  (let* ((heading (split-string title " " t))
         (month (month-to-number (car heading)))
         (year (string-to-number (cadr heading)))
         (y (mod year 100))
         (c (/ year 100)))
    (if (string= day "") " "
      (weekday-to-abbr (ymd-to-weekday c y month (string-to-number day))))))

(defun clock-to-float (time)
  "Convert a clock time (e.g. 12:30) to a float (e.g. 12.5)"
  (let* ((split (split-string time ":" t))
         (hours (string-to-number (car split)))
         (minutes 0))
    (when (cadr split)
      (setf minutes (string-to-number (cadr split))))
    (print (+ hours (/ minutes 60.0)))))

(defun range-to-time (range)
  "Convert a 12hr clock time range time (e.g. 1-2:30) to a float amount of time (1.5)"
  (if (string-empty-p range) ""
    (let ((start (clock-to-float (car (split-string range "-" t))))
          (end (clock-to-float (cadr (split-string range "-" t)))))
      (when (> start end)
        (setf end (+ 12 end)))
      (- end start))))

;; (if (not (equal a ""))
;;     (range-to-time a) a)

;; ;(setq path "January 2023")
;; (when (and (boundp 'path) (boundp 'k))
;;     (get-day-of-week path k))

(defun update-all-histograms ()
  (save-excursion
    (goto-char (point-min))
    (while
        (re-search-forward "#\\+begin_src emacs-lisp.*data=[0-9]\\{4\\}-[0-9]\\{2\\}$" nil t)
      (forward-line)
      (org-babel-execute-src-block))))
