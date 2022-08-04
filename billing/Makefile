.PHONY: export_org_tables summary_tables summary_tables_html

all: export_org_tables summary_tables summary_tables_html

export_org_tables:
	@bash export_org_tables.sh billing.org

summary_tables: sum_tables.py
	@python sum_tables.py > summary_tables.org
	@bash ./beautify_table.sh summary_tables.org
	@cat summary_tables.org
	@bash export_org_tables.sh summary_tables.org

summary_tables_html: summary_tables
	@bash org_to_html.sh summary_tables.org
#@scp summary_tables.html ohm:~/public_html/notes/hours.html
	@scp *-*Summary.csv ohm:~/billing/

summary:
	@awk -f timesheets.awk 20[0-9][0-9]-[0-9][0-9].csv

clean:
	rm *.csv
	rm summary_tables.org
	rm summary_tables.html
