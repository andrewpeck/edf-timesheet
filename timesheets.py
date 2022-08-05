#!/usr/bin/env python3
import pandas as pd
import matplotlib.pyplot as plt

for fname in ["monthly_histo.txt", "yearly_histo.txt"]:

    df = pd.read_csv(fname, delim_whitespace=True)

    if fname=="monthly_histo.txt":
        fmt = "%Y/%m"
        suffix = "monthly"
        df["Date"] = pd.to_datetime(df["Date"], format=fmt)
    if fname=="yearly_histo.txt":
        fmt = "%Y"
        suffix = "yearly"

    print(df)

    plt.style.use("ggplot")

    df.head()

    plt.rcParams["figure.figsize"] = [7.50, 5]
    plt.rcParams["figure.autolayout"] = True

    if (suffix=="monthly"):
        df.plot.area(x="Date")
    if (suffix=="yearly"):
        df.plot.bar(x="Date", stacked=True)

    plt.title('EDF Work')
    plt.legend(bbox_to_anchor=(1.0, 1.0))
    plt.savefig('timesheet' + suffix + '.svg')
    #plt.show()
