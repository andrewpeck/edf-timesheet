#!/usr/bin/env python3
import sum_tables
import numpy as np
import matplotlib.pyplot as plt
import datetime
import dateutil
from glob import glob

month_begin = 13
year_begin = 3000
month_end = 0
year_end = 0
delta = datetime.timedelta(days=1)


for file in glob("* *.csv"):
    d = datetime.datetime.strptime(file.replace(".csv",""), "%B %Y")
    month = d.month
    year = d.year
    if (month < month_begin and year <= year_begin):
        month_begin = month
        year_begin = year
        start_date = d
    if (month > month_end and year >= year_end):
        month_end = month
        year_end = year
        end_date = d + dateutil.relativedelta.relativedelta(day=31)

# print ("start=%d %d" % (month_begin, year_begin))
# print ("end=%d %d" % (month_end, year_end))
# print ("days = %d" % (end_date - start_date).days)

date_range = (end_date - start_date).days

work = sum_tables.parse_projects()

# gather a list of the projects
projects = {}
for year in work.keys():
    for month in work[year].keys():
        for prj in work[year][month].keys():
            if not prj in projects:
                if (prj not in ["SICK", "VAC", "--", "ADMIN", "HOLIDAY"]):
                    projects[prj]=[]

for prj in projects:

    date = start_date
    weeksum = 0
    while date <= end_date:
        year = date.year
        month = date.month
        day = date.day
        weekday = date.weekday()

        if (weekday==0):
            weeksum = 0

        if prj in work[year][month]:
            if day in work[year][month][prj]:
                if ("hours" in work[year][month][prj][day]):
                    weeksum += work[year][month][prj][day]["hours"]

        if (weekday==6):
            projects[prj].append(weeksum)

        date += delta

#print (projects)

# Create data
y = []
t = []
for prj in projects:
    y.append(projects[prj])
    t.append(prj)
x = range(len(y[0]))
print(y)

normalize = True

if (normalize):
    for i in range(len(y[0])):
        sum = 0

        for j in range(len(y)):
            sum += y[j][i]

        for j in range(len(y)):
            y[j][i] = y[j][i]/sum



# Basic stacked area chart.
plt.stackplot(x, y, labels=t)
plt.legend(loc='upper left')
plt.show()
