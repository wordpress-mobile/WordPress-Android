#!/bin/sh

if [ x"$1" == x ]; then
	compared_branch=develop
fi

current_branch=$(git rev-parse --abbrev-ref HEAD)
current_branch_filtered=$(echo $current_branch | tr "/" "-")

# save local changes if any

modified_files=$(git --no-pager diff develop --name-only | grep ".java$")

# Check style on current branch
checkstyle -c cq-configs/checkstyle/checkstyle.xml $modified_files > /tmp/checkstyle-$current_branch_filtered.log

# Check style on current develop
git stash | grep "No local changes to save" > /dev/null
needpop=$?
git checkout $compared_branch
checkstyle -c cq-configs/checkstyle/checkstyle.xml $modified_files > /tmp/checkstyle-develop.log

# Back on current branch
git checkout $current_branch

echo
echo --------------------------
echo The following warnings seem to be introduced by your branch:
diff -u /tmp/checkstyle-develop.log /tmp/checkstyle-$current_branch_filtered.log | grep "^+" | grep -v "^+++"

# restore local changes
if [ $needpop -eq 1 ]; then
    git stash pop > /dev/null
fi
