#!/usr/bin/env python

from collections import defaultdict
from pprint import pprint
import os
import sys

RESDIR="WordPress/src/main/res"
TO_CHECK = ["-hdpi", "-xhdpi", "-xxhdpi"]

def main():
    if len(sys.argv) == 2:
        check_drawables(sys.argv[1])
    else:
        check_drawables(RESDIR)

def check_drawables(rootdir):
    filenames = defaultdict(set)
    results =  defaultdict(set)
    # scan directories
    for subdir, dirs, files in os.walk(rootdir):
        for filename in files:
            for dpi in TO_CHECK:
                if subdir.endswith("drawable" + dpi) and \
                   filename.endswith("png"):
                   filenames[subdir].add(filename)
    # check missing drawables
    for subdir in filenames:
        for filename in filenames[subdir]:
            for sub in filenames:
                if sub == subdir:
                    continue
                if filename not in filenames[sub]:
                    results[filename].add(sub)
    # print results
    for i in results:
        print(i + " missing in: " + " ".join(results[i]))

if __name__ == "__main__":
    main()
