####
# Each function take a file as a parameter, the file will be modified inplace.

function fix_line_comments() {
    perl -i -pe 's+ //([^ ])+ // $1+g' $1
}

function remove_duplicate_white_spaces() {
    perl -i -pe 's/([^ ])  +/$1 /g' $1
}

function move_binary_operators() {
    perl -i -0pe 's/ \+\r?\n([ ]+)/\n$1+ /g' $1
    perl -i -0pe 's/ &&\r?\n([ ]+)/\n$1&& /g' $1
    perl -i -0pe 's/ \|\|\r?\n([ ]+)/\n$1|| /g' $1
    perl -i -0pe 's/ \|\r?\n([ ]+)/\n$1| /g' $1
}

function remove_trailing_white_spaces {
    perl -i -ple 's/\s+$//' $1
}

# !!!! This one is breaking one regexp (at least one) - makes sure to revert the change
function add_white_spaces_typecast() {
    perl -i -pe 's/\)([\(a-zA-Z])/) $1/g' $1
}

function add_white_space_after_if() {
    perl -i -pe 's/ if[\(]/ if (/g' $1
}

function remove_empty_lines_after_brace() {
    perl -i -0pe 's/({\r?\n)\r?\n/$1/g' $1
}

function remove_empty_lines_before_brace() {
    perl -i -0pe 's/\r?\n(\r?\n\s*})/$1/g' $1
}

for filename in $(find WordPress/src -type f -iname "*.java"); do
    fix_line_comments $filename
    move_binary_operators $filename
    remove_duplicate_white_spaces $filename
    remove_trailing_white_spaces $filename
    add_white_spaces_typecast $filename
    add_white_space_after_if $filename
    remove_empty_lines_after_brace $filename
    remove_empty_lines_before_brace $filename
done

