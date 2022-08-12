#!/usr/bin/env python3
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.pyplot import pie, axis, show

plt.style.use("fivethirtyeight")

df = pd.read_csv("accruals.txt", delim_whitespace=True, header=None)
df.columns = ["Project", "Year/Month", "Hours", "hrs", "Percent", "Dollars"]
df['Project'] = df['Project'].map(lambda x : "OTHER" if x == "CSC" or x == "EMPHATIC" else x)
df['Project'] = df['Project'].map(lambda x : "GEM" if x == "GE11" or x == "GE21" else x)
df = df.groupby(df["Project"]).sum()
df.plot.pie(y='Hours',legend=None, title=None)
plt.ylabel("")
plt.savefig('timesheet_pi.svg')


plt.style.use("ggplot")
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
