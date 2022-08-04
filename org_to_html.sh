#!/bin/bash
# -*- mode: shell-script -*-
#
# re-align an org mode table
#
DIR=$(pwd)
FILES=""
# wrap each argument in the code required to call tangle on it
for i in "$@"; do
FILES="$FILES \"$i\""
done

if ! command -v emacs &> /dev/null
then
    echo "Emacs not in path :("
    exit
fi

emacs -Q --batch \
--eval "(progn
     (require 'org)
     (require 'org-table)
     (mapc (lambda (file)
            (find-file (expand-file-name file \"$DIR\"))
            (org-html-export-to-html)
            (kill-buffer)) '($FILES)))"
