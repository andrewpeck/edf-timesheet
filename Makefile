.PHONY: export_org_tables summary_tables summary_tables_html

all: export_org_tables db_tables summary plots

plots:
	@python3 timesheets.py

export_org_tables:
	@bash export_org_tables.sh billing.org
	@mv *.csv csv/

db_tables: sum_tables.py
	@python sum_tables.py > summary_tables.org
	@bash ./beautify_table.sh summary_tables.org
	@cat summary_tables.org
	@bash export_org_tables.sh summary_tables.org
	@mv *.csv csv/
	@scp csv/*-*Summary.csv ohm:~/billing/

# summary_tables_html: summary_tables
# 	@bash org_to_html.sh summary_tables.org
#@scp summary_tables.html ohm:~/public_html/notes/hours.html

summary:
	@awk -f timesheets.awk csv/20[0-9][0-9]-[0-9][0-9].csv
	@wordcloud_cli --text wordcloud.txt --imagefile wordcloud.png --background white --colormap "Solarize_Light2" --max_words 50 --width 1024 --height 768 --fontfile "DroidSans" --margin 4 --min_word_length 2 --prefer_horizontal 15 --no_collocations

clean:
	rm *.csv
	rm summary_tables.org
	rm summary_tables.html
