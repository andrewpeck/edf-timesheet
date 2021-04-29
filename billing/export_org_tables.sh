#!/usr/bin/env sh
:; ( echo "$EMACS" | grep -q "term" ) && EMACS=emacs || EMACS=${EMACS:-emacs} # -*-emacs-lisp-*-
:; command -v $EMACS >/dev/null || { >&2 echo "Can't find emacs in your PATH"; exit 1; }
:; exec emacs -Q --script "$0" -- "$@"
:; exit 0
;; -*- lexical-binding: t -*-

(require 'org)
(require 'org-table)

(defun org-table-export-all ()
  "Export to CSV all named tables in current org mode file"
  (interactive)
  (outline-show-all)
  (let ((case-fold-search t))
    (while (search-forward-regexp "#\\(\\+TBLNAME: \\|\\+TBLNAME: \\)\\(.*\\)" nil t)
      (let ((name (match-string-no-properties 2)))
        (progn
          (next-line)
          (princ (format "Exporting table to %s.csv\n" name))
          (org-table-export (format "%s.csv" name) "orgtbl-to-csv"))))))

(mapc (lambda (file)
        (find-file (expand-file-name file))
        ;;(print file)
        (org-table-export-all)
        (kill-buffer)) argv)

;;;
