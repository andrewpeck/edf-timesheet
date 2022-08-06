#!/usr/bin/env gawk
# awk -f timesheets.awk 20[0-9][0-9]-[0-9][0-9].csv

BEGIN {
  FS=","
  PROJECT=3
  TASK=4
  HOURS=6
}

{
  if ($HOURS ~ /^[0-9.]+$/) {

    split(FILENAME, yearmonth, "[-./]")
    year=yearmonth[2]
    month=yearmonth[3]

    prj = toupper($PROJECT)

    # make some common substitutions
    sub("ME0[A-z][A-z]", "ME0", prj)
    sub("TRACKER", "APOLLO", prj)
    sub("VACATION", "VAC", prj)

    asplit("-- VAC SICK DEVEL ADMIN HOLIDAY SCOTT-LAB-CAMERA", skipped_plots)

    if (!(prj in skipped_plots)) {
      sum += $HOURS
      sum_by_prj[prj] += $HOURS

      sum_by_yy_mm_prj[year][month][prj] += $HOURS

      sum_by_prj_yy[prj][year] += $HOURS
      sum_by_yy[year] += $HOURS
      sum_by_yy_prj[year][prj] += $HOURS

      sum_by_yy_mm[year][month] += $HOURS

      sum_by_prj_yy_mm[prj][year][month] += $HOURS
    }
  }
}

function print_title (title) {
  printf("\n%s\n", title)
  printf("-----------------------------------------------------\n")
}

function print_heading (array, outfile) {
  pipe = "tee " outfile
  s = ""
  s = s sprintf("%7s", "Date")
  for (prj in array) {
    s = s sprintf("\t%8s", prj)
  }
  s = s sprintf("\n")
  printf(s) | pipe
  close(pipe)
}

# make an assoc array from a string
function asplit (str, arr) {
  n = split(str, temp, " ")
  for (i=1; i<=n; i++)
    arr[temp[i]]++
  return n
}

END {

  ################################################################################
  print_title("Monthly Accruals")
  ################################################################################

  sortcmd="sort -t'\t' -n -k1 | tee accruals.txt"
  for (prj in sum_by_prj_yy_mm) {
    for (year in sum_by_prj_yy_mm[prj]) {
      for (month in sum_by_prj_yy_mm[prj][year]) {

        amt = sum_by_prj_yy_mm[prj][year][month];
        sum = sum_by_yy_mm[year][month];
        rate = 89

        printf("%10s \t %4d-%02d \t %6.2f hours \t %4.1f\% \t $%.2f\n", prj, year, month,
               amt,
               amt/sum*100,
               amt * rate) | sortcmd
      }
    }
  }
  close(sortcmd)

  ################################################################################
  print_title("Total by Project")
  ################################################################################

  sortcmd="sort -t'\t' -n -k2"
  for (key in sum_by_prj) {
    printf("%10s \t %5.1f \t %4.1f\%\n", key, sum_by_prj[key], sum_by_prj[key]/sum * 100) | sortcmd
  }
  close(sortcmd)

  ################################################################################
  print_title("Total by Month")
  ################################################################################

  sortcmd="sort -t'\t' -n -k1"
  for (year in sum_by_yy_mm) {
    for (month in sum_by_yy_mm[year]) {
      printf("    %4d-%02d\t%6.2f hours\n", year, month, sum_by_yy_mm[year][month]) | sortcmd
      E}
  }
  close(sortcmd)

  ################################################################################
  print_title("Yearly Accruals")
  ################################################################################

  sortcmd="sort -t'\t' -n -k1"
  for (prj in sum_by_prj_yy) {
    for (year in sum_by_prj_yy[prj]) {
      printf("%10s\t%4d\t%6.2f hours \t %4.1f\%\n",
             prj, year, sum_by_prj_yy[prj][year],
             sum_by_prj_yy[prj][year]/sum_by_yy[year]*100) | sortcmd
    }
  }
  close(sortcmd)

  ################################################################################
  print_title("Yearly Histogram")
  ################################################################################

  outfile = "yearly_histo.txt"
  sortcmd="sort -t'\t' -n -k1 | tee -a " outfile
  print_heading(sum_by_prj, outfile)
  for (year in sum_by_yy_prj) {
    s = ""
    s = s sprintf("%4d    ", year)
    for (prj in sum_by_prj) {
      hrs = sum_by_yy_prj[year][prj]/sum_by_yy[year]
      s = s sprintf("\t%8.3f", hrs)
    }
    s = s sprintf("\n")
    printf("%s", s) | sortcmd
  }
  close(sortcmd)

  ################################################################################
  print_title("Monthly Histogram")
  ################################################################################

  outfile = "monthly_histo.txt"
  sortcmd="sort -t'\t' -n -k1 | tee -a " outfile

  print_heading(sum_by_prj, outfile)

  for (year in sum_by_yy_mm_prj) {
    for (month in sum_by_yy_mm_prj[year]) {
      s = ""
      s = s sprintf("%4d/%02d    ", year, month)
      for (prj in sum_by_prj) {
        hrs = sum_by_yy_mm_prj[year][month][prj]/sum_by_yy_mm[year][month]
        s = s sprintf("\t%8.3f", hrs)
      }
      s = s sprintf("\n")
      printf("%s", s) | sortcmd
    }
  }
  close(sortcmd)

}
