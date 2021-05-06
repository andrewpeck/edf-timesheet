import csv
import datetime
import pprint
import re
import sys
from glob import glob

ONEDAY = datetime.timedelta(days=1)

def princ (string):
    print (string, end='')

def substitute_project_name (name):
    name = name.replace("ETL","CMS-ETL")
    name = name.replace("GE21","CMS-EMU-UPGRADE-GE21")
    name = name.replace("CSC","CMS-EMU-OPS-CSC")
    name = name.replace("GE11","CMS-EMU-OPS-GE11")
    name = name.replace("ME0","CMS-EMU-UPGRADE-ME0")
    name = name.replace("L0MDT","ATLAS-MUON-PHASE2")
    name = name.replace("TRACKER","CMS-PIXEL-DTC")
    name = name.replace("VACATION","VAC")
    return name

def csv_to_dict (file):

    with open(file) as csvfile:

        projects = {}

        table = csv.reader(csvfile, delimiter=",")
        for row in table:
            if is_float(row[6]):
                day = int(row[1])
                project = substitute_project_name(row[3].upper())
                description = row[4]
                hours = float(row[6])

                if not project in projects:
                    projects [project] = {}

                if not day in projects[project]:
                    projects [project][day] = {"hours" : 0.0, "description" : ""}

                projects [project][day]["hours"] += hours
                projects [project][day]["description"] += description + "; "

                #print ("%d %s %s %3.2f" % (day, project, description, hours))

        return projects

def is_float(string):
    try:
        float(string)
        return True
    except ValueError:
        return False

def print_spacer ():
    spacer = "|----------------------+-------+-------+-------+-------+-------+-------+-------+-------|"
    print(spacer)

def print_weekdays (time):
    d = time
    # print the day number headings
    princ("| %20s |" % "")
    for weekday in range(7):
        if (d.day < 7 and d.weekday() > weekday):
            diff = d.weekday() - weekday
            princ ("% 6d*|" % (d-diff*ONEDAY).day )
        else:
            if (d.month==month):
                princ ("% 6d |" % d.day)
            else:
                princ ("% 6d*|" % d.day)
            d = d + ONEDAY
    princ("       |\n")

def check_project_active (name, time, dict):
    d = time
    for i in range(7):
        if (d.month in projects):
            if (prj in projects[d.month]):
                if (d.day in projects[d.month][prj]):
                    if (projects[d.month][prj][d.day]["hours"] > 0):
                        return True
        d = d + ONEDAY # INCREMENT


projects = {}
summaries = {}

if sys.version_info < (3,0,0):
    print(__file__ + ' requires Python 3, while Python ' + str(sys.version[0] + ' was detected. Terminating. '))
    sys.exit(1)

for file in glob("* *.csv"):
    d = datetime.datetime.strptime(file.replace(".csv",""), "%B %Y")
    month = d.month
    year = d.year

    if (not year in projects):
        projects[year]={}

    projects[year][month] = csv_to_dict (file)

def project_to_summary():

    # pp = pprint.PrettyPrinter(indent=4)
    # pp.pprint(projects)

    # d = datetime.datetime.strptime(file.replace(".csv",""), "%B %Y")

    # month = d.month
    # year = d.year
    # week = d.isocalendar()[1]

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
                    if not year in summaries:
                        summaries [year] = {}
                    if not week in summaries[year]:
                        summaries [year][week] = {}
                    if not prj in summaries[year][week]:
                        summaries [year][week][prj] = {}
                    if not weekday in summaries[year][week][prj]:
                        summaries [year][week][prj][weekday] = 0
                    if not "notes" in summaries[year][week][prj]:
                        summaries [year][week][prj]["notes"] = ""

                    summaries[year][week][prj][weekday] += hours
                    summaries[year][week][prj]["notes"] += description

                    #print (week)

    for year in summaries:
        for week in summaries[year]:
            for prj in summaries[year][week]:
                notes = summaries[year][week][prj]["notes"]

                # conditioning of the text
                notes = re.sub(r"\s+"," ",notes) # REMOVE DUPLICATE SPACES
                notes = re.sub(r",", ";",notes) # CONVERT COMMAS TO SEMICOLONS
                notes = re.sub(r"; ", ";",notes) # REMOVE SPACE SEPARATORS

                notes = re.split(";", notes)
                notes.sort()
                notes = list(set(notes)) # REMOVE DUPLICATES
                notes = ' '.join([str(elem)+";" for elem in notes]) # CONVERT FROM LIST TO STRING
                notes = re.sub(r"^;\s*","",notes) # REMOVE THE FIRST SEMICOLON
                notes = re.sub(r";\s*$","",notes) # REMOVE THE LAST SEMICOLON
                summaries[year][week][prj]["notes"] = notes

    # pp = pprint.PrettyPrinter(indent=4)
    # pp.pprint(summaries)

project_to_summary()

print("#+TITLE: Hours")
for file in glob("* *.csv"):

     d = datetime.datetime.strptime(file.replace(".csv",""), "%B %Y")

     month = d.month
     year = d.year

     d = datetime.datetime(year,month,1)

     print("")
     print("* %4d-%02d" % (year, month))
     print("#+TBLNAME: %4d-%02d" % (year, month))
     print("| %20s |    Mo |    Tu |    We |    Th |    Fr |    Sa |    Su | Notes |" %
           ("%04d-%02d" % (year, month) ))
     print_spacer()

     while (d.month == month):

        if (d.day==1 or d.weekday() == 0):
            print_weekdays(d)
            print_spacer()
            week = d.isocalendar()[1]

            if (week in summaries[year]):
                for prj in summaries[year][week]:
                    if (prj == "--"):
                        continue
                    princ ("| %20s |" % prj)
                    for weekday in range(7):
                        if weekday in summaries[year][week][prj]:
                            princ (" % 3.2f |" % summaries[year][week][prj][weekday])
                        else:
                            princ ("     0 |")
                    princ("%s | \n" % summaries[year][week][prj]["notes"])

        if (d.weekday()==6):
            print_spacer()

        d = d + ONEDAY # INCREMENT
     print_spacer()
