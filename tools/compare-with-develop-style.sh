#!/bin/sh

CONFIG_FILE=cq-configs/checkstyle/checkstyle.xml
cp $CONFIG_FILE /tmp/checkstyle.xml

if [ x"$1" == x ]; then
	compared_branch=develop
fi

current_branch=$(git rev-parse --abbrev-ref HEAD)
current_branch_filtered=$(echo $current_branch | tr "/" "-")

# save local changes if any

modified_files=$(git --no-pager diff develop --name-only | grep ".java$")

# Check style on current branch
checkstyle -c /tmp/checkstyle.xml $modified_files | sed "s/:[0-9]*//g" > /tmp/checkstyle-$current_branch_filtered.log

# Check style on current develop
git stash | grep "No local changes to save" > /dev/null
needpop=$?
git checkout $compared_branch
checkstyle -c /tmp/checkstyle.xml $modified_files | sed "s/:[0-9]*//g" > /tmp/checkstyle-develop.log

# Back on current branch
git checkout $current_branch

echo
echo --------------------------
echo The following warnings seem to be introduced by your branch:
diff -u /tmp/checkstyle-develop.log /tmp/checkstyle-$current_branch_filtered.log > /tmp/checkstyle.diff
cat /tmp/checkstyle.diff | grep "^+" | grep -v "^+++" || echo Yay no new style errors!!
echo Style errors removed:
cat /tmp/checkstyle.diff | grep "^-" | wc -l

# restore local changes
if [ $needpop -eq 1 ]; then
    git stash pop > /dev/null
fi
