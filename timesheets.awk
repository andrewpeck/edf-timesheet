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
    sub("ME0BE", "ME0SF", prj)
    sub("ME0[A-z][A-z]", "ME0", prj)
    #sub("GE[2,1]", "GEM", prj)
    sub("TRACKER", "APOLLO", prj)
    sub("VACATION", "VAC", prj)
    sub("CSC", "OTHER", prj)
    sub("EMPHATIC", "OTHER", prj)
    sub("ROBLYER-DDOSI", "OTHER", prj)
    sub("YOUNGER-MISC", "OTHER", prj)
    sub("SCOTT-LAB-CAMERA", "OTHER", prj)
    sub("DINESH-MIGDAL", "OTHER", prj)

    sub("DEVEL", "ETC", prj)
    sub("SICK", "ETC", prj)
    sub("VAC", "ETC", prj)
    sub("HOLIDAY", "ETC", prj)
    sub("ADMIN", "ETC", prj)

    asplit("--", skipped_plots)
    #asplit("-- VAC SICK DEVEL ADMIN HOLIDAY", skipped_plots)

    wordcloud = "wordcloud.txt"

    if (!(prj in skipped_plots)) {
      sum += $HOURS
      sum_by_prj[prj] += $HOURS

      sum_by_yy_mm_prj[year][month][prj] += $HOURS

      sum_by_prj_yy[prj][year] += $HOURS
      sum_by_yy[year] += $HOURS
      sum_by_yy_prj[year][prj] += $HOURS

      sum_by_yy_mm[year][month] += $HOURS

      sum_by_prj_yy_mm[prj][year][month] += $HOURS

      sub("chat", "meeting", $4)
      sub("Chat", "meeting", $4)
      sub("Meetings", "meeting", $4)
      sub("meetings", "meeting", $4)
      sub("meet ", "meeting ", $4)
      sub("Meet ", "meeting ", $4)
      gsub(" ", "\n\n", $4)
      print tolower($4) "\n" > "wordcloud.txt"
    }
  }
}

function print_title (title) {
  printf("\n%s\n", title)
  printf("-----------------------------------------------------\n")
}

function fmt_sort_cmd () {
  return "sort -t',' -n -k1 | column -t -s \",\" "
}

function fmt_heading (array, outfile) {
  pipe = "tee " outfile
  s = ""
  s = s sprintf("%s", "Date")
  for (prj in array) {
    s = s sprintf(", %s", prj)
  }
  s = s sprintf("\n")
  return s
}

# make an assoc array from a string
function asplit (str, arr) {
  n = split(str, temp, " ")
  for (i=1; i<=n; i++)
    arr[temp[i]]++
  return n
}

END {

  # system("sort -o wordcloud.txt wordcloud.txt")

  ################################################################################
  print_title("Monthly Accruals")
  ################################################################################

  printf("Project Date     Hours Prcnt  Cost\n") | "tee accruals.txt"
  close("tee accruals.txt")

  sortcmd="sort -t',' -n -k1 >> accruals.txt"
  for (prj in sum_by_prj_yy_mm) {
    for (year in sum_by_prj_yy_mm[prj]) {
      for (month in sum_by_prj_yy_mm[prj][year]) {

        amt = sum_by_prj_yy_mm[prj][year][month];
        sum = sum_by_yy_mm[year][month];
        rate = 89

        printf("%-7s %4d-%02d %6.2f %4.1f%%  $%.2f\n",
               prj, year, month, amt, amt/sum*100, amt * rate) | sortcmd
      }
    }
  }
  close(sortcmd)

  ################################################################################
  print_title("Total by Project")
  ################################################################################

  sortcmd="sort -t',' -n -k2 | column -t -s \",\" "
  for (key in sum_by_prj) {
    printf("%s, %5.1f, %4.1f%%\n", key, sum_by_prj[key], sum_by_prj[key]/sum * 100) | sortcmd
  }
  close(sortcmd)

  ################################################################################
  print_title("Total by Month")
  ################################################################################

  sortcmd=fmt_sort_cmd()
  for (year in sum_by_yy_mm) {
    for (month in sum_by_yy_mm[year]) {
      printf("%4d-%02d, %6.2f hours\n", year, month, sum_by_yy_mm[year][month]) | sortcmd
      }
  }
  close(sortcmd)

  ################################################################################
  print_title("Yearly Accruals")
  ################################################################################

  sortcmd=fmt_sort_cmd()
  for (prj in sum_by_prj_yy) {
    for (year in sum_by_prj_yy[prj]) {
      printf("%s, %4d, %6.2f hours, %4.1f%%\n",
             prj, year, sum_by_prj_yy[prj][year],
             sum_by_prj_yy[prj][year]/sum_by_yy[year]*100) | sortcmd
    }
  }
  close(sortcmd)

  ################################################################################
  print_title("Yearly Histogram")
  ################################################################################

  outfile = "yearly_histo.txt"
  sortcmd=fmt_sort_cmd() " | tee " outfile

  s = fmt_heading(sum_by_prj, outfile)
  for (year in sum_by_yy_prj) {
    s = s sprintf("%4d", year)
    for (prj in sum_by_prj) {
      hrs = sum_by_yy_prj[year][prj]/sum_by_yy[year]
      s = s sprintf(", %.3f", hrs)
    }
    s = s sprintf("\n")
  }
  printf("%s", s) | sortcmd
  close(sortcmd)

  ################################################################################
  print_title("Monthly Histogram")
  ################################################################################

  outfile = "monthly_histo.txt"
  sortcmd=fmt_sort_cmd() " | tee " outfile

  s = fmt_heading(sum_by_prj, outfile)

  for (year in sum_by_yy_mm_prj) {
    for (month in sum_by_yy_mm_prj[year]) {
      s = s sprintf("%4d/%02d", year, month)
      for (prj in sum_by_prj) {
        hrs = sum_by_yy_mm_prj[year][month][prj]
        s = s sprintf(", %.3f", hrs)
      }
      s = s sprintf("\n")
    }
  }
  printf("%s", s) | sortcmd
  close(sortcmd)

  ################################################################################
  print_title("Monthly Totals")
  ################################################################################

  outfile = "monthly_totals.txt"
  sortcmd=fmt_sort_cmd() " | tee " outfile

  s = "Date, Total\n"

  for (year in sum_by_yy_mm) {
    for (month in sum_by_yy_mm[year]) {
      s = s sprintf("%4d/%02d", year, month)
        hrs = sum_by_yy_mm[year][month]
        s = s sprintf(", %.3f", hrs)
      s = s sprintf("\n")
    }
  }
  printf("%s", s) | sortcmd
  close(sortcmd)

}
