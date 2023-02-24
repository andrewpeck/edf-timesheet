.PHONY: export_org_tables summary_tables summary_tables_html

all: export_org_tables db_tables summary plots

plots:
	@clj plot-timesheets.clj

export_org_tables:
	@./export-org-tables billing.org
	@mv *.csv csv/

db_tables: sum_tables.py
	@python sum_tables.py
	@scp -r csv/*-Summary.csv ohm:~/billing/

summary:
	@awk -f timesheets.awk csv/20[0-9][0-9]-[0-9][0-9].csv
	@wordcloud_cli --text wordcloud.txt --imagefile wordcloud.svg --background white --colormap "Solarize_Light2" --max_words 50 --width 800 --height 600 --fontfile "DroidSans" --margin 4 --min_word_length 2 --prefer_horizontal 15 --no_collocations

clean:
	rm *.csv
	rm summary_tables.org
	rm summary_tables.html
