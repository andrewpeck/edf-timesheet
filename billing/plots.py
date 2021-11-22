import sys
import datetime
import dateutil
import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties
from statistics import mean

import sum_tables

# import pprint
# from collections import OrderedDict
# import altair as alt
# import pandas as pd
# from vega_datasets import data

def plot_table(figname, meetingize=False, zeynepize=False, indaraize=False,
               muonize=False, atlasize=False, etlize=False):
    ""

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
    print(start_date)

    for x in range(date_range+1):
        d = (d + datetime.timedelta(weeks=1))
        #date_list.append("%d/%d/%02d" % (d.year, d.month, d.day % 7 +1))
        date_list.append("%d/%d/%02d" % (d.year, d.month, (d.day) // 7))

    #print(date_list)

    work = sum_tables.parse_projects()

    meetings = 0
    not_meetings = 0

    # for year in work.keys():
    #     for month in work[year].keys():
    #         for prj in work[year][month].keys():
    #             for day in work[year][month][prj]:

    #                 is_meeting = False
    #                 hours = work[year][month][prj][day]["hours"]
    #                 description = work[year][month][prj][day]["description"].lower()

    #                 for mkey in ["meeting", "chat", "discuss"]:
    #                     if mkey in description:
    #                         is_meeting = True
    #                         print(description)

    #                 if (is_meeting):
    #                     meetings += hours
    #                 else:
    #                     not_meetings += hours

    # gather a list of the projects
    projects = {}
    for year in work.keys():
        for month in work[year].keys():
            for prj in work[year][month].keys():
                if prj not in projects:
                    if (prj not in ["SICK", "VAC", "--", "ADMIN", "HOLIDAY"]):
                        projects[prj] = []

    print("%30s, Month, Hours, Cost" % "Project")
    for prj in projects:

        date = start_date
        weeksum = 0
        monthsum = 0

        while date <= end_date:
            year = date.year

            # if its a new month, reset the sum
            if date.month != month:
                monthsum = 0

            month = date.month
            day = date.day
            weekday = date.weekday()

            if weekday == 0:
                weeksum = 0

            if prj in work[year][month]:
                if day in work[year][month][prj]:
                    if "hours" in work[year][month][prj][day]:
                        amt = work[year][month][prj][day]["hours"]
                        weeksum += amt
                        monthsum += amt

            if weekday == 6:
                projects[prj].append(weeksum)

            date += delta

            if date.month != month:
                if (year < 2021 or (year == 2021 and month < 7)):
                    rate = 70
                else:
                    rate = 89
                print("%30s, %5d,  %4.1f, $%6.1f" %
                      (prj, month, monthsum, monthsum * rate))

    y = []
    t = []

    print("")

    for prj in projects:
        y.append(projects[prj])
        t.append(prj)
    x = date_list  # range(len(y[0]))
    #x = range(len(y[0]))

    # pop off empty weeks at the end
    #
    busy = []
    for i in range(len(x)):
        busy.append(0)
        for prj in y:
            if (0 <= i < len(prj)):
                if prj[i] > 0:
                    busy[i] = 1

    weeks_to_pop = 0
    for i in range(len(busy) - 1, -1, -1):
        if busy[i] == 1:
            break
        weeks_to_pop += 1

    # print(weeks_to_pop)
    if weeks_to_pop > 0:
        for i in range(weeks_to_pop):
            x.pop()
            for i in range(len(y)):
                y[i].pop()

    # for i in x:
    #     sum_tables.princ("%s " % i)
    # sum_tables.princ("\n")

    # for i in y:
    #     for j in i:
    #         sum_tables.princ("%4.1f " % j)
    #     sum_tables.princ("\n")

    normalize = True

    if indaraize:
        indara = [0 for j in range(len(y[0]))]
        not_indara = [0 for j in range(len(y[0]))]

        for prj in projects:
            for day in range(len(y[0])):
                if (prj in ["CMS-ETL", "CMS-EMU-UPGRADE-ME0",
                            "CMS-EMU-UPGRADE-GE21",
                            "CMS-EMU-OPS-CSC", "CMS-EMU-OPS-GE11"]):
                    indara[day] += projects[prj][day]
                else:
                    not_indara[day] += projects[prj][day]

        y = [indara, not_indara]

    if muonize:
        muon = [0 for j in range(len(y[0]))]
        not_muon = [0 for j in range(len(y[0]))]

        for prj in projects:
            for day in range(len(y[0])):
                if (prj in ["CMS-EMU-UPGRADE-ME0",
                            "CMS-EMU-UPGRADE-GE21",
                            "CMS-EMU-OPS-CSC", "CMS-EMU-OPS-GE11"]):
                    muon[day] += projects[prj][day]
                else:
                    not_muon[day] += projects[prj][day]

        y = [muon, not_muon]

    if etlize:
        etl = [0 for j in range(len(y[0]))]
        not_etl = [0 for j in range(len(y[0]))]

        for prj in projects:
            for day in range(len(y[0])):
                if prj in ["CMS-ETL"]:
                    etl[day] += projects[prj][day]
                else:
                    not_etl[day] += projects[prj][day]

        y = [etl, not_etl]

    if atlasize:
        atlas = [0 for j in range(len(y[0]))]
        not_atlas = [0 for j in range(len(y[0]))]

        for prj in projects:
            for day in range(len(y[0])):
                if prj in ["ATLAS-MUON-PHASE2"]:
                    atlas[day] += projects[prj][day]
                else:
                    not_atlas[day] += projects[prj][day]

        y = [atlas, not_atlas]

    if zeynepize:
        zeynep = [0 for j in range(len(y[0]))]
        not_zeynep = [0 for j in range(len(y[0]))]

        for prj in projects:
            for day in range(len(y[0])):
                if prj in ["CMS-PIXEL-DTC", "APOLLO"]:
                    zeynep[day] += projects[prj][day]
                else:
                    not_zeynep[day] += projects[prj][day]

        y = [zeynep, not_zeynep]

    # for prj in projects:
    #     print(prj)
    #     print(projects[prj])

    # if (meetingize):
    #     meetings = [0 for j in range(len(y[0]))]
    #     not_meetings = [0 for j in range(len(y[0]))]

    #     for prj in projects:
    #         for day in range(len(y[0])):
    #             if (projects[prj][day]["
    #                 zeynep[day] +=
    #             else:
    #                 not_zeynep[day] += projects[prj][day]

    #     y = [zeynep, not_zeynep]

    if normalize:
        for week in range(len(y[0])):

            sum = 0

            for prj in range(len(y)):
                sum += y[prj][week]

            for prj in range(len(y)):
                if sum > 0:
                    y[prj][week] = y[prj][week]/sum

    plt.xticks(rotation=75, ha='right')

    fontP = FontProperties()
    fontP.set_size('x-small')
    plt.margins(x=0)
    plt.margins(y=0)

    if indaraize:
        x = x[0:len(indara)]
        xmean = (mean(indara))
        plt.stackplot(x, indara, not_indara, labels=["indara = %0.2f" % xmean, "not indara"],
                      colors=["#008080", "#abcdef"])
        plt.legend(loc='upper left')
        plt.savefig(figname, bbox_inches="tight")
    elif zeynepize:
        x = x[0:len(zeynep)]
        xmean = (mean(zeynep))
        plt.stackplot(x, zeynep, not_zeynep, labels=["zeynep = %0.2f" % xmean, "not zeynep"])
        plt.legend(loc='upper left')
        plt.savefig(figname, bbox_inches="tight")
    elif muonize:
        x = x[0:len(muon)]
        xmean = (mean(muon))
        plt.stackplot(x, muon, not_muon, labels=["muon = %0.2f" % xmean, "not muon"])
        plt.legend(loc='upper left')
        plt.savefig(figname, bbox_inches="tight")
    elif atlasize:
        x = x[0:len(atlas)]
        xmean = (mean(atlas))
        plt.stackplot(x, atlas, not_atlas, labels=["atlas = %0.2f" % xmean, "not atlas"])
        plt.legend(loc='upper left')
        plt.savefig(figname, bbox_inches="tight")
    elif etlize:
        x = x[0:len(etl)]
        xmean = (mean(etl))
        plt.stackplot(x, etl, not_etl, labels=["etl = %0.2f" % xmean, "not etl"])
        plt.legend(loc='upper left')
        plt.savefig(figname, bbox_inches="tight")
    else:
        # Basic stacked area chart.
        print(x)
        print(y[0])
        print(len(x))
        print(len(y[0]))

        x = x[0:len(y[0])]

        plt.stackplot(x, y, labels=t)
        plt.legend(bbox_to_anchor=(1.48, 0.9), loc='upper right', prop=fontP)
        plt.savefig(figname, bbox_inches="tight")
        # print (date_range)
        # print(len(x))
        # print(len(y[0]))
        # data = pd.DataFrame(
        #     {
        #         t[0]: y[0],
        #         t[1]: y[1],
        #         t[2]: y[1],
        #         t[3]: y[2],
        #         t[4]: y[3],
        #         t[5]: y[4],
        #         t[6]: y[5]
        #     } #, index=x
        #                           )
        # ax = data.plot.area()

        # print(data)
        # chart = alt.Chart(data).mark_area().encode().show()
        # chart.save(figname)

    # print("meetings: %d hours" % meetings)
    # print("not_meetings: %d hours" % not_meetings)
    # print("fraction of time in meetings = %f" % (meetings/(meetings + not_meetings)))

    return "./%s" % figname


if __name__ == "__main__":
    if sys.version_info < (3, 0, 0):
        print(__file__ + ' requires Python 3, while Python '
              + str(sys.version[0] + ' was detected. Terminating. '))
        sys.exit(1)
    plot_table("test.png")
