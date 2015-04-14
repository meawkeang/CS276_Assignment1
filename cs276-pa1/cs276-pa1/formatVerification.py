import tempfile
import os
import shutil
import subprocess
import re

passed = True
pattern = "\d/"
indexDir = tempfile.mkdtemp()
os.system("sh task1/index.sh toy_example/data " + indexDir)
for i in range(1, 6):
	cmd = "sh task1/query.sh " + indexDir + " < toy_example/queries/query.%d" %  i
	result = subprocess.check_output(cmd, shell=True)

	if i == 1:
		if result != "no results found\n":
			print "incorrect form for 'no results found'."
			passed = False

	else:

		if re.match(pattern, result) == None:
			print "incorrect form for docName."
			passed = False

if passed:
	print "Output format correct."

shutil.rmtree(indexDir)