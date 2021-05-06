all: timestamp summary_tables.org summary_tables.html

export_org_tables.sh: billing.org
	@bash export_org_tables.sh billing.org
	@touch export_org_tables.sh

summary_tables.org: sum_tables.py billing.org
	@python sum_tables.py > summary_tables.org
	@bash ./beautify_table.sh summary_tables.org
	@cat summary_tables.org
	@bash export_org_tables.sh summary_tables.org

summary_tables.html: summary_tables.org
	@bash org_to_html.sh summary_tables.org
	@scp summary_tables.html ohm:~/public_html/notes/hours.html
	@scp *-*.csv ohm:~/billing/
