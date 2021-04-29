all: timestamp summary_tables.org summary_tables.html

timestamp: billing.org
	@sh export_org_tables.sh billing.org
	@touch timestamp

summary_tables.org: sum_tables.py billing.org
	@python sum_tables.py > summary_tables.org
	@sh ./beautify_table.sh summary_tables.org
	@cat summary_tables.org
	@sh export_org_tables.sh summary_tables.org

summary_tables.html: summary_tables.org
	@sh org_to_html.sh summary_tables.org
	@scp summary_tables.html ohm:~/public_html/notes/hours.html
	@scp *-*.csv ohm:~/billing/
