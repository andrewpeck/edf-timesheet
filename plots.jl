using CSV
using Printf
using Dates
#using PyCall
using DataFrames
#using Query
#https://stackoverflow.com/questions/44780761/converting-csv-to-json-in-bash
# https://julia.school/julia/dataframes/
# https://dataframes.juliadata.org/v0.18.3/man/getting_started.html

#sum_tables = pyimport("sum_tables")

function get_csv_files()
    return sort!(filter(s->occursin(r".[0-9]+-[0-9]+\.csv$",s),readdir()))
end

function get_date_range()
    dates = (sort(map(file -> DateTime(replace(file,".csv" => ""),"yyyy-mm"), get_csv_files())))
    start = first(dates)
    finish = last(dates)
    print(start, finish)
end

function parse_projects()
    return sum_tables.parse_projects()
end

function filename_to_datetime(file)
    return(DateTime(replace(file,".csv" => ""),"yyyy-mm"))
end

"""Reads in a list of CSV files and dumps them into a dataframe"""
function read_csvs_to_df!(files)
    df = DataFrame(Y = Int[], M = Int[], D = Int[], Hours = Float16[], Project = String[])
    for file in files
        date = filename_to_datetime(file)
        for row in CSV.File(file)
            if (! isequal(missing,row.D))
                (year, month, day, hours, project) = (Dates.year(date), Dates.month(date), row.D, row.Hours, row.Project)
                push!(df, (year, month, day, hours, project))
            end
        end
    end
    return df
end

"""Transform a dataframe to change names to canonical EDF database names"""
function substitute_project_names!(df)
    for i in eachrow(df)
        for replacement in ["ETL" => "CMS-ETL",
                            "ETL-RB" => "CMS-ETL",
                            "ETL-MODULE" => "CMS-ETL",
                            "GE21" => "CMS-EMU-UPGRADE-GE21",
                            "CSC" => "CMS-EMU-OPS-CSC",
                            "GE11" => "CMS-EMU-OPS-GE11",
                            "ME0" => "CMS-EMU-UPGRADE-ME0",
                            "IPMC" => "CMS-PIXEL-DTC",
                            "ATLAS" => "ATLAS-MUON-PHASE2",
                            "L0MDT" => "ATLAS-MUON-PHASE2",
                            "APOLLO-IPMC" => "APOLLO",
                            "TRACKER" => "CMS-PIXEL-DTC",
                            "VACATION" => "VAC"]
            i[:Project] = replace(i[:Project], replacement)
        end
    end
    return df
end

df = read_csvs_to_df!(get_csv_files())
substitute_project_names!(df)
println(df)
println(describe(df))

function get_project_df(project, df)
    result = filter(
        x -> any(occursin.([project], x.Project)),
        df
    )
    return result
end

l0mdt = get_project_df("ATLAS-MUON-PHASE-2", df)

println(l0mdt)
println(describe(l0mdt))
