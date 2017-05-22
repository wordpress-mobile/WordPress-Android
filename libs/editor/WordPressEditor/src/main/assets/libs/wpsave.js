/**
* The code of this function comes from the WordPress source code,
* from file `wp-admin/js/editor.js`, so we can get 100% consistent results
* with wp-admin.
*/

// Ensure the global `wp` object exists.
window.wp = window.wp || {};

(function(){

 /**
  *  @brief      Performs multiple transformations to the post source code when
  *              saving, removing paragraphs whenever possible.
  *
  *  @param      content   The string to transform
  *
  *  @return     Returns the transformed string
  */
  wp.saveText = function( content ) {

    if ( content == null || !content.trim() ) {
        // Just whitespace, null, or undefined
        return '';
    }
 
    var blocklist1, blocklist2,
        preserve_linebreaks = false,
        preserve_br = false;

    // Protect pre|script tags
    if ( content.indexOf( '<pre' ) !== -1 || content.indexOf( '<script' ) !== -1 ) {
      preserve_linebreaks = true;
      content = content.replace( /<(pre|script)[^>]*>[\s\S]+?<\/\1>/g, function( a ) {
        a = a.replace( /<br ?\/?>(\r\n|\n)?/g, '<wp-line-break>' );
        a = a.replace( /<\/?p( [^>]*)?>(\r\n|\n)?/g, '<wp-line-break>' );
        return a.replace( /\r?\n/g, '<wp-line-break>' );
      });
    }

    // keep <br> tags inside captions and remove line breaks
    if ( content.indexOf( '[caption' ) !== -1 ) {
      preserve_br = true;
      content = content.replace( /\[caption[\s\S]+?\[\/caption\]/g, function( a ) {
        return a.replace( /<br([^>]*)>/g, '<wp-temp-br$1>' ).replace( /[\r\n\t]+/, '' );
      });
    }

    // Pretty it up for the source editor
    blocklist1 = 'blockquote|ul|ol|li|table|thead|tbody|tfoot|tr|th|td|div|h[1-6]|p|fieldset';
    content = content.replace( new RegExp( '\\s*</(' + blocklist1 + ')>\\s*', 'g' ), '</$1>\n' );
    content = content.replace( new RegExp( '\\s*<((?:' + blocklist1 + ')(?: [^>]*)?)>', 'g' ), '\n<$1>' );

    // Mark </p> if it has any attributes.
    content = content.replace( /(<p [^>]+>.*?)<\/p>/g, '$1</p#>' );

    // Separate <div> containing <p>
    content = content.replace( /<div( [^>]*)?>\s*<p>/gi, '<div$1>\n\n' );

    // Remove <p> and <br />
    content = content.replace( /\s*<p>/gi, '' );
    content = content.replace( /\s*<\/p>\s*/gi, '\n\n' );
    content = content.replace( /\n[\s\u00a0]+\n/g, '\n\n' );
    content = content.replace( /\s*<br ?\/?>\s*/gi, '\n' );

    // Fix some block element newline issues
    content = content.replace( /\s*<div/g, '\n<div' );
    content = content.replace( /<\/div>\s*/g, '</div>\n' );
    content = content.replace( /\s*\[caption([^\[]+)\[\/caption\]\s*/gi, '\n\n[caption$1[/caption]\n\n' );
    content = content.replace( /caption\]\n\n+\[caption/g, 'caption]\n\n[caption' );

    blocklist2 = 'blockquote|ul|ol|li|table|thead|tbody|tfoot|tr|th|td|h[1-6]|pre|fieldset';
    content = content.replace( new RegExp('\\s*<((?:' + blocklist2 + ')(?: [^>]*)?)\\s*>', 'g' ), '\n<$1>' );
    content = content.replace( new RegExp('\\s*</(' + blocklist2 + ')>\\s*', 'g' ), '</$1>\n' );
    content = content.replace( /<li([^>]*)>/g, '\t<li$1>' );

    if ( content.indexOf( '<option' ) !== -1 ) {
      content = content.replace( /\s*<option/g, '\n<option' );
      content = content.replace( /\s*<\/select>/g, '\n</select>' );
    }

    if ( content.indexOf( '<hr' ) !== -1 ) {
      content = content.replace( /\s*<hr( [^>]*)?>\s*/g, '\n\n<hr$1>\n\n' );
    }

    if ( content.indexOf( '<object' ) !== -1 ) {
      content = content.replace( /<object[\s\S]+?<\/object>/g, function( a ) {
        return a.replace( /[\r\n]+/g, '' );
      });
    }

    // Unmark special paragraph closing tags
    content = content.replace( /<\/p#>/g, '</p>\n' );
    content = content.replace( /\s*(<p [^>]+>[\s\S]*?<\/p>)/g, '\n$1' );

    // Trim whitespace
    content = content.replace( /^\s+/, '' );
    content = content.replace( /[\s\u00a0]+$/, '' );

    // put back the line breaks in pre|script
    if ( preserve_linebreaks ) {
      content = content.replace( /<wp-line-break>/g, '\n' );
    }

    // and the <br> tags in captions
    if ( preserve_br ) {
      content = content.replace( /<wp-temp-br([^>]*)>/g, '<br$1>' );
    }

    return content;
  }

}());
