function Util () {}

/* Tag building */

Util.buildOpeningTag = function(tagName) {
    return '<' + tagName + '>';
};

Util.buildClosingTag = function(tagName) {
    return '</' + tagName + '>';
};

Util.wrapHTMLInTag = function(html, tagName) {
    return Util.buildOpeningTag(tagName) + html + Util.buildClosingTag(tagName);
};

/* Selection */

Util.rangeIsAtStartOfParent = function(range) {
    return (range.startContainer.previousSibling == null && range.startOffset == 0);
};

Util.rangeIsAtEndOfParent = function(range) {
    return ((range.startContainer.nextSibling == null || range.startContainer.nextSibling == "<br>")
        && range.endOffset == range.endContainer.length);
};