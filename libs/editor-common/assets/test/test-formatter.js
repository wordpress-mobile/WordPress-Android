var assert = require('chai').assert;
var underscore = require('../underscore-min.js');

// Set up globals needed by shortcode, wpload, and wpsave
global.window = {};
global._ = underscore;
global.wp = {};

// wp-admin libraries
var shortcode = require("../shortcode.js");
var wpload = require("../wpload.js");
var wpsave = require("../wpsave.js");

var formatterlib = require("../editor-utils-formatter.js");
var formatter = formatterlib.Formatter;

// Media strings

// Image strings
var imageSrc = 'content://com.android.providers.media.documents/document/image%3A12951';
var plainImageHtml = '<img src="' + imageSrc + '" alt="" class="wp-image-123 size-full" width="172" height="244">';
var imageWrappedInLinkHtml = '<a href="' + imageSrc + '">' + plainImageHtml + '</a>';

// Video strings
var videoSrc = 'content://com.android.providers.media.documents/document/video%3A12966';
var videoShortcode = '[video src="' + videoSrc + '" poster=""][/video]';
var videoHtml = '<span class="edit-container" contenteditable="false"><span class="delete-overlay"></span>' +
    '<video webkit-playsinline src="' + videoSrc + '" poster="" preload="metadata" onclick="" controls="controls">' +
    '</video></span>';

// VideoPress video strings
var vpVideoShortcode = '[wpvideo ABCD1234]';
var vpVideoHtml = '<span class="edit-container" contenteditable="false"><span class="delete-overlay"></span>' +
    '<video data-wpvideopress="ABCD1234" webkit-playsinline src="" preload="metadata" poster="svg/wpposter.svg" ' +
    'onclick="" onerror="ZSSEditor.sendVideoPressInfoRequest(\'ABCD1234\');"></video></span>';

describe('HTML to Visual formatter should correctly convert', function () {
  it('single-line HTML', function () {
    assert.equal('<p>Some text</p>\n', formatter.htmlToVisual('Some text'));
  });

  it('multi-paragraph HTML', function () {
    assert.equal('<p>Some text</p>\n<p>More text</p>\n', formatter.htmlToVisual('Some text\n\nMore text'));
  });

  testMediaParagraphWrapping('image wrapped in link', plainImageHtml, plainImageHtml);
  testMediaParagraphWrapping('image not wrapped in link', imageWrappedInLinkHtml, imageWrappedInLinkHtml);
  testMediaParagraphWrapping('non-VideoPress video', videoShortcode, videoHtml);
  testMediaParagraphWrapping('VideoPress video', vpVideoShortcode, vpVideoHtml);
});

function testMediaParagraphWrapping(mediaType, htmlModeMediaHtml, visualModeMediaHtml) {
  describe(mediaType, function () {
    it('alone in post', function () {
      var visualFormattingApplied = formatter.htmlToVisual(htmlModeMediaHtml);
      assert.equal('<p>' + visualModeMediaHtml + '</p>\n', visualFormattingApplied);

      var convertedToDivs = formatter.convertPToDiv(visualFormattingApplied).replace(/\n/g, '');
      assert.equal('<div>' + visualModeMediaHtml + '</div><div><br></div>', convertedToDivs);
    });

    it('with paragraphs above and below', function () {
      var imageBetweenParagraphs = 'Line 1\n\n' + htmlModeMediaHtml + '\n\nLine 2';

      var visualFormattingApplied = formatter.htmlToVisual(imageBetweenParagraphs);
      assert.equal('<p>Line 1</p>\n<p>' + visualModeMediaHtml + '</p>\n<p>Line 2</p>\n', visualFormattingApplied);

      var convertedToDivs = formatter.convertPToDiv(visualFormattingApplied).replace(/\n/g, '');
      assert.equal('<div>Line 1</div><div>' + visualModeMediaHtml + '</div><div>Line 2</div>', convertedToDivs);
    });

    it('with line breaks above and below', function () {
      var imageBetweenLineBreaks = 'Line 1\n' + htmlModeMediaHtml + '\nLine 2';

      var visualFormattingApplied = formatter.htmlToVisual(imageBetweenLineBreaks);
      assert.equal('<p>Line 1<br />\n' + visualModeMediaHtml + '<br />\nLine 2</p>\n', visualFormattingApplied);

      var convertedToDivs = formatter.convertPToDiv(visualFormattingApplied).replace(/\n/g, '');
      assert.equal('<div>Line 1</div><div>' + visualModeMediaHtml + '</div><div>Line 2</div>', convertedToDivs);
    });

    it('start of post, with paragraph underneath', function () {
      var imageFollowedByParagraph = htmlModeMediaHtml + '\n\nLine 2';

      var visualFormattingApplied = formatter.htmlToVisual(imageFollowedByParagraph);
      assert.equal('<p>' + visualModeMediaHtml + '</p>\n<p>Line 2</p>\n', visualFormattingApplied);

      var convertedToDivs = formatter.convertPToDiv(visualFormattingApplied).replace(/\n/g, '');
      assert.equal('<div>' + visualModeMediaHtml + '</div><div>Line 2</div>', convertedToDivs);
    });

    it('start of post, with line break underneath', function () {
      var imageFollowedByLineBreak = htmlModeMediaHtml + '\nLine 2';

      var visualFormattingApplied = formatter.htmlToVisual(imageFollowedByLineBreak);
      assert.equal('<p>' + visualModeMediaHtml + '<br \/>\nLine 2</p>\n', visualFormattingApplied);

      var convertedToDivs = formatter.convertPToDiv(visualFormattingApplied).replace(/\n/g, '');
      assert.equal('<div>' + visualModeMediaHtml + '</div><div>Line 2</div>', convertedToDivs);
    });

    it('end of post, with paragraph above', function () {
      var imageUnderParagraph = 'Line 1\n\n' + htmlModeMediaHtml;

      var visualFormattingApplied = formatter.htmlToVisual(imageUnderParagraph);
      assert.equal('<p>Line 1</p>\n<p>' + visualModeMediaHtml + '</p>\n', visualFormattingApplied);

      var convertedToDivs = formatter.convertPToDiv(visualFormattingApplied).replace(/\n/g, '');
      assert.equal('<div>Line 1</div><div>' + visualModeMediaHtml + '</div><div><br></div>', convertedToDivs);
    });

    it('end of post, with line break above', function () {
      var imageUnderLineBreak = 'Line 1\n' + htmlModeMediaHtml;

      var visualFormattingApplied = formatter.htmlToVisual(imageUnderLineBreak);
      assert.equal('<p>Line 1<br \/>\n' + visualModeMediaHtml + '</p>\n', visualFormattingApplied);

      var convertedToDivs = formatter.convertPToDiv(visualFormattingApplied).replace(/\n/g, '');
      assert.equal('<div>Line 1</div><div>' + visualModeMediaHtml + '</div><div><br></div>', convertedToDivs);
    });
  });
}
