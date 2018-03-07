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

function move_ternary_operators() {
    perl -i -0pe 's/ :\r?\n([ ]+)/\n$1: /g' $1
    perl -i -0pe 's/ \?\r?\n([ ]+)/\n$1? /g' $1
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

function add_space_between_bracket_and_brace() {
    # perl -i -pe 's/\){/\) {/g' $1
    perl -i -pe 's/\){\r?\n/\) {\n/g' $1
}

function add_space_around_loop_equals() {
    perl -i -pe 's/int i\=0;/int i \= 0;/g' $1
}

function add_space_around_equality_operator() {
    perl -i -pe 's/(?<=[a-z0-9)])\=\=(?=[a-z0-9])/ == /g' $1
}

function add_space_after_switch() {
    perl -i -pe 's/switch\(/switch \(/g' $1
}

function clean_up_java_files() {
    for filename in $(find $1 -type f -iname "*.java"); do
        fix_line_comments $filename
        move_binary_operators $filename
        move_ternary_operators $filename
        remove_duplicate_white_spaces $filename
        remove_trailing_white_spaces $filename
        # Following rule dangerously breaks some regex
        # add_white_spaces_typecast $filename
        add_white_space_after_if $filename
        remove_empty_lines_after_brace $filename
        remove_empty_lines_before_brace $filename
        add_space_between_bracket_and_brace $filename
        add_space_around_loop_equals $filename
        add_space_around_equality_operator $filename
        add_space_after_switch $filename
    done
}

function clean_up_xml_files() {
    for filename in $(find $1 -type f -iname "*.xml"); do
        remove_trailing_white_spaces $filename
    done
}

function print_help() {
    echo "Usage:   $0 FILETYPE DIR"
    echo "Example: $0 java WordPress/src"
    echo "Example: $0 xml libs/utils/WordPressUtils/src/main/res/values/"
    echo "Supported FILETYPEs: java, xml"
}

function check_args() {
    if [ x"$1" == x ]; then
        echo "Invalid argument"
        print_help
        exit 2
    fi
}

case $1 in
    java)
        check_args $2
        clean_up_java_files $2;;
    xml)
        check_args $2
        clean_up_xml_files $2;;
    *)
        print_help;;
esac
