function Formatter () {}

// Video format tags supported by the [video] shortcode: https://codex.wordpress.org/Video_Shortcode
// mp4, m4v and webm prioritized since they're supported by the stock player as of Android API 23
Formatter.videoShortcodeFormats = ["mp4", "m4v", "webm", "ogv", "wmv", "flv"];

Formatter.htmlToVisual = function(html) {
    var mutatedHTML = wp.loadText(html);

    // Perform extra transformations to properly wrap captioned images in paragraphs
    mutatedHTML = mutatedHTML.replace(/^\[caption([^\]]*\])/igm, '<p>[caption$1');
    mutatedHTML = mutatedHTML.replace(/([^\n>])\[caption/igm, '$1<br />\n[caption');
    mutatedHTML = mutatedHTML.replace(/\[\/caption\]\n(?=<|$)/igm, '[/caption]</p>\n');
    mutatedHTML = mutatedHTML.replace(/\[\/caption\]\n(?=[^<])/igm, '[/caption]<br />\n');

    return Formatter.applyVisualFormatting(mutatedHTML);
}

Formatter.convertPToDiv = function(html) {
    // Replace the paragraph tags we get from wpload with divs
    var mutatedHTML = html.replace(/(<p(?=[>\s]))/igm, '<div').replace(/<\/p>/igm, '</div>');

    // Replace break tags around media items with paragraphs
    // The break tags appear when text and media are separated by only a line break rather than a paragraph break,
    // which can happen when inserting media inline and switching to HTML mode and back, or by deleting line breaks
    // in HTML mode
    mutatedHTML = mutatedHTML.replace(/<br \/>(?=\s*(<img|<a href|<label|<video|<span class="edit-container"))/igm,
            '</div><div>');
    mutatedHTML = mutatedHTML.replace(/(<img [^<>]*>|<\/a>|<\/label>|<\/video>|<\/span>)<br \/>/igm,
            function replaceBrWithDivs(match) { return match.substr(0, match.length - 6) + '</div><div>'; });

    // Append paragraph-wrapped break tag under media at the end of a post
    mutatedHTML = mutatedHTML.replace(/(<img [^<>]*>|<\/a>|<\/label>|<\/video>|<\/span>)[^<>]*<\/div>\s$/igm,
            function replaceBrWithDivs(match) { return match + '<div><br></div>'; });

    return mutatedHTML;
}

Formatter.visualToHtml = function(html) {
    return wp.saveText(html);
    return Formatter.removeVisualFormatting(mutatedHTML);
}

Formatter.convertDivToP = function(html) {
    return html.replace(/(<div(?=[>\s]))/igm, '<p').replace(/<\/div>/igm, '</p>');
}

/**
 *  @brief      Applies editor specific visual formatting.
 *
 *  @param      html   The markup to format
 *
 *  @return     Returns the string with the visual formatting applied.
 */
Formatter.applyVisualFormatting  = function(html) {
    var str = wp.shortcode.replace('caption', html, Formatter.applyCaptionFormatting);
    str = wp.shortcode.replace('wpvideo', str, Formatter.applyVideoPressFormattingCallback);
    str = wp.shortcode.replace('video', str, Formatter.applyVideoFormattingCallback);

    // More tag
    str = str.replace(/<!--more(.*?)-->/igm, "<hr class=\"more-tag\" wp-more-data=\"$1\">")
    str = str.replace(/<!--nextpage-->/igm, "<hr class=\"nextpage-tag\">")
    return str;
}

/**
 *  @brief      Adds visual formatting to a caption shortcodes.
 *
 *  @param      html   The markup containing caption shortcodes to process.
 *
 *  @return     The html with caption shortcodes replaced with editor specific markup.
 *  See shortcode.js::next or details
 */
Formatter.applyCaptionFormatting = function(match) {
    var attrs = match.attrs.named;
    // The empty 'onclick' is important. It prevents the cursor jumping to the end
    // of the content body when `-webkit-user-select: none` is set and the caption is tapped.
    var out = '<label class="wp-temp" data-wp-temp="caption" onclick="">';
    out += '<span class="wp-caption"';

    if (attrs.width) {
        out += ' style="width:' + attrs.width + 'px; max-width:100% !important;"';
    }
    for (var key in attrs) {
        out += " data-caption-" + key + '="' + attrs[key] + '"';
    }

    out += '>';
    out += match.content;
    out += '</span>';
    out += '</label>';

    return out;
}

Formatter.applyVideoPressFormattingCallback = function(match) {
    if (match.attrs.numeric.length == 0) {
        return match.content;
    }
    var videopressID = match.attrs.numeric[0];
    var posterSVG = '"svg/wpposter.svg"';
    // The empty 'onclick' is important. It prevents the cursor jumping to the end
    // of the content body when `-webkit-user-select: none` is set and the video is tapped.
    var out = '<video data-wpvideopress="' + videopressID + '" webkit-playsinline src="" preload="metadata" poster='
           + posterSVG +' onclick="" onerror="ZSSEditor.sendVideoPressInfoRequest(\'' + videopressID +'\');"></video>';

    // Wrap video in edit-container node for a permanent delete button overlay
    var containerStart = '<span class="edit-container" contenteditable="false"><span class="delete-overlay"></span>';
    out = containerStart + out + '</span>';

    return out;
}

Formatter.applyVideoFormattingCallback = function(match) {
    // Find the tag containing the video source
    var srcTag = "";

    if (match.attrs.named['src']) {
        srcTag = "src";
    } else {
        for (var i = 0; i < Formatter.videoShortcodeFormats.length; i++) {
            var format = Formatter.videoShortcodeFormats[i];
            if (match.attrs.named[format]) {
                srcTag = format;
                break;
            }
        }
    }

    if (srcTag.length == 0) {
        return match.content;
    }

    var out = '<video webkit-playsinline src="' + match.attrs.named[srcTag] + '"';

    // Preserve all existing tags
    for (var item in match.attrs.named) {
        if (item != srcTag) {
            out += ' ' + item + '="' + match.attrs.named[item] + '"';
        }
    }

    if (!match.attrs.named['preload']) {
        out += ' preload="metadata"';
    }

    out += ' onclick="" controls="controls"></video>';

    // Wrap video in edit-container node for a permanent delete button overlay
    var containerStart = '<span class="edit-container" contenteditable="false"><span class="delete-overlay"></span>';
    out = containerStart + out + '</span>';

    return out;
}

if (typeof module !== 'undefined' && module.exports != null) {
    exports.Formatter = Formatter;
}