#!/usr/bin/env python3
import pandas as pd
import matplotlib.pyplot as plt

df = pd.read_csv("monthly_histo.txt", delim_whitespace=True)

print(df.keys())
df["Date"] = pd.to_datetime(df["Date"], format="%Y/%m")

print(df)

df.head()

plt.rcParams["figure.figsize"] = [7.50, 5]
plt.rcParams["figure.autolayout"] = True

df.plot.area(x="Date")

plt.title('EDF Work')
plt.legend(bbox_to_anchor=(1.0, 1.0))
plt.show()
