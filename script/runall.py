#!/usr/bin/evn python3

import subprocess
import time
import pandas as pd
import numpy as np
import re

# Number of days per month in 2020
y2020 = { 1: 31,
          2: 29,
          3: 31,
          4: 30,
          5: 31,
          6: 30,
          7: 31,
          8: 31,
          9: 30,
         10: 31,
         11: 30,
         12: 31 }

# Number of days per month in 2021
y2021 = { 1: 31,
          2: 28,
          3: 31,
          4: 30,
          5: 31,
          6: 30,
          7: 31,
          8: 31,
          9: 30,
         10: 31,
         11: 30,
         12: 31 }

days_in_month = {2020: y2020, 2021: y2021}

print("Initiating testing sequence...")

# Construct arguments
startLng = "-3.188396"
startLat = "55.944425" 

time_for_each_day = np.array([], dtype=np.int64)
sensors_found_for_each_day = np.array([], dtype=np.float64)
battery_for_each_day = np.array([], dtype=np.int64)

failed = 0
for year in [2020, 2021]:
    for month in range(1, 13):
        for day in range(1, days_in_month[year][month]):
            try:
                print("\n{:02d}/{:02d}/{}".format(day, month, year))
                tic = time.clock_gettime(time.CLOCK_MONOTONIC)     
                res = subprocess.run(["java", "-jar", "../aqmaps/target/aqmaps-0.0.1-SNAPSHOT.jar",
                    "{:02d}".format(day), "{:02d}".format(month), "{:02d}".format(year), startLat, startLng, "5678", "9898"], 
                                     check=True, capture_output=True)
                time_taken = time.clock_gettime(time.CLOCK_MONOTONIC) - tic
                output = res.stdout.decode("utf-8")
                sensors_found = re.search("([0-9]+)/33", output).group(1)
                battery = re.search("Battery: ([0-9]+)", output).group(1)
                
                time_for_each_day = np.append(time_for_each_day, time_taken)
                sensors_found_for_each_day = np.append(sensors_found_for_each_day, int(sensors_found))
                battery_for_each_day = np.append(battery_for_each_day, int(battery))
                print(output)
                print("Time: {}".format(time_taken))
            except subprocess.CalledProcessError as e:
                print(e)
                failed += 1

print("Testing finished...{} failed".format(failed))

times_pd = pd.Series(time_for_each_day, name="Times")
found_pd = pd.Series(sensors_found_for_each_day, name="Sensors")
battery_pd = pd.Series(battery_for_each_day, name="Battery")

times_pd.to_csv("times.csv")
found_pd.to_csv("sensors.csv")
battery_pd.to_csv("battery.csv")


print("\n\nTimes")
print(times_pd.describe())
print(times_pd)

print("\n\nSensors")
print(found_pd.describe())
print(found_pd)

print("\n\nBattery")
print(battery_pd.describe())
print(battery_pd)
