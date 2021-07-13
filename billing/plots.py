import sum_tables
import sys
import matplotlib.pyplot as plt
import datetime
import dateutil
import pprint
from collections import OrderedDict

def plot_table(figname, zeynepize=False, indaraize=False):

    month_begin = 13
    year_begin = 3000
    month_end = 0
    year_end = 0
    delta = datetime.timedelta(days=1)

    for file in sum_tables.get_csv_files():
        d = datetime.datetime.strptime(file.replace(".csv", ""), "%Y-%m")
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

    date_range = int((end_date - start_date).days / 7)
    date_list = []
    d = start_date
    for x in range (date_range):
        d = (d + datetime.timedelta(weeks=1))
        date_list.append("%d/%02d" % (d.month, d.day-d.day % 7 + 1))

    print(date_list)

    work = sum_tables.parse_projects()

    # gather a list of the projects
    projects = {}
    for year in work.keys():
        for month in work[year].keys():
            for prj in work[year][month].keys():
                if prj not in projects:
                    if (prj not in ["SICK", "VAC", "--", "ADMIN", "HOLIDAY"]):
                        projects[prj] = []

    for prj in projects:

        date = start_date
        weeksum = 0
        monthsum = 0

        while date <= end_date:
            year = date.year

            # if its a new month, reset the sum
            if (date.month != month):
                monthsum = 0

            month = date.month
            day = date.day
            weekday = date.weekday()

            if weekday == 0:
                weeksum = 0

            if prj in work[year][month]:
                if day in work[year][month][prj]:
                    if ("hours" in work[year][month][prj][day]):
                        amt = work[year][month][prj][day]["hours"]
                        weeksum += amt
                        monthsum += amt

            if (weekday == 6):
                projects[prj].append(weeksum)

            date += delta

            if (date.month != month):
                print("Project %s Month %d, accruals = %4.1f hours = $%6.1f" %
                      (prj, month, monthsum, monthsum*70.0))

    y = []
    t = []

    for prj in projects:
        y.append(projects[prj])
        t.append(prj)
    x = date_list  # range(len(y[0]))

    # pop off empty weeks at the end
    busy = []
    for i in range(len(x)):
        busy.append(0)
        for prj in y:
            if prj[i] > 0:
                busy[i] = 1

    weeks_to_pop = 0
    for i in range(len(busy)-1, -1, -1):
        if (busy[i] == 1):
            break
        else:
            weeks_to_pop += 1

    print(weeks_to_pop)
    if (weeks_to_pop > 0):
        for i in range(weeks_to_pop):
            x.pop()
            for i in range(len(y)):
                y[i].pop()

    for i in x:
        sum_tables.princ("%s " % i)
    sum_tables.princ("\n")

    for i in y:
        for j in i:
            sum_tables.princ("%4.1f " % j)
        sum_tables.princ("\n")

    normalize = True

    if (indaraize):
        indara = [0 for j in range(len(y[0]))]
        not_indara = [0 for j in range(len(y[0]))]

        for prj in projects:
            for day in range(len(y[0])):
                if (prj in ["CMS-ETL", "CMS-EMU-UPGRADE-ME0", "CMS-EMU-UPGRADE-GE21", "CMS-EMU-OPS-CSC","CMS-EMU-OPS-GE11"]):
                    indara[day] += projects[prj][day]
                else:
                    not_indara[day] += projects[prj][day]

        y=[indara,not_indara]

    if (zeynepize):
        zeynep = [0 for j in range(len(y[0]))]
        not_zeynep = [0 for j in range(len(y[0]))]

        for prj in projects:
            for day in range(len(y[0])):
                if (prj in ["CMS-PIXEL-DTC", "APOLLO"]):
                    zeynep[day] += projects[prj][day]
                else:
                    not_zeynep[day] += projects[prj][day]

        y=[zeynep,not_zeynep]

    if (normalize):
        for week in range(len(y[0])):

            sum = 0

            for prj in range(len(y)):
                sum += y[prj][week]

            for prj in range(len(y)):
                if sum>0:
                  y[prj][week] = y[prj][week]/sum

    plt.xticks(rotation=75, ha='right')

    if (indaraize):
        plt.stackplot(x, indara, not_indara, labels=["indara", "not indara"])
        plt.legend(loc='upper left')
        plt.savefig(figname)
    elif (zeynepize):
        plt.stackplot(x, zeynep, not_zeynep, labels=["zeynep", "not zeynep"])
        plt.legend(loc='upper left')
        plt.savefig(figname)
    else:
        # Basic stacked area chart.
        plt.stackplot(x, y, labels=t)
        plt.legend(loc='upper left')
        plt.savefig(figname)

    return "./%s" % figname

if __name__ == "__main__":
    if sys.version_info < (3,0,0):
        print(__file__ + ' requires Python 3, while Python ' +
              str(sys.version[0] + ' was detected. Terminating. '))
        sys.exit(1)
    plot_table("test.png", True)
