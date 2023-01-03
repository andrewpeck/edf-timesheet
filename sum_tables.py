import csv
import datetime
import re
import sys
import csv
import os
import json
from collections import OrderedDict
import pprint
#from glob import glob

ONEDAY = datetime.timedelta(days=1)


def get_csv_files():
    "Get a list of CSV files matching the date format e.g. 2021-09.csv"
    res = [f for f in os.listdir("./csv/") if re.search(r'.[0-9]+-[0-9]+\.csv$', f)]
    res.sort()
    return res


def princ(string):
    """Wrapper to print without a newine... uhg"""
    print(string, end='')


def substitute_project_name(name):
    "Replace some alternative project names with the canconical names"

    def subst(name, vara, varb):
        if name == vara:
            return varb
        return name

    """Common substitions from shorthand to EDF database names"""
    name = subst(name, "ETL", "CMS-ETL")
    name = subst(name, "ETL-RB", "CMS-ETL")
    name = subst(name, "ETL-MODULE", "CMS-ETL")
    name = subst(name, "GE21", "CMS-EMU-UPGRADE-GE21")
    name = subst(name, "CSC", "CMS-EMU-OPS-CSC")
    name = subst(name, "GE11", "CMS-EMU-OPS-GE11")
    name = subst(name, "ME0", "CMS-EMU-UPGRADE-ME0")
    name = subst(name, "ME0OH", "CMS-EMU-UPGRADE-ME0")
    name = subst(name, "ME0SF", "CMS-EMU-UPGRADE-ME0")
    name = subst(name, "ME0BE", "CMS-EMU-UPGRADE-ME0")
    name = subst(name, "IPMC", "CMS-PIXEL-DTC")
    name = subst(name, "ATLAS", "ATLAS-MUON-PHASE2")
    name = subst(name, "L0MDT", "ATLAS-MUON-PHASE2")
    name = subst(name, "APOLLO-IPMC", "APOLLO")
    name = subst(name, "TRACKER", "CMS-PIXEL-DTC")
    name = subst(name, "VACATION", "VAC")
    return name


def csv_to_dict(file):
    """Reads in an org-mode table exported to csv, convert it a python
    dictionary"""

    with open(file) as csvfile:

        projects = OrderedDict()

        table = csv.reader(csvfile, delimiter=",")
        for row in table:
            if (len(row) > 0):
                if is_float(row[5]):
                    day = int(row[0])
                    project = substitute_project_name(row[2].upper())
                    description = row[3]
                    hours = float(row[5])

                    if project not in projects:
                        projects[project] = OrderedDict()

                    if day not in projects[project]:
                        projects[project][day] = {"hours": 0.0, "description": ""}

                    projects[project][day]["hours"] += hours
                    projects[project][day]["description"] += description + "; "

        return projects


def is_float(string):
    """Check if argument is a floating point number"""
    try:
        float(string)
        return True
    except ValueError:
        return False

def project_to_summary(projects):
    ""

    summaries = OrderedDict()

    for year in projects:
        for month in projects[year]:
            for prj in projects[year][month]:
                for day in projects[year][month][prj]:

                    d = datetime.date(year, month, day)
                    week = d.isocalendar()[1]
                    weekday = d.weekday()

                    hours = projects[year][month][prj][day]["hours"]
                    description = projects[year][month][prj][day]["description"] + "; "

                    # intialize the dict
                    if year not in summaries:
                        summaries[year] = OrderedDict()
                    if week not in summaries[year]:
                        summaries[year][week] = OrderedDict()
                    if prj not in summaries[year][week]:
                        summaries[year][week][prj] = OrderedDict()
                    if weekday not in summaries[year][week][prj]:
                        summaries[year][week][prj][weekday] = 0
                    if "notes" not in summaries[year][week][prj]:
                        summaries[year][week][prj]["notes"] = ""

                    summaries[year][week][prj][weekday] += hours
                    summaries[year][week][prj]["notes"] += description

    for year in summaries:
        for week in summaries[year]:
            for prj in summaries[year][week]:
                notes = summaries[year][week][prj]["notes"]

                # conditioning of the text
                notes = re.sub(r"\s+", " ", notes)  # REMOVE DUPLICATE SPACES
                notes = re.sub(r", ", ";", notes)   # CONVERT COMMAS TO SEMICOLONS
                notes = re.sub(r"; ", ";", notes)  # REMOVE SPACE SEPARATORS

                notes = re.split(";", notes)
                notes.sort()
                notes = list(set(notes))  # REMOVE DUPLICATES
                notes.sort()
                notes = ' '.join([str(elem)+";" for elem in notes])  # CONVERT FROM LIST TO STRING
                notes = re.sub(r"^;\s*", "", notes)  # REMOVE THE FIRST SEMICOLON
                notes = re.sub(r";\s*$", "", notes)  # REMOVE THE LAST SEMICOLON
                summaries[year][week][prj]["notes"] = notes

    return summaries


def print_org_summary(summaries):

    def print_spacer():
        spacer = "|----------------------+-------+-------+-------+-------+-------+-------+-------+-------|"
        print(spacer)

    def print_weekdays(time):
        """"""
        d = time
        # print the day number headings
        princ("| %20s |" % "")
        for weekday in range(7):
            if (d.day < 7 and d.weekday() > weekday):
                diff = d.weekday() - weekday
                princ("% 6d*|" % (d-diff*ONEDAY).day)
            else:
                if d.month == month:
                    princ("% 6d |" % d.day)
                else:
                    princ("% 6d*|" % d.day)
                d = d + ONEDAY
        princ("       |\n")

    print("#+TITLE: Hours")

    for file in get_csv_files():

        d = datetime.datetime.strptime(file.replace(".csv", ""), "%Y-%m")

        month = d.month
        year = d.year

        d = datetime.datetime(year, month, 1)

        print("")
        print("* %4d-%02d" % (year, month))
        print("#+TBLNAME: %4d-%02d-Summary" % (year, month))
        print("| %20s |    Mo |    Tu |    We |    Th |    Fr |    Sa |    Su | Notes |" %
              ("%04d-%02d" % (year, month)))
        print_spacer()

        while d.month == month:

            if (d.day == 1 or d.weekday() == 0):
                print_weekdays(d)
                print_spacer()
                week = d.isocalendar()[1]

                if week in summaries[year]:
                    for prj in summaries[year][week]:
                        if prj == "--":
                            continue
                        princ("| %20s |" % prj)
                        for weekday in range(7):
                            if weekday in summaries[year][week][prj]:
                                princ(" % 3.2f |" % summaries[year][week][prj][weekday])
                            else:
                                princ("     0 |")
                        princ("%s | \n" % summaries[year][week][prj]["notes"])

            if d.weekday() == 6:
                print_spacer()

            d = d + ONEDAY # INCREMENT
        print_spacer()

def create_summary_tables(summaries):

    tables = {}

    def weekdays(time):
        """"""
        weekrow = [""]
        d = time
        # print the day number headings
        for weekday in range(7):
            if (d.day < 7 and d.weekday() > weekday):
                diff = d.weekday() - weekday
                weekrow.append("%d*" % (d-diff*ONEDAY).day)
            else:
                if d.month == month:
                    weekrow.append("%d" % d.day)
                else:
                    weekrow.append("%d*" % d.day)
                d = d + ONEDAY
        weekrow.append("")
        return(weekrow)

    for file in get_csv_files():

        d = datetime.datetime.strptime(file.replace(".csv", ""), "%Y-%m")

        month = d.month
        year = d.year

        d = datetime.datetime(year, month, 1)

        table = []

        table.append(["%4d-%02d" % (year, month), "Mo", "Tu", "We", "Th", "Fr", "Sa", "Su", "Notes"])

        while d.month == month:

            if (d.day == 1 or d.weekday() == 0):
                table.append(weekdays(d))

                week = d.isocalendar()[1]

                if week in summaries[year]:
                    for prj in summaries[year][week]:
                        if prj == "--":
                            continue
                        projectrow = [prj]
                        for weekday in range(7):
                            if weekday in summaries[year][week][prj]:
                                projectrow.append("%3.2f" % summaries[year][week][prj][weekday])
                            else:
                                projectrow.append("0")
                        projectrow.append(summaries[year][week][prj]["notes"])
                        table.append(projectrow)

            d = d + ONEDAY # INCREMENT

        table.append("")
        csvfile = "csv/" + os.path.splitext(file)[0] + "-Summary.csv"
        with open(csvfile, 'w') as f:
            # using csv.writer method from CSV package
            write = csv.writer(f, lineterminator="\n")
            write.writerows(table)

        tables["%4d-%02d" % (year, month)]=table
    return tables

def parse_projects():
    """Reads in Org exported CSV files and converts it into a
    dictionary of the form
    projects[year][month][project][day][hours/description]"""

    projects = OrderedDict()

    for file in get_csv_files():
        d = datetime.datetime.strptime(file.replace(".csv", ""), "%Y-%m")
        month = d.month
        year = d.year

        if (year not in projects):
            projects[year] = OrderedDict()

        projects[year][month] = csv_to_dict("csv/" + file)

    return projects


if __name__ == "__main__":
    summaries=project_to_summary(parse_projects())
    tables=create_summary_tables(summaries)
    # print_org_summary(project_to_summary(parse_projects()))
