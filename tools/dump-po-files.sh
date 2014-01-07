LANGS=$(cat tools/language-codes.csv|cut -d "," -f1)
mkdir po-files
for lang in $LANGS; do
    curl "http://translate.wordpress.org/projects/android/dev/$lang/default/export-translations" > po-files/"$lang".po
done
