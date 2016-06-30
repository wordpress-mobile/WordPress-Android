/*!
 *
 * ZSSRichTextEditor v1.0
 * http://www.zedsaid.com
 *
 * Copyright 2013 Zed Said Studio
 *
 */

// If we are using iOS or desktop
var isUsingiOS = false;
var isUsingAndroid = true;

// THe default callback parameter separator
var defaultCallbackSeparator = '~';

const NodeName = {
    BLOCKQUOTE: "BLOCKQUOTE",
    PARAGRAPH: "P",
    STRONG: "STRONG",
    DEL: "DEL",
    EM: "EM",
    A: "A",
    OL: "OL",
    UL: "UL",
    LI: "LI",
    CODE: "CODE",
    SPAN: "SPAN",
    BR: "BR",
    DIV: "DIV",
    BODY: "BODY"
};

// The editor object
var ZSSEditor = {};

// These variables exist to reduce garbage (as in memory garbage) generation when typing real fast
// in the editor.
//
ZSSEditor.caretArguments = ['yOffset=' + 0, 'height=' + 0];
ZSSEditor.caretInfo = { y: 0, height: 0 };

// Is this device an iPad
ZSSEditor.isiPad;

// The current selection
ZSSEditor.currentSelection;

// The current editing image
ZSSEditor.currentEditingImage;

// The current editing video
ZSSEditor.currentEditingVideo;

// The current editing link
ZSSEditor.currentEditingLink;

ZSSEditor.focusedField = null;

// The objects that are enabled
ZSSEditor.enabledItems = {};

ZSSEditor.editableFields = {};

ZSSEditor.lastTappedNode = null;

// The default paragraph separator
ZSSEditor.defaultParagraphSeparator = 'div';

// We use a MutationObserver to catch user deletions of uploading or failed media
// This is only officially supported on API>18; when the WebView doesn't recognize the MutationObserver,
// we fall back to the deprecated DOMNodeRemoved event
ZSSEditor.mutationObserver;

ZSSEditor.defaultMutationObserverConfig = { attributes: false, childList: true, characterData: false };

/**
 * The initializer function that must be called onLoad
 */
ZSSEditor.init = function() {

    rangy.init();

    // Change a few CSS values if the device is an iPad
    ZSSEditor.isiPad = (navigator.userAgent.match(/iPad/i) != null);
    if (ZSSEditor.isiPad) {
        $(document.body).addClass('ipad_body');
        $('#zss_field_title').addClass('ipad_field_title');
        $('#zss_field_content').addClass('ipad_field_content');
    }

    document.execCommand('insertBrOnReturn', false, false);

    var editor = $('div.field').each(function() {
        var editableField = new ZSSField($(this));
        var editableFieldId = editableField.getNodeId();

        ZSSEditor.editableFields[editableFieldId] = editableField;
        ZSSEditor.callback("callback-new-field", "id=" + editableFieldId);
    });

	document.addEventListener("selectionchange", function(e) {
		ZSSEditor.currentEditingLink = null;
		// DRM: only do something here if the editor has focus.  The reason is that when the
		// selection changes due to the editor loosing focus, the focusout event will not be
		// sent if we try to load a callback here.
		//
		if (editor.is(":focus")) {
            ZSSEditor.selectionChangedCallback();
            ZSSEditor.sendEnabledStyles(e);
			var clicked = $(e.target);
			if (!clicked.hasClass('zs_active')) {
				$('img').removeClass('zs_active');
			}
		}
	}, false);

    // Attempt to instantiate a MutationObserver. This should fail for API<19, unless the OEM of the device has
    // modified the WebView. If it fails, the editor will fall back to DOMNodeRemoved events.
    try {
        ZSSEditor.mutationObserver = new MutationObserver(function(mutations) {
            ZSSEditor.onMutationObserved(mutations);} );
    } catch(e) {
        // no op
    }

}; //end

// MARK: - Debugging logs

ZSSEditor.logMainElementSizes = function() {
    msg = 'Window [w:' + $(window).width() + '|h:' + $(window).height() + ']';
    this.log(msg);

    var msg = encodeURIComponent('Viewport [w:' + window.innerWidth + '|h:' + window.innerHeight + ']');
    this.log(msg);

    msg = encodeURIComponent('Body [w:' + $(document.body).width() + '|h:' + $(document.body).height() + ']');
    this.log(msg);

    msg = encodeURIComponent('HTML [w:' + $('html').width() + '|h:' + $('html').height() + ']');
    this.log(msg);

    msg = encodeURIComponent('Document [w:' + $(document).width() + '|h:' + $(document).height() + ']');
    this.log(msg);
};

// MARK: - Viewport Refreshing

ZSSEditor.refreshVisibleViewportSize = function() {
    $(document.body).css('min-height', window.innerHeight + 'px');
    $('#zss_field_content').css('min-height', (window.innerHeight - $('#zss_field_content').position().top) + 'px');
};

// MARK: - Fields

ZSSEditor.focusFirstEditableField = function() {
    $('div[contenteditable=true]:first').focus();
};

ZSSEditor.formatNewLine = function(e) {

    var currentField = this.getFocusedField();

    if (currentField.isMultiline()) {
        var parentBlockQuoteNode = ZSSEditor.closerParentNodeWithName('blockquote');

        if (parentBlockQuoteNode) {
            this.formatNewLineInsideBlockquote(e);
        } else if (!ZSSEditor.isCommandEnabled('insertOrderedList')
                   && !ZSSEditor.isCommandEnabled('insertUnorderedList')) {
            document.execCommand('formatBlock', false, 'div');
        }
    } else {
        e.preventDefault();
    }
};

ZSSEditor.formatNewLineInsideBlockquote = function(e) {
    this.insertBreakTagAtCaretPosition();
    e.preventDefault();
};

ZSSEditor.getField = function(fieldId) {

    var field = this.editableFields[fieldId];

    return field;
};

ZSSEditor.getFocusedField = function() {
    var currentField = $(this.findParentContenteditableDiv());
    var currentFieldId;

    if (currentField) {
        currentFieldId = currentField.attr('id');
    }

    if (!currentFieldId) {
        ZSSEditor.resetSelectionOnField('zss_field_content');
        currentFieldId = 'zss_field_content';
    }

    return this.editableFields[currentFieldId];
};

ZSSEditor.execFunctionForResult = function(methodName) {
    var functionArgument = "function=" + methodName;
    var resultArgument = "result=" + window["ZSSEditor"][methodName].apply();
    ZSSEditor.callback('callback-response-string', functionArgument +  defaultCallbackSeparator + resultArgument);
};

// MARK: - Mutation observing

/**
 *  @brief      Register a node to be tracked for modifications
 */
ZSSEditor.trackNodeForMutation = function(target) {
    if (ZSSEditor.mutationObserver != undefined) {
        ZSSEditor.mutationObserver.observe(target[0], ZSSEditor.defaultMutationObserverConfig);
    } else {
        // The WebView doesn't support MutationObservers - fall back to DOMNodeRemoved events
        target.bind("DOMNodeRemoved", function(event) { ZSSEditor.onDomNodeRemoved(event); });
    }
};

/**
 *  @brief      Called when the MutationObserver registers a mutation to a node it's listening to
 */
ZSSEditor.onMutationObserved = function(mutations) {
    mutations.forEach(function(mutation) {
        for (var i = 0; i < mutation.removedNodes.length; i++) {
            var removedNode = mutation.removedNodes[i];
            if (!removedNode.attributes) {
                // Fix for https://github.com/wordpress-mobile/WordPress-Editor-Android/issues/394
                // If removedNode doesn't have an attributes property, it's not of type Node and we shouldn't proceed
                continue;
            }
            if (ZSSEditor.isMediaContainerNode(removedNode)) {
                // An uploading or failed container node was deleted manually - notify native
                var mediaIdentifier = ZSSEditor.extractMediaIdentifier(removedNode);
                ZSSEditor.sendMediaRemovedCallback(mediaIdentifier);
            } else if (removedNode.attributes.getNamedItem("data-wpid")) {
                // An uploading or failed image was deleted manually - remove its container and send the callback
                var mediaIdentifier = removedNode.attributes.getNamedItem("data-wpid").value;
                var parentRange = ZSSEditor.getParentRangeOfFocusedNode();
                ZSSEditor.removeImage(mediaIdentifier);
                if (parentRange != null) {
                    ZSSEditor.setRange(parentRange);
                }
                ZSSEditor.sendMediaRemovedCallback(mediaIdentifier);
            } else if (removedNode.attributes.getNamedItem("data-video_wpid")) {
                // An uploading or failed video was deleted manually - remove its container and send the callback
                var mediaIdentifier = removedNode.attributes.getNamedItem("data-video_wpid").value;
                var parentRange = ZSSEditor.getParentRangeOfFocusedNode();
                ZSSEditor.removeVideo(mediaIdentifier);
                if (parentRange != null) {
                    ZSSEditor.setRange(parentRange);
                }
                ZSSEditor.sendMediaRemovedCallback(mediaIdentifier);
            } else if (mutation.target.className == "edit-container") {
                // A media item wrapped in an edit container was deleted manually - remove its container
                // No callback in this case since it's not an uploading or failed media item
                var parentRange = ZSSEditor.getParentRangeOfFocusedNode();

                mutation.target.remove();

                if (parentRange != null) {
                    ZSSEditor.setRange(parentRange);
                }

                ZSSEditor.getFocusedField().emptyFieldIfNoContents();
            }
        }
    });
};

/**
 *  @brief      Called when a DOMNodeRemoved event is triggered for an element we're tracking
 *              (only used when MutationObserver is unsupported by the WebView)
 */
ZSSEditor.onDomNodeRemoved = function(event) {
    if (event.target.id.length > 0) {
        var mediaId = ZSSEditor.extractMediaIdentifier(event.target);
    } else if (event.target.parentNode.id.length > 0) {
        var mediaId = ZSSEditor.extractMediaIdentifier(event.target.parentNode);
    } else {
        return;
    }
    ZSSEditor.sendMediaRemovedCallback(mediaId);
};

// MARK: - Logging

ZSSEditor.log = function(msg) {
	ZSSEditor.callback('callback-log', 'msg=' + msg);
};

// MARK: - Callbacks

ZSSEditor.domLoadedCallback = function() {

	ZSSEditor.callback("callback-dom-loaded");
};

ZSSEditor.selectionChangedCallback = function () {

    var joinedArguments = ZSSEditor.getJoinedFocusedFieldIdAndCaretArguments();

    ZSSEditor.callback('callback-selection-changed', joinedArguments);
    this.callback("callback-input", joinedArguments);
};

ZSSEditor.callback = function(callbackScheme, callbackPath) {

	var url =  callbackScheme + ":";

	if (callbackPath) {
		url = url + callbackPath;
	}

	if (isUsingiOS) {
        ZSSEditor.callbackThroughIFrame(url);
    } else if (isUsingAndroid) {
        if (nativeState.androidApiLevel < 17) {
            ZSSEditor.callbackThroughIFrame(url);
        } else {
            nativeCallbackHandler.executeCallback(callbackScheme, callbackPath);
        }
	} else {
		console.log(url);
	}
};

/**
 *  @brief      Executes a callback by loading it into an IFrame.
 *  @details    The reason why we're using this instead of window.location is that window.location
 *              can sometimes fail silently when called multiple times in rapid succession.
 *              Found here:
 *              http://stackoverflow.com/questions/10010342/clicking-on-a-link-inside-a-webview-that-will-trigger-a-native-ios-screen-with/10080969#10080969
 *
 *  @param      url     The callback URL.
 */
ZSSEditor.callbackThroughIFrame = function(url) {
    var iframe = document.createElement("IFRAME");
    iframe.setAttribute('sandbox', '');
    iframe.setAttribute("src", url);

    // IMPORTANT: the IFrame was showing up as a black box below our text.  By setting its borders
    // to be 0px transparent we make sure it's not shown at all.
    //
    // REF BUG: https://github.com/wordpress-mobile/WordPress-iOS-Editor/issues/318
    //
    iframe.style.cssText = "border: 0px transparent;";

    document.documentElement.appendChild(iframe);
    iframe.parentNode.removeChild(iframe);
    iframe = null;
};

ZSSEditor.stylesCallback = function(stylesArray) {

	var stylesString = '';

	if (stylesArray.length > 0) {
		stylesString = stylesArray.join(defaultCallbackSeparator);
	}

	ZSSEditor.callback("callback-selection-style", stylesString);
};

// MARK: - Selection

ZSSEditor.backupRange = function(){
	var selection = window.getSelection();
    if (selection.rangeCount < 1) {
        return;
    }
    var range = selection.getRangeAt(0);

    ZSSEditor.currentSelection =
    {
        "startContainer": range.startContainer,
        "startOffset": range.startOffset,
        "endContainer": range.endContainer,
        "endOffset": range.endOffset
    };
};

ZSSEditor.restoreRange = function(){
    if (this.currentSelection) {
        var selection = window.getSelection();
        selection.removeAllRanges();

        var range = document.createRange();
        range.setStart(this.currentSelection.startContainer, this.currentSelection.startOffset);
        range.setEnd(this.currentSelection.endContainer, this.currentSelection.endOffset);
        selection.addRange(range);
    }
};

ZSSEditor.resetSelectionOnField = function(fieldId, offset) {
    var query = "div#" + fieldId;
    var field = document.querySelector(query);

    this.giveFocusToElement(field, offset);
};

ZSSEditor.giveFocusToElement = function(element, offset) {
    offset = typeof offset !== 'undefined' ? offset : 0;

    var range = document.createRange();
    range.setStart(element, offset);
    range.setEnd(element, offset);

    var selection = document.getSelection();
    selection.removeAllRanges();
    selection.addRange(range);
};

ZSSEditor.setFocusAfterElement = function(element) {
    var selection = window.getSelection();

    if (selection.rangeCount) {
        var range = document.createRange();

        range.setStartAfter(element);
        range.setEndAfter(element);
        selection.removeAllRanges();
        selection.addRange(range);
    }
};

ZSSEditor.getSelectedText = function() {
	var selection = window.getSelection();
	return selection.toString();
};

ZSSEditor.canExpandBackward = function(range) {
    // Can't expand if focus is not a text node
    if (!range.endContainer.nodeType == 3) {
        return false;
    }
    var caretRange = range.cloneRange();
    if (range.startOffset == 0) {
    return false;
    }
    caretRange.setStart(range.startContainer, range.startOffset - 1);
    caretRange.setEnd(range.startContainer, range.startOffset);
    if (!caretRange.toString().match(/\w/)) {
    return false;
    }
    return true;
};

ZSSEditor.canExpandForward = function(range) {
    // Can't expand if focus is not a text node
    if (!range.endContainer.nodeType == 3) {
        return false;
    }
    var caretRange = range.cloneRange();
    if (range.endOffset == range.endContainer.length)  {
    return false;
    }
    caretRange.setStart(range.endContainer, range.endOffset);
    if (range.endOffset )
    caretRange.setEnd(range.endContainer, range.endOffset + 1);
    var strin = caretRange.toString();
    if (!caretRange.toString().match(/\w/)) {
    return false;
    }
    return true;
};

ZSSEditor.getSelectedTextToLinkify = function() {
  var selection = window.getSelection();
  var element = ZSSEditor.getField("zss_field_content");
  // If there is no text selected, try to expand it to the word under the cursor
  if (selection.rangeCount == 1) {
    var range = selection.getRangeAt(0);
    while (ZSSEditor.canExpandBackward(range)) {
      range.setStart(range.startContainer, range.startOffset - 1);
    }
    while (ZSSEditor.canExpandForward(range)) {
      range.setEnd(range.endContainer, range.endOffset + 1);
    }
    selection.removeAllRanges();
    selection.addRange(range);
  }
  return selection.toString();
};

ZSSEditor.getCaretArguments = function() {
    var caretInfo = this.getYCaretInfo();

    if (caretInfo == null) {
        return null;
    } else {
        this.caretArguments[0] = 'yOffset=' + caretInfo.y;
        this.caretArguments[1] = 'height=' + caretInfo.height;
        return this.caretArguments;
    }
};

ZSSEditor.getJoinedFocusedFieldIdAndCaretArguments = function() {
    var joinedArguments = ZSSEditor.getJoinedCaretArguments();
    var idArgument = "id=" + ZSSEditor.getFocusedField().getNodeId();

    joinedArguments = idArgument + defaultCallbackSeparator + joinedArguments;

    return joinedArguments;
};

ZSSEditor.getJoinedCaretArguments = function() {

    var caretArguments = this.getCaretArguments();
    var joinedArguments = this.caretArguments.join(defaultCallbackSeparator);

    return joinedArguments;
};

ZSSEditor.getCaretYPosition = function() {
    var selection = window.getSelection();
    if (selection.rangeCount == 0)  {
        return 0;
    }
    var range = selection.getRangeAt(0);
    var span = document.createElement("span");
    // Ensure span has dimensions and position by
    // adding a zero-width space character
    span.appendChild( document.createTextNode("\u200b") );
    range.insertNode(span);
    var y = span.offsetTop;
    var spanParent = span.parentNode;
    spanParent.removeChild(span);

    // Glue any broken text nodes back together
    spanParent.normalize();

    return y;
}

ZSSEditor.getYCaretInfo = function() {
    var selection = window.getSelection();
    var noSelectionAvailable = selection.rangeCount == 0;

    if (noSelectionAvailable) {
        return null;
    }

    var y = 0;
    var height = 0;
    var range = selection.getRangeAt(0);
    var needsToWorkAroundNewlineBug = (range.getClientRects().length == 0);

    // PROBLEM: iOS seems to have problems getting the offset for some empty nodes and return
    // 0 (zero) as the selection range top offset.
    //
    // WORKAROUND: To fix this problem we use a different method to obtain the Y position instead.
    //
    if (needsToWorkAroundNewlineBug) {
        var closerParentNode = ZSSEditor.closerParentNode();
        var closerDiv = ZSSEditor.findParentContenteditableDiv();

        var fontSize = $(closerParentNode).css('font-size');
        var lineHeight = Math.floor(parseInt(fontSize.replace('px','')) * 1.5);

        y = this.getCaretYPosition();
        height = lineHeight;
    } else {
        if (range.getClientRects) {
            var rects = range.getClientRects();
            if (rects.length > 0) {
                // PROBLEM: some iOS versions differ in what is returned by getClientRects()
                // Some versions return the offset from the page's top, some other return the
                // offset from the visible viewport's top.
                //
                // WORKAROUND: see if the offset of the body's top is ever negative.  If it is
                // then it means that the offset we have is relative to the body's top, and we
                // should add the scroll offset.
                //
                var addsScrollOffset = document.body.getClientRects()[0].top < 0;

                if (addsScrollOffset) {
                    y = document.body.scrollTop;
                }

                y += rects[0].top;
                height = rects[0].height;
            }
        }
    }

    this.caretInfo.y = y;
    this.caretInfo.height = height;

    return this.caretInfo;
};

// MARK: - Styles

ZSSEditor.setBold = function() {
	document.execCommand('bold', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setItalic = function() {
	document.execCommand('italic', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setSubscript = function() {
	document.execCommand('subscript', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setSuperscript = function() {
	document.execCommand('superscript', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setStrikeThrough = function() {
	var commandName = 'strikeThrough';
	var isDisablingStrikeThrough = ZSSEditor.isCommandEnabled(commandName);

	document.execCommand(commandName, false, null);

	// DRM: WebKit has a problem disabling strikeThrough when the tag <del> is used instead of
	// <strike>.  The code below serves as a way to fix this issue.
	//
	var mustHandleWebKitIssue = (isDisablingStrikeThrough
								 && ZSSEditor.isCommandEnabled(commandName));

	if (mustHandleWebKitIssue && window.getSelection().rangeCount > 0) {
		var troublesomeNodeNames = ['del'];

		var selection = window.getSelection();
		var range = selection.getRangeAt(0).cloneRange();

		var container = range.commonAncestorContainer;
		var nodeFound = false;
		var textNode = null;

		while (container && !nodeFound) {
			nodeFound = (container
						 && container.nodeType == document.ELEMENT_NODE
						 && troublesomeNodeNames.indexOf(container.nodeName.toLowerCase()) > -1);

			if (!nodeFound) {
				container = container.parentElement;
			}
		}

		if (container) {
			var newObject = $(container).replaceWith(container.innerHTML);

			var finalSelection = window.getSelection();
			var finalRange = selection.getRangeAt(0).cloneRange();

			finalRange.setEnd(finalRange.startContainer, finalRange.startOffset + 1);

			selection.removeAllRanges();
			selection.addRange(finalRange);
		}
	}

	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setUnderline = function() {
	document.execCommand('underline', false, null);
	ZSSEditor.sendEnabledStyles();
};

/**
 *  @brief      Turns blockquote ON or OFF for the current selection.
 *  @details    This method makes sure that the contents of the blockquotes are surrounded by the
 *              defaultParagraphSeparator tag (by default '<p>').  This ensures parity with the web
 *              editor.
 */
ZSSEditor.setBlockquote = function() {

    var savedSelection = rangy.saveSelection();
    var selection = document.getSelection();
    var range = selection.getRangeAt(0).cloneRange();
    var sendStyles = false;

    // Make sure text being wrapped in blockquotes is inside paragraph tags
    // (should be <blockquote><paragraph>contents</paragraph></blockquote>)
    var currentHtml = ZSSEditor.focusedField.getWrappedDomNode().innerHTML;
    if (currentHtml.search('<' + ZSSEditor.defaultParagraphSeparator) == -1) {
        ZSSEditor.focusedField.setHTML(Util.wrapHTMLInTag(currentHtml, ZSSEditor.defaultParagraphSeparator));
    }

    var ancestorElement = this.getAncestorElementForSettingBlockquote(range);

    if (ancestorElement) {
        sendStyles = true;

        var childNodes = this.getChildNodesIntersectingRange(ancestorElement, range);

        // On older APIs, the rangy selection node is targeted when turning off empty blockquotes at the start of a post
        // In that case, add the empty DIV element next to the rangy selection to the childNodes array to correctly
        // turn the blockquote off
        // https://github.com/wordpress-mobile/WordPress-Editor-Android/issues/401
        var nextChildNode = childNodes[childNodes.length-1].nextSibling;
        if (nextChildNode && nextChildNode.nodeName == NodeName.DIV && nextChildNode.innerHTML == "") {
            childNodes.push(nextChildNode);
        }

        if (childNodes && childNodes.length) {
            this.toggleBlockquoteForSpecificChildNodes(ancestorElement, childNodes);
        }
    }

    rangy.restoreSelection(savedSelection);

    // When turning off an empty blockquote in an empty post, ensure there aren't any leftover empty paragraph tags
    // https://github.com/wordpress-mobile/WordPress-Editor-Android/issues/401
    var currentContenteditableDiv = ZSSEditor.focusedField.getWrappedDomNode();
    if (currentContenteditableDiv.children.length == 1 && currentContenteditableDiv.firstChild.innerHTML == "") {
        ZSSEditor.focusedField.emptyFieldIfNoContents();
    }

    if (sendStyles) {
        ZSSEditor.sendEnabledStyles();
    }
};

ZSSEditor.removeFormating = function() {
	document.execCommand('removeFormat', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setHorizontalRule = function() {
	document.execCommand('insertHorizontalRule', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setHeading = function(heading) {
	var formatTag = heading;
	var formatBlock = document.queryCommandValue('formatBlock');

	if (formatBlock.length > 0 && formatBlock.toLowerCase() == formatTag) {
		document.execCommand('formatBlock', false, Util.buildOpeningTag(this.defaultParagraphSeparator));
	} else {
		document.execCommand('formatBlock', false, Util.buildOpeningTag(formatTag));
	}

	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setParagraph = function() {
	var formatTag = "div";
	var formatBlock = document.queryCommandValue('formatBlock');

	if (formatBlock.length > 0 && formatBlock.toLowerCase() == formatTag) {
		document.execCommand('formatBlock', false, Util.buildOpeningTag(this.defaultParagraphSeparator));
	} else {
		document.execCommand('formatBlock', false, Util.buildOpeningTag(formatTag));
	}

	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.undo = function() {
	document.execCommand('undo', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.redo = function() {
	document.execCommand('redo', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setOrderedList = function() {
    document.execCommand('insertOrderedList', false, null);

    // If the insertOrderedList is no longer enabled after running execCommand,
    // we can assume the user is turning it off.
    if (!ZSSEditor.isCommandEnabled('insertOrderedList')) {
        ZSSEditor.completeListEditing();
    }
    ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setUnorderedList = function() {
	document.execCommand('insertUnorderedList', false, null);

    // If the insertUnorderedList is no longer enabled after running execCommand,
    // we can assume the user is turning it off.
    if (!ZSSEditor.isCommandEnabled('insertUnorderedList')) {
        ZSSEditor.completeListEditing();
    }
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setJustifyCenter = function() {
	document.execCommand('justifyCenter', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setJustifyFull = function() {
	document.execCommand('justifyFull', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setJustifyLeft = function() {
	document.execCommand('justifyLeft', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setJustifyRight = function() {
	document.execCommand('justifyRight', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setIndent = function() {
	document.execCommand('indent', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setOutdent = function() {
	document.execCommand('outdent', false, null);
	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.setTextColor = function(color) {
    ZSSEditor.restoreRange();
	document.execCommand("styleWithCSS", null, true);
	document.execCommand('foreColor', false, color);
	document.execCommand("styleWithCSS", null, false);
	ZSSEditor.sendEnabledStyles();
    // document.execCommand("removeFormat", false, "foreColor"); // Removes just foreColor
};

ZSSEditor.setBackgroundColor = function(color) {
	ZSSEditor.restoreRange();
	document.execCommand("styleWithCSS", null, true);
	document.execCommand('hiliteColor', false, color);
	document.execCommand("styleWithCSS", null, false);
	ZSSEditor.sendEnabledStyles();
};

/**
 *  @brief      Wraps given HTML in paragraph tags, appends a new line, and inserts it into the field
 *  @details    This method makes sure that passed HTML is wrapped in a separate paragraph.
 *              It also appends a new opening paragraph tag and a space. This step is necessary to keep any spans or
 *              divs in the HTML from being read by the WebView as a style and applied to all future paragraphs.
 */
ZSSEditor.insertHTMLWrappedInParagraphTags = function(html) {
    var space = '<br>';
    var paragraphOpenTag = Util.buildOpeningTag(this.defaultParagraphSeparator);
    var paragraphCloseTag = Util.buildClosingTag(this.defaultParagraphSeparator);

    if (this.getFocusedField().getHTML().length == 0) {
        html = paragraphOpenTag + html;
    }

    // Without this line, API<19 WebView will reset the caret to the start of the document, inserting the new line
    // there instead of under the newly added media item
    if (nativeState.androidApiLevel < 19) {
        html = html + '&#x200b;';
    }

    // Due to the way the WebView handles divs, we need to add a new paragraph in a separate insertion - otherwise,
    // the new paragraph will be nested within the existing paragraph.
    this.insertHTML(html);

    this.insertHTML(paragraphOpenTag + space + paragraphCloseTag);
};

ZSSEditor.insertLink = function(url, title) {
    var html = '<a href="' + url + '">' + title + "</a>";

    var parentBlockQuoteNode = ZSSEditor.closerParentNodeWithName('blockquote');

    var currentRange = document.getSelection().getRangeAt(0);
    var currentNode = currentRange.startContainer;
    var currentNodeIsEmpty = (currentNode.innerHTML == '' || currentNode.innerHTML == '<br>');

    var selectionIsAtStartOrEnd = Util.rangeIsAtStartOfParent(currentRange) || Util.rangeIsAtEndOfParent(currentRange);

    if (this.getFocusedField().getHTML().length == 0
        || (parentBlockQuoteNode && !currentNodeIsEmpty && selectionIsAtStartOrEnd)) {
        // Wrap the link tag in paragraph tags when the post is empty, and also when inside a blockquote
        // The latter is to fix a bug with document.execCommand('insertHTML') inside a blockquote, where the div inside
        // the blockquote is ignored and the link tag is inserted outside it, on a new line with no wrapping div
        // Wrapping the link in paragraph tags makes insertHTML join it to the existing div, for some reason
        // We exclude being on an empty line inside a blockquote and when the selection isn't at the beginning or end
        // of the line, as the fix is unnecessary in both those cases and causes paragraph formatting issues
        html = Util.buildOpeningTag(this.defaultParagraphSeparator) + html;
    }

    this.insertHTML(html);
};

ZSSEditor.updateLink = function(url, title) {

    ZSSEditor.restoreRange();

    var currentLinkNode = ZSSEditor.lastTappedNode;

    if (currentLinkNode) {
		currentLinkNode.setAttribute("href", url);
		currentLinkNode.innerHTML = title;
    }
    ZSSEditor.sendEnabledStyles();
};

ZSSEditor.unlink = function() {
	var savedSelection = rangy.saveSelection();

	var currentLinkNode = ZSSEditor.closerParentNodeWithName('a');

	if (currentLinkNode) {
		ZSSEditor.unwrapNode(currentLinkNode);
	}

    rangy.restoreSelection(savedSelection);

	ZSSEditor.sendEnabledStyles();
};

ZSSEditor.unwrapNode = function(node) {
    $(node).contents().unwrap();
};

ZSSEditor.quickLink = function() {

	var sel = document.getSelection();
	var link_url = "";
	var test = new String(sel);
	var mailregexp = new RegExp("^(.+)(\@)(.+)$", "gi");
	if (test.search(mailregexp) == -1) {
		checkhttplink = new RegExp("^http\:\/\/", "gi");
		if (test.search(checkhttplink) == -1) {
			checkanchorlink = new RegExp("^\#", "gi");
			if (test.search(checkanchorlink) == -1) {
				link_url = "http://" + sel;
			} else {
				link_url = sel;
			}
		} else {
			link_url = sel;
		}
	} else {
		checkmaillink = new RegExp("^mailto\:", "gi");
		if (test.search(checkmaillink) == -1) {
			link_url = "mailto:" + sel;
		} else {
			link_url = sel;
		}
	}

	var html_code = '<a href="' + link_url + '">' + sel + '</a>';
	ZSSEditor.insertHTML(html_code);
};

// MARK: - Blockquotes

/**
 *  @brief      This method toggles blockquote for the specified child nodes.  This is useful since
 *              we can toggle blockquote either for some or ALL of the child nodes, depending on
 *              what we need to achieve.
 *  @details    CASE 1: If the parent node is a blockquote node, the child nodes will be extracted
 *              from it leaving the remaining siblings untouched (by splitting the parent blockquote
 *              node in two if necessary).
 *              CASE 2: If the parent node is NOT a blockquote node, but the first child is, the
 *              method will make sure all child nodes that are blockquote nodes will be toggled to
 *              non-blockquote nodes.
 *              CASE 3: If both the parent node and the first node are non-blockquote nodes, this
 *              method will turn all child nodes into blockquote nodes.
 *
 *  @param      parentNode      The parent node.  Can be either a blockquote or non-blockquote node.
 *                              Cannot be null.
 *  @param      nodes           The child nodes.  Can be any combination of blockquote and
 *                              non-blockquote nodes.  Cannot be null.
 */
ZSSEditor.toggleBlockquoteForSpecificChildNodes = function(parentNode, nodes) {

    if (nodes && nodes.length > 0) {
        if (parentNode.nodeName == NodeName.BLOCKQUOTE) {
            for (var counter = 0; counter < nodes.length; counter++) {
                this.turnBlockquoteOffForNode(nodes[counter]);
            }
        } else {

            var turnOn = (nodes[0].nodeName != NodeName.BLOCKQUOTE);

            for (var counter = 0; counter < nodes.length; counter++) {
                if (turnOn) {
                    this.turnBlockquoteOnForNode(nodes[counter]);
                } else {
                    this.turnBlockquoteOffForNode(nodes[counter]);
                }
            }
        }
    }
};


/**
 *  @brief      Turns blockquote off for the specified node.
 *
 *  @param      node    The node to turn the blockquote off for.  It can either be a blockquote
 *                      node (in which case it will be removed and all child nodes extracted) or
 *                      have a parent blockquote node (in which case the node will be extracted
 *                      from its parent).
 */
ZSSEditor.turnBlockquoteOffForNode = function(node) {

    if (node.nodeName == NodeName.BLOCKQUOTE) {
        for (var i = 0; i < node.childNodes.length; i++) {
            this.extractNodeFromAncestorNode(node.childNodes[i], node);
        }
    } else {
        if (node.parentNode.nodeName == NodeName.BLOCKQUOTE) {
            this.extractNodeFromAncestorNode(node, node.parentNode);
        }
    }
};

/**
 *  @brief      Turns blockquote on for the specified node.
 *
 *  @param      node    The node to turn blockquote on for.  Will attempt to attach the newly
 *                      created blockquote to sibling or uncle blockquote nodes.  If the node is
 *                      null or it's parent is null, this method will exit without affecting it
 *                      (this can actually be caused by this method modifying the surrounding
 *                      nodes, if those nodes are stored in an array - and thus are not notified
 *                      of DOM hierarchy changes).
 */
ZSSEditor.turnBlockquoteOnForNode = function(node) {

    if (!node || !node.parentNode) {
        return;
    }

    var couldJoinBlockquotes = this.joinAdjacentSiblingsOrAncestorBlockquotes(node);

    if (!couldJoinBlockquotes) {
        var blockquote = document.createElement(NodeName.BLOCKQUOTE);

        node.parentNode.insertBefore(blockquote, node);
        blockquote.appendChild(node);
    }
};

// MARK: - Generic media

ZSSEditor.isMediaContainerNode = function(node) {
    if (node.id === undefined) {
        return false;
    }
    return (node.id.search("img_container_") == 0) || (node.id.search("video_container_") == 0);
};

ZSSEditor.extractMediaIdentifier = function(node) {
    if (node.id.search("img_container_") == 0) {
        return node.id.replace("img_container_", "");
    } else if (node.id.search("video_container_") == 0) {
        return node.id.replace("video_container_", "");
    }
    return "";
};

ZSSEditor.getMediaNodeWithIdentifier = function(mediaNodeIdentifier) {
    var imageNode = ZSSEditor.getImageNodeWithIdentifier(mediaNodeIdentifier);
    if (imageNode.length > 0) {
        return imageNode;
    } else {
        return ZSSEditor.getVideoNodeWithIdentifier(mediaNodeIdentifier);
    }
};

ZSSEditor.getMediaProgressNodeWithIdentifier = function(mediaNodeIdentifier) {
    var imageProgressNode = ZSSEditor.getImageProgressNodeWithIdentifier(mediaNodeIdentifier);
    if (imageProgressNode.length > 0) {
        return imageProgressNode;
    } else {
        return ZSSEditor.getVideoProgressNodeWithIdentifier(mediaNodeIdentifier);
    }
};

ZSSEditor.getMediaContainerNodeWithIdentifier = function(mediaNodeIdentifier) {
    var imageContainerNode = ZSSEditor.getImageContainerNodeWithIdentifier(mediaNodeIdentifier);
    if (imageContainerNode.length > 0) {
        return imageContainerNode;
    } else {
        return ZSSEditor.getVideoContainerNodeWithIdentifier(mediaNodeIdentifier);
    }
};

/**
 *  @brief      Update the progress indicator for the media item identified with the value in progress.
 *
 *  @param      mediaNodeIdentifier This is a unique ID provided by the caller.
 *  @param      progress    A value between 0 and 1 indicating the progress on the media upload.
 */
ZSSEditor.setProgressOnMedia = function(mediaNodeIdentifier, progress) {
    var mediaNode = this.getMediaNodeWithIdentifier(mediaNodeIdentifier);
    var mediaProgressNode = this.getMediaProgressNodeWithIdentifier(mediaNodeIdentifier);

    if (progress == 0) {
        mediaNode.addClass("uploading");
    }

    // Don't allow the progress bar to move backward
    if (mediaNode.length == 0 || mediaProgressNode.length == 0 || mediaProgressNode.attr("value") > progress) {
        return;
    }

    // Revert to non-compatibility image container once image upload has begun. This centers the overlays on the image
    // (instead of the screen), while still circumventing the small container bug the compat class was added to fix
    if (progress > 0) {
        this.getMediaContainerNodeWithIdentifier(mediaNodeIdentifier).removeClass("compat");
    }

    // Sometimes the progress bar can be stuck at 100% for a long time while further processing happens
    // From a UX perspective, it's better to just keep the progress bars at 90% until the upload is really complete
    // and the progress bar is removed entirely
    if (progress > 0.9) {
        return;
    }

    mediaProgressNode.attr("value", progress);
};

ZSSEditor.setupOptimisticProgressUpdate = function(mediaNodeIdentifier, nCall) {
    setTimeout(ZSSEditor.sendOptimisticProgressUpdate, nCall * 100, mediaNodeIdentifier, nCall);
};

ZSSEditor.sendOptimisticProgressUpdate = function(mediaNodeIdentifier, nCall) {
    if (nCall > 15) {
        return;
    }

    var mediaNode = ZSSEditor.getMediaNodeWithIdentifier(mediaNodeIdentifier);

    // Don't send progress updates to failed media
    if (mediaNode.length != 0 && mediaNode[0].classList.contains("failed")) {
        return;
    }

    ZSSEditor.setProgressOnMedia(mediaNodeIdentifier, nCall / 100);
    ZSSEditor.setupOptimisticProgressUpdate(mediaNodeIdentifier, nCall + 1);
};

ZSSEditor.removeAllFailedMediaUploads = function() {
    console.log("Remove all failed media");
    var failedMediaArray = ZSSEditor.getFailedMediaIdArray();
    for (var i = 0; i < failedMediaArray.length; i++) {
        ZSSEditor.removeMedia(failedMediaArray[i]);
    }
};

ZSSEditor.removeMedia = function(mediaNodeIdentifier) {
    if (this.getImageNodeWithIdentifier(mediaNodeIdentifier).length != 0) {
        this.removeImage(mediaNodeIdentifier);
    } else if (this.getVideoNodeWithIdentifier(mediaNodeIdentifier).length != 0) {
        this.removeVideo(mediaNodeIdentifier);
    }
};

ZSSEditor.sendMediaRemovedCallback = function(mediaNodeIdentifier) {
    var arguments = ['id=' + encodeURIComponent(mediaNodeIdentifier)];
    var joinedArguments = arguments.join(defaultCallbackSeparator);
    this.callback("callback-media-removed", joinedArguments);
};

/**
 *  @brief      Marks all in-progress images as failed to upload
 */
ZSSEditor.markAllUploadingMediaAsFailed = function(message) {
    var html = ZSSEditor.getField("zss_field_content").getHTML();
    var tmp = document.createElement( "div" );
    var tmpDom = $( tmp ).html( html );
    var matches = tmpDom.find("img.uploading");

    for(var i = 0; i < matches.size(); i++) {
        if (matches[i].hasAttribute('data-wpid')) {
            var mediaId = matches[i].getAttribute('data-wpid');
            ZSSEditor.markImageUploadFailed(mediaId, message);
        } else if (matches[i].hasAttribute('data-video_wpid')) {
            var videoId = matches[i].getAttribute('data-video_wpid');
            ZSSEditor.markVideoUploadFailed(videoId, message);
        }
    }
};

ZSSEditor.getFailedMediaIdArray = function() {
    var html = ZSSEditor.getField("zss_field_content").getHTML();
    var tmp = document.createElement( "div" );
    var tmpDom = $( tmp ).html( html );
    var matches = tmpDom.find("img.failed");

    var mediaIdArray = [];

    for (var i = 0; i < matches.size(); i++) {
        var mediaId = null;
        if (matches[i].hasAttribute("data-wpid")) {
            mediaId = matches[i].getAttribute("data-wpid");
        } else if (matches[i].hasAttribute("data-video_wpid")) {
            mediaId = matches[i].getAttribute("data-video_wpid");
        }
        if (mediaId !== null) {
            mediaIdArray.push(mediaId);
        }
    }
    return mediaIdArray;
};

/**
 *  @brief      Sends a callback with a list of failed images
 */
ZSSEditor.getFailedMedia = function() {
    var mediaIdArray = ZSSEditor.getFailedMediaIdArray();
    for (var i = 0; i < mediaIdArray.length; i++) {
        // Track pre-existing failed media nodes for manual deletion events
        ZSSEditor.trackNodeForMutation(this.getMediaContainerNodeWithIdentifier(mediaIdArray[i]));
    }

    var functionArgument = "function=getFailedMedia";
    var joinedArguments = functionArgument + defaultCallbackSeparator + "ids=" + mediaIdArray.toString();
    ZSSEditor.callback('callback-response-string', joinedArguments);
};

// MARK: - Images

ZSSEditor.updateImage = function(url, alt) {

    ZSSEditor.restoreRange();

    if (ZSSEditor.currentEditingImage) {
        var c = ZSSEditor.currentEditingImage;
        c.attr('src', url);
        c.attr('alt', alt);
    }
    ZSSEditor.sendEnabledStyles();

};

ZSSEditor.insertImage = function(url, remoteId, alt) {
    var html = '<img src="' + url + '" class="wp-image-' + remoteId + ' alignnone size-full';
    if (alt) {
        html += '" alt="' + alt;
    }
    html += '"/>';

    this.insertHTMLWrappedInParagraphTags(html);

    this.sendEnabledStyles();
    this.callback("callback-action-finished");
};

/**
 *  @brief      Inserts a local image URL.  Useful for images that need to be uploaded.
 *  @details    By inserting a local image URL, we can make sure the image is shown to the user
 *              as soon as it's selected for uploading.  Once the image is successfully uploaded
 *              the application should call replaceLocalImageWithRemoteImage().
 *
 *  @param      imageNodeIdentifier     This is a unique ID provided by the caller.  It exists as
 *                                      a mechanism to update the image node with the remote URL
 *                                      when replaceLocalImageWithRemoteImage() is called.
 *  @param      localImageUrl           The URL of the local image to display.  Please keep in mind
 *                                      that a remote URL can be used here too, since this method
 *                                      does not check for that.  It would be a mistake.
 */
ZSSEditor.insertLocalImage = function(imageNodeIdentifier, localImageUrl) {
    var progressIdentifier = this.getImageProgressIdentifier(imageNodeIdentifier);
    var imageContainerIdentifier = this.getImageContainerIdentifier(imageNodeIdentifier);

    if (nativeState.androidApiLevel > 18) {
        var imgContainerClass = 'img_container';
        var progressElement = '<progress id="' + progressIdentifier + '" value=0 class="wp_media_indicator" contenteditable="false"></progress>';
    } else {
        // Before API 19, the WebView didn't support progress tags. Use an upload overlay instead of a progress bar
        var imgContainerClass = 'img_container compat';
        var progressElement = '<span class="upload-overlay" contenteditable="false">' + nativeState.localizedStringUploading
                              + '</span><span class="upload-overlay-bg"></span>';
    }

    var imgContainerStart = '<span id="' + imageContainerIdentifier + '" class="' + imgContainerClass
                            + '" contenteditable="false">';
    var imgContainerEnd = '</span>';
    var image = '<img data-wpid="' + imageNodeIdentifier + '" src="' + localImageUrl + '" alt="" />';
    var html = imgContainerStart + progressElement + image + imgContainerEnd;

    this.insertHTMLWrappedInParagraphTags(html);

    ZSSEditor.trackNodeForMutation(this.getImageContainerNodeWithIdentifier(imageNodeIdentifier));

    this.setProgressOnMedia(imageNodeIdentifier, 0);

    if (nativeState.androidApiLevel > 18) {
        setTimeout(ZSSEditor.setupOptimisticProgressUpdate, 300, imageNodeIdentifier, 1);
    }

    this.sendEnabledStyles();
};

ZSSEditor.getImageNodeWithIdentifier = function(imageNodeIdentifier) {
    return $('img[data-wpid="' + imageNodeIdentifier+'"]');
};

ZSSEditor.getImageProgressIdentifier = function(imageNodeIdentifier) {
    return 'progress_' + imageNodeIdentifier;
};

ZSSEditor.getImageProgressNodeWithIdentifier = function(imageNodeIdentifier) {
    return $('#'+this.getImageProgressIdentifier(imageNodeIdentifier));
};

ZSSEditor.getImageContainerIdentifier = function(imageNodeIdentifier) {
    return 'img_container_' + imageNodeIdentifier;
};

ZSSEditor.getImageContainerNodeWithIdentifier = function(imageNodeIdentifier) {
    return $('#'+this.getImageContainerIdentifier(imageNodeIdentifier));
};

/**
 *  @brief      Replaces a local image URL with a remote image URL.  Useful for images that have
 *              just finished uploading.
 *  @details    The remote image can be available after a while, when uploading images.  This method
 *              allows for the remote URL to be loaded once the upload completes.
 *
 *  @param      imageNodeIdentifier     This is a unique ID provided by the caller.  It exists as
 *                                      a mechanism to update the image node with the remote URL
 *                                      when replaceLocalImageWithRemoteImage() is called.
 *  @param      remoteImageUrl          The URL of the remote image to display.
 */
ZSSEditor.replaceLocalImageWithRemoteImage = function(imageNodeIdentifier, remoteImageId, remoteImageUrl) {
    var imageNode = this.getImageNodeWithIdentifier(imageNodeIdentifier);

    if (imageNode.length == 0) {
        // even if the image is not present anymore we must do callback
        this.markImageUploadDone(imageNodeIdentifier);
        return;
    }

    var image = new Image;

    image.onload = function () {
        ZSSEditor.finishLocalImageSwap(image, imageNode, imageNodeIdentifier, remoteImageId)
        image.classList.add("image-loaded");
        console.log("Image Loaded!");
    }

    image.onerror = function () {
        // Add a remoteUrl attribute, remoteUrl and src must be swapped before publishing.
        image.setAttribute('remoteurl', image.src);
        // Try to reload the image on error.
        ZSSEditor.tryToReload(image, imageNode, imageNodeIdentifier, remoteImageId, 1);
    }

    image.src = remoteImageUrl;
};

ZSSEditor.finishLocalImageSwap = function(image, imageNode, imageNodeIdentifier, remoteImageId) {
    imageNode.addClass("wp-image-" + remoteImageId);
    if (image.getAttribute("remoteurl")) {
        imageNode.attr('remoteurl', image.getAttribute("remoteurl"));
    }
    imageNode.attr('src', image.src);
    // Set extra attributes and classes used by WordPress
    imageNode.attr({'width': image.width, 'height': image.height});
    imageNode.addClass("alignnone size-full");
    ZSSEditor.markImageUploadDone(imageNodeIdentifier);
    var joinedArguments = ZSSEditor.getJoinedFocusedFieldIdAndCaretArguments();
    ZSSEditor.callback("callback-input", joinedArguments);
    image.onerror = null;
}

ZSSEditor.reloadImage = function(image, imageNode, imageNodeIdentifier, remoteImageId, nCall) {
    if (image.classList.contains("image-loaded")) {
        return;
    }
    image.onerror = ZSSEditor.tryToReload(image, imageNode, imageNodeIdentifier, remoteImageId, nCall + 1);
    // Force reloading by updating image src
    image.src = image.getAttribute("remoteurl") + "?retry=" + nCall;
    console.log("Reloading image:" + nCall + " - " + image.src);
}

ZSSEditor.tryToReload = function (image, imageNode, imageNodeIdentifier, remoteImageId, nCall) {
    if (nCall > 8) { // 7 tries: 22500 ms total
        ZSSEditor.finishLocalImageSwap(image, imageNode, imageNodeIdentifier, remoteImageId);
        return;
    }
    image.onerror = null;
    console.log("Image not loaded");
    // reload the image with a variable delay: 500ms, 1000ms, 1500ms, 2000ms, etc.
    setTimeout(ZSSEditor.reloadImage, nCall * 500, image, imageNode, imageNodeIdentifier, remoteImageId, nCall);
}

/**
 *  @brief      Notifies that the image upload as finished
 *
 *  @param      imageNodeIdentifier     The unique image ID for the uploaded image
 */
ZSSEditor.markImageUploadDone = function(imageNodeIdentifier) {
    var imageNode = this.getImageNodeWithIdentifier(imageNodeIdentifier);
    if (imageNode.length == 0){
        return;
    }

    // remove identifier attributed from image
    imageNode.removeAttr('data-wpid');

    // remove uploading style
    imageNode.removeClass("uploading");

    // Remove all extra formatting nodes for progress
    if (imageNode.parent().attr("id") == this.getImageContainerIdentifier(imageNodeIdentifier)) {
        // Reset id before removal to avoid triggering the manual media removal callback
        imageNode.parent().attr("id", "");
        imageNode.parent().replaceWith(imageNode);
    }
    // Wrap link around image
    var link = $('<a>', { href: imageNode.attr("src") } );
    imageNode.wrap(link);
    // We invoke the sendImageReplacedCallback with a delay to avoid for
    // it to be ignored by the webview because of the previous callback being done.
    var thisObj = this;
    setTimeout(function() { thisObj.sendImageReplacedCallback(imageNodeIdentifier);}, 500);
};

/**
 *  @brief      Callbacks to native that the image upload as finished and the local url was replaced by the remote url
 *
 *  @param      imageNodeIdentifier     The unique image ID for the uploaded image
 */
ZSSEditor.sendImageReplacedCallback = function( imageNodeIdentifier ) {
    var arguments = ['id=' + encodeURIComponent( imageNodeIdentifier )];

    var joinedArguments = arguments.join( defaultCallbackSeparator );

    this.callback("callback-image-replaced", joinedArguments);
};

/**
 *  @brief      Marks the image as failed to upload
 *
 *  @param      imageNodeIdentifier     This is a unique ID provided by the caller.
 *  @param      message                 A message to show to the user, overlayed on the image
 */
ZSSEditor.markImageUploadFailed = function(imageNodeIdentifier, message) {
    var imageNode = this.getImageNodeWithIdentifier(imageNodeIdentifier);
    if (imageNode.length == 0){
        return;
    }

    var sizeClass = '';
    if ( imageNode[0].width > 480 && imageNode[0].height > 240 ) {
        sizeClass = "largeFail";
    } else if ( imageNode[0].width < 100 || imageNode[0].height < 100 ) {
        sizeClass = "smallFail";
    }

    imageNode.addClass('failed');

    var imageContainerNode = this.getImageContainerNodeWithIdentifier(imageNodeIdentifier);
    if(imageContainerNode.length != 0){
        imageContainerNode.attr("data-failed", message);
        imageNode.removeClass("uploading");
        imageContainerNode.addClass('failed');
        imageContainerNode.addClass(sizeClass);
    }

    var imageProgressNode = this.getImageProgressNodeWithIdentifier(imageNodeIdentifier);
    if (imageProgressNode.length != 0){
        imageProgressNode.addClass('failed');
        imageProgressNode.attr("value", 0);
    }

    // Delete the compatibility overlay if present
    imageContainerNode.find("span.upload-overlay").addClass("failed");
};

/**
 *  @brief      Unmarks the image as failed to upload
 *
 *  @param      imageNodeIdentifier     This is a unique ID provided by the caller.
 */
ZSSEditor.unmarkImageUploadFailed = function(imageNodeIdentifier) {
    var imageNode = this.getImageNodeWithIdentifier(imageNodeIdentifier);
    if (imageNode.length != 0){
        imageNode.removeClass('failed');
    }

    var imageContainerNode = this.getImageContainerNodeWithIdentifier(imageNodeIdentifier);
    if(imageContainerNode.length != 0){
        imageContainerNode.removeAttr("data-failed");
        imageContainerNode.removeClass('failed');
    }

    var imageProgressNode = this.getImageProgressNodeWithIdentifier(imageNodeIdentifier);
    if (imageProgressNode.length != 0){
        imageProgressNode.removeClass('failed');
    }

    // Display the compatibility overlay again if present
    imageContainerNode.find("span.upload-overlay").removeClass("failed");

    this.setProgressOnMedia(imageNodeIdentifier, 0);

    if (nativeState.androidApiLevel > 18) {
        setTimeout(ZSSEditor.setupOptimisticProgressUpdate, 300, imageNodeIdentifier, 1);
    }
};

/**
 *  @brief      Remove the image from the DOM.
 *
 *  @param      imageNodeIdentifier     This is a unique ID provided by the caller.
 */
ZSSEditor.removeImage = function(imageNodeIdentifier) {
    var imageNode = this.getImageNodeWithIdentifier(imageNodeIdentifier);
    if (imageNode.length != 0){
        // Reset id before removal to avoid triggering the manual media removal callback
        imageNode.attr("id","");
        imageNode.remove();
    }

    // if image is inside options container we need to remove the container
    var imageContainerNode = this.getImageContainerNodeWithIdentifier(imageNodeIdentifier);
    if (imageContainerNode.length != 0){
        imageContainerNode.remove();
    }
};

/**
 *  @brief Inserts a video tag using the videoURL as source and posterURL as the
 *  image to show while video is loading.
 *
 *  @param videoURL     the url of the video
 *  @param posterURL    the url of an image to show while the video is loading
 *  @param videoPressID the VideoPress ID of the video, when applicable
 *
 */
ZSSEditor.insertVideo = function(videoURL, posterURL, videopressID) {
    var videoId = Date.now();
    var html = '<video id=' + videoId + ' webkit-playsinline src="' + videoURL + '" onclick="" controls="controls" preload="metadata"';

    if (posterURL != '') {
        html += ' poster="' + posterURL + '"';
    }

    if (videopressID != '') {
        html += ' data-wpvideopress="' + videopressID + '"';
    }

    html += '></video>';

    this.insertHTMLWrappedInParagraphTags('&#x200b;' + html);

    // Wrap video in edit-container node for a permanent delete button overlay
    var videoNode = $('video[id=' + videoId + ']')[0];
    var selectionNode = this.applyEditContainer(videoNode);
    videoNode.removeAttribute('id');

    // Remove the zero-width space node (it's not needed now that the paragraph-wrapped video is in place)
    var zeroWidthNode = selectionNode.previousSibling;
    if (zeroWidthNode != null && zeroWidthNode.nodeType == 3) {
        zeroWidthNode.parentNode.removeChild(zeroWidthNode);
    }

    ZSSEditor.trackNodeForMutation($(selectionNode));

    this.sendEnabledStyles();
    this.callback("callback-action-finished");
};

/**
 *  @brief      Inserts a placeholder image tag for in-progress video uploads, marked with an identifier.
 *  @details    The image shown can be the video's poster if available - otherwise the default poster image is used.
 *              Using an image instead of a video placeholder is a departure from iOS, necessary because the original
 *              method caused occasional WebView freezes on Android.
 *              Once the video is successfully uploaded, the application should call replaceLocalVideoWithRemoteVideo().
 *
 *  @param      videoNodeIdentifier     This is a unique ID provided by the caller.  It exists as
 *                                      a mechanism to update the video node with the remote URL
 *                                      when replaceLocalVideoWithRemoteVideo() is called.
 *  @param      posterURL               The URL of a poster image to display while the video is being uploaded.
 */
ZSSEditor.insertLocalVideo = function(videoNodeIdentifier, posterURL) {
    var progressIdentifier = this.getVideoProgressIdentifier(videoNodeIdentifier);
    var videoContainerIdentifier = this.getVideoContainerIdentifier(videoNodeIdentifier);

    if (nativeState.androidApiLevel > 18) {
        var videoContainerClass = 'video_container';
        var progressElement = '<progress id="' + progressIdentifier + '" value=0 class="wp_media_indicator"'
                + 'contenteditable="false"></progress>';
    } else {
        // Before API 19, the WebView didn't support progress tags. Use an upload overlay instead of a progress bar
        var videoContainerClass = 'video_container compat';
        var progressElement = '<span class="upload-overlay" contenteditable="false">' + nativeState.localizedStringUploading
                + '</span><span class="upload-overlay-bg"></span>';
    }

    var videoContainerStart = '<span id="' + videoContainerIdentifier + '" class="' + videoContainerClass
            + '" contenteditable="false">';
    var videoContainerEnd = '</span>';

    if (posterURL == '') {
       posterURL = "svg/wpposter.svg";
    }

    var image = '<img data-video_wpid="' + videoNodeIdentifier + '" src="' + posterURL + '" alt="" />';
    var html = videoContainerStart + progressElement + image + videoContainerEnd;

    this.insertHTMLWrappedInParagraphTags(html);

    ZSSEditor.trackNodeForMutation(this.getVideoContainerNodeWithIdentifier(videoNodeIdentifier));

    this.setProgressOnMedia(videoNodeIdentifier, 0);

    if (nativeState.androidApiLevel > 18) {
        setTimeout(ZSSEditor.setupOptimisticProgressUpdate, 300, videoNodeIdentifier, 1);
    }

    this.sendEnabledStyles();
};

ZSSEditor.getVideoNodeWithIdentifier = function(videoNodeIdentifier) {
    var videoNode = $('img[data-video_wpid="' + videoNodeIdentifier+'"]');
    if (videoNode.length == 0) {
        videoNode = $('video[data-wpid="' + videoNodeIdentifier+'"]');
    }
    return videoNode;
};

ZSSEditor.getVideoProgressIdentifier = function(videoNodeIdentifier) {
    return 'progress_' + videoNodeIdentifier;
};

ZSSEditor.getVideoProgressNodeWithIdentifier = function(videoNodeIdentifier) {
    return $('#'+this.getVideoProgressIdentifier(videoNodeIdentifier));
};

ZSSEditor.getVideoContainerIdentifier = function(videoNodeIdentifier) {
    return 'video_container_' + videoNodeIdentifier;
};

ZSSEditor.getVideoContainerNodeWithIdentifier = function(videoNodeIdentifier) {
    return $('#'+this.getVideoContainerIdentifier(videoNodeIdentifier));
};

/**
 *  @brief      Replaces the image placeholder with a video element containing the uploaded video's attributes,
 *              and removes the upload container.
 *
 *  @param      videoNodeIdentifier     The unique id of the video upload
 *  @param      remoteVideoUrl          The URL of the remote video to display
 *  @param      remotePosterUrl         The URL of the remote poster image to display
 *  @param      videopressID            The VideoPress ID of the video, where applicable
 */
ZSSEditor.replaceLocalVideoWithRemoteVideo = function(videoNodeIdentifier, remoteVideoUrl, remotePosterUrl, videopressID) {
    var imagePlaceholderNode = this.getVideoNodeWithIdentifier(videoNodeIdentifier);

    if (imagePlaceholderNode.length != 0) {
        var videoNode = document.createElement("video");
        videoNode.setAttribute('webkit-playsinline', '');
        videoNode.setAttribute('onclick', '');
        videoNode.setAttribute('src', remoteVideoUrl);
        videoNode.setAttribute('controls', 'controls');
        videoNode.setAttribute('preload', 'metadata');
        if (videopressID != '') {
           videoNode.setAttribute('data-wpvideopress', videopressID);
        }
        videoNode.setAttribute('poster', remotePosterUrl);

        // Replace upload container and placeholder image with the uploaded video node
        var containerNode = imagePlaceholderNode.parent();
        containerNode.replaceWith(videoNode);
    }

    var selectionNode = this.applyEditContainer(videoNode);

    ZSSEditor.trackNodeForMutation($(selectionNode));

    var joinedArguments = ZSSEditor.getJoinedFocusedFieldIdAndCaretArguments();
    ZSSEditor.callback("callback-input", joinedArguments);
    // We invoke the sendVideoReplacedCallback with a delay to avoid for
    // it to be ignored by the webview because of the previous callback being done.
    var thisObj = this;
    setTimeout(function() { thisObj.sendVideoReplacedCallback(videoNodeIdentifier);}, 500);
};

/**
 *  @brief      Callbacks to native that the video upload as finished and the local url was replaced by the remote url
 *
 *  @param      videoNodeIdentifier    the unique video ID for the uploaded Video
 */
ZSSEditor.sendVideoReplacedCallback = function( videoNodeIdentifier ) {
    var arguments = ['id=' + encodeURIComponent( videoNodeIdentifier )];

    var joinedArguments = arguments.join( defaultCallbackSeparator );

    this.callback("callback-video-replaced", joinedArguments);
};

/**
 *  @brief      Callbacks to native that the video upload as finished and the local url was replaced by the remote url
 *
 *  @param      videoNodeIdentifier    the unique video ID for the uploaded Video
 */
ZSSEditor.sendVideoPressInfoRequest = function( videoPressID ) {
    var arguments = ['id=' + encodeURIComponent( videoPressID )];

    var joinedArguments = arguments.join( defaultCallbackSeparator );

    this.callback("callback-videopress-info-request", joinedArguments);
};


/**
 *  @brief      Marks the Video as failed to upload
 *
 *  @param      VideoNodeIdentifier     This is a unique ID provided by the caller.
 *  @param      message                 A message to show to the user, overlayed on the Video
 */
ZSSEditor.markVideoUploadFailed = function(videoNodeIdentifier, message) {
    var videoNode = this.getVideoNodeWithIdentifier(videoNodeIdentifier);
    if (videoNode.length == 0){
        return;
    }

    var sizeClass = '';
    if ( videoNode[0].width > 480 && videoNode[0].height > 240 ) {
        sizeClass = "largeFail";
    } else if ( videoNode[0].width < 100 || videoNode[0].height < 100 ) {
        sizeClass = "smallFail";
    }

    videoNode.addClass('failed');

    var videoContainerNode = this.getVideoContainerNodeWithIdentifier(videoNodeIdentifier);
    if(videoContainerNode.length != 0){
        videoContainerNode.attr("data-failed", message);
        videoNode.removeClass("uploading");
        videoContainerNode.addClass('failed');
        videoContainerNode.addClass(sizeClass);
    }

    var videoProgressNode = this.getVideoProgressNodeWithIdentifier(videoNodeIdentifier);
    if (videoProgressNode.length != 0){
        videoProgressNode.addClass('failed');
        videoProgressNode.attr("value", 0);
    }

    // Delete the compatibility overlay if present
    videoContainerNode.find("span.upload-overlay").addClass("failed");
};

/**
 *  @brief      Unmarks the Video as failed to upload
 *
 *  @param      VideoNodeIdentifier     This is a unique ID provided by the caller.
 */
ZSSEditor.unmarkVideoUploadFailed = function(videoNodeIdentifier) {
    var videoNode = this.getVideoNodeWithIdentifier(videoNodeIdentifier);
    if (videoNode.length != 0){
        videoNode.removeClass('failed');
    }

    var videoContainerNode = this.getVideoContainerNodeWithIdentifier(videoNodeIdentifier);
    if(videoContainerNode.length != 0){
        videoContainerNode.removeAttr("data-failed");
        videoContainerNode.removeClass('failed');
    }

    var videoProgressNode = this.getVideoProgressNodeWithIdentifier(videoNodeIdentifier);
    if (videoProgressNode.length != 0){
        videoProgressNode.removeClass('failed');
    }

    // Display the compatibility overlay again if present
    videoContainerNode.find("span.upload-overlay").removeClass("failed");

    this.setProgressOnMedia(videoNodeIdentifier, 0);

    if (nativeState.androidApiLevel > 18) {
        setTimeout(ZSSEditor.setupOptimisticProgressUpdate, 300, videoNodeIdentifier, 1);
    }
};

/**
 *  @brief      Remove the Video from the DOM.
 *
 *  @param      videoNodeIdentifier     This is a unique ID provided by the caller.
 */
ZSSEditor.removeVideo = function(videoNodeIdentifier) {
    var videoNode = this.getVideoNodeWithIdentifier(videoNodeIdentifier);
    if (videoNode.length != 0){
        videoNode.remove();
    }

    // if Video is inside options container we need to remove the container
    var videoContainerNode = this.getVideoContainerNodeWithIdentifier(videoNodeIdentifier);
    if (videoContainerNode.length != 0){
        // Reset id before removal to avoid triggering the manual media removal callback
        videoContainerNode.attr("id","");
        videoContainerNode.remove();
    }
};

/**
 *  @brief      Wrap the video in an edit-container with a delete button overlay.
 */
ZSSEditor.applyEditContainer = function(videoNode) {
    var containerHtml = '<span class="edit-container" contenteditable="false"><span class="delete-overlay"></span></span>';
    videoNode.insertAdjacentHTML('beforebegin', containerHtml);

    var selectionNode = videoNode.previousSibling;
    selectionNode.appendChild(videoNode);

    return selectionNode;
}

ZSSEditor.replaceVideoPressVideosForShortcode = function ( html) {
    // call methods to restore any transformed content from its visual presentation to its source code.
    var regex = /<video[^>]*data-wpvideopress="([\s\S]+?)"[^>]*>*<\/video>/g;
    var str = html.replace( regex, ZSSEditor.removeVideoPressVisualFormattingCallback );

    return str;
}

ZSSEditor.replaceVideosForShortcode = function ( html) {
    var regex = /<video(?:(?!data-wpvideopress).)*><\/video>/g;
    var str = html.replace( regex, ZSSEditor.removeVideoVisualFormattingCallback );

    return str;
}

ZSSEditor.removeVideoContainers = function(html) {
    var containerRegex = /<span class="edit-container" contenteditable="false">(?:<span class="delete-overlay"[^<>]*><\/span>)?(\[[^<>]*)<\/span>/g;
    var str = html.replace(containerRegex, ZSSEditor.removeVideoContainerCallback);

    return str;
}

ZSSEditor.removeVideoPressVisualFormattingCallback = function( match, content ) {
    return "[wpvideo " + content + "]";
}

ZSSEditor.removeVideoVisualFormattingCallback = function( match, content ) {
    var videoElement = $.parseHTML(match)[0];

    // Remove editor playback attributes
    videoElement.removeAttribute("onclick");
    videoElement.removeAttribute("controls");
    videoElement.removeAttribute("webkit-playsinline");
    if (videoElement.getAttribute("preload") == "metadata") {
        // The "metadata" setting is the WP default and is usually automatically stripped from the shortcode.
        // If it's present, it was probably set by this editor and we should remove it. Even if it wasn't, removing it
        // won't affect anything as it's the default setting for the preload attribute.
        videoElement.removeAttribute("preload");
    }

    // If filetype attributes exist, the src attribute wasn't there originally and we should remove it
    for (var i = 0; i < Formatter.videoShortcodeFormats.length; i++) {
        var format = Formatter.videoShortcodeFormats[i];
        if (videoElement.hasAttribute(format)) {
            videoElement.removeAttribute("src");
            break;
        }
    }

    var shortcode = videoElement.outerHTML.replace(/</g, "[");
    shortcode = shortcode.replace(/>/g, "]");
    return shortcode;
}

ZSSEditor.removeVideoContainerCallback = function( match, content ) {
    return content;
}

/**
 *  @brief      Sets the VideoPress video URL and poster URL on a video tag.
 *  @details    When switching from HTML to visual mode, wpvideo shortcodes are replaced by video tags.
 *              A request is sent using ZSSEditor.sendVideoPressInfoRequest() to obtain the video url matching each
 *              wpvideo shortcode. This function must be called to set the url for each videopress id.
 *
 *  @param      videopressID      VideoPress identifier of the video.
 *  @param      videoURL          URL of the video file to display.
 *  @param      posterURL         URL of the poster image to display
 */
ZSSEditor.setVideoPressLinks = function(videopressID, videoURL, posterURL ) {
    var videoNode = $('video[data-wpvideopress="' + videopressID + '"]');
    if (videoNode.length == 0) {
        return;
    }

    // It's safest to drop the onError now, to avoid endless calls if the video can't be loaded
    // Even if sendVideoPressInfoRequest failed, it's still possible to request a reload by tapping the video
    videoNode.attr('onError', '');

    if (videoURL.length == 0) {
        return;
    }

    videoNode.attr('src', videoURL);
    videoNode.attr('controls', '');
    videoNode.attr('poster', posterURL);
    var thisObj = this;
    videoNode.load();
};

/**
 *  @brief  Stops all video of playing
 *
 */
ZSSEditor.pauseAllVideos = function () {
    $('video').each(function() {
        this.pause();
    });
}

ZSSEditor.clearCurrentEditingImage = function() {
    ZSSEditor.currentEditingImage = null;
};

/**
 *  @brief      Updates the currently selected image, replacing its markup with
 *  new markup based on the specified meta data string.
 *
 *  @param      imageMetaString   A JSON string representing the updated meta data.
 */
ZSSEditor.updateCurrentImageMeta = function( imageMetaString ) {
    if ( !ZSSEditor.currentEditingImage ) {
        return;
    }

    var imageMeta = JSON.parse( imageMetaString );
    var html = ZSSEditor.createImageFromMeta( imageMeta );

    // Insert the updated html and remove the outdated node.
    // This approach is preferred to selecting the current node via a range,
    // and then replacing it when calling insertHTML. The insertHTML call can,
    // in certain cases, modify the current and inserted markup depending on what
    // elements surround the targeted node.  This approach is safer.
    var node = ZSSEditor.findImageCaptionNode( ZSSEditor.currentEditingImage );
    var parent = node.parentNode;

    node.insertAdjacentHTML( 'afterend', html );
    // Use {node}.{parent}.removeChild() instead of {node}.remove(), since Android API<19 doesn't support Node.remove()
    node.parentNode.removeChild(node);

    ZSSEditor.currentEditingImage = null;

    ZSSEditor.setFocusAfterElement(parent);
}

ZSSEditor.applyImageSelectionFormatting = function( imageNode ) {
    var node = ZSSEditor.findImageCaptionNode( imageNode );

    var overlay = '<span class="edit-overlay" contenteditable="false"><span class="edit-icon"></span>'
                  + '<span class="edit-content">' + nativeState.localizedStringEdit + '</span></span>';

    var sizeClass = "";
    if ( imageNode.width < 100 || imageNode.height < 100 ) {
        sizeClass = " small";
    } else {
        overlay = '<span class="delete-overlay" contenteditable="false"></span>' + overlay;
    }

    if (document.body.style.filter == null) {
        // CSS Filters (including blur) are not supported
        // Use dark semi-transparent background for edit overlay instead of blur in this case
        overlay = overlay + '<div class="edit-overlay-bg"></div>';
    }

    var html = '<span class="edit-container' + sizeClass + '">' + overlay + '</span>';
   	node.insertAdjacentHTML( 'beforebegin', html );
    var selectionNode = node.previousSibling;
    selectionNode.appendChild( node );

    this.trackNodeForMutation($(selectionNode));

    return selectionNode;
}

ZSSEditor.removeImageSelectionFormatting = function( imageNode ) {
    var node = ZSSEditor.findImageCaptionNode( imageNode );
    if ( !node.parentNode || node.parentNode.className.indexOf( "edit-container" ) == -1 ) {
        return;
    }

    var parentNode = node.parentNode;
    var container = parentNode.parentNode;

    if (container != null) {
        container.insertBefore( node, parentNode );
        // Use {node}.{parent}.removeChild() instead of {node}.remove(), since Android API<19 doesn't support Node.remove()
        container.removeChild(parentNode);
    }
}

ZSSEditor.removeImageSelectionFormattingFromHTML = function( html ) {
    var tmp = document.createElement( "div" );
    var tmpDom = $( tmp ).html( html );

    var matches = tmpDom.find( "span.edit-container img" );
    if ( matches.length == 0 ) {
        return html;
    }

    for ( var i = 0; i < matches.length; i++ ) {
        ZSSEditor.removeImageSelectionFormatting( matches[i] );
    }

    return tmpDom.html();
}

ZSSEditor.removeImageRemoteUrl = function(html) {
    var tmp = document.createElement("div");
    var tmpDom = $(tmp).html(html);

    var matches = tmpDom.find("img");
    if (matches.length == 0) {
        return html;
    }

    for (var i = 0; i < matches.length; i++) {
        if (matches[i].getAttribute('remoteurl')) {
            if (matches[i].parentNode && matches[i].parentNode.href === matches[i].src) {
                matches[i].parentNode.href = matches[i].getAttribute('remoteurl')
            }
            matches[i].src = matches[i].getAttribute('remoteurl');
            matches[i].removeAttribute('remoteurl');
        }
    }

    return tmpDom.html();
}

/**
 *  @brief       Finds all related caption nodes for the specified image node.
 *
 *  @param      imageNode   An image node in the DOM to inspect.
 */
ZSSEditor.findImageCaptionNode = function( imageNode ) {
    var node = imageNode;
    if ( node.parentNode && node.parentNode.nodeName === 'A' ) {
        node = node.parentNode;
    }

    if ( node.parentNode && node.parentNode.className.indexOf( 'wp-caption' ) != -1 ) {
        node = node.parentNode;
    }

    if ( node.parentNode && (node.parentNode.className.indexOf( 'wp-temp' ) != -1 ) ) {
        node = node.parentNode;
    }

    return node;
}

/**
 *  Modified from wp-includes/js/media-editor.js
 *  see `image`
 *
 *  @brief      Construct html markup for an image, and optionally a link an caption shortcode.
 *
 *  @param      props   A dictionary of properties used to compose the markup. See comments in extractImageMeta.
 *
 *  @return     Returns the html mark up as a string
 */
ZSSEditor.createImageFromMeta = function( props ) {
    var img = {},
    options, classes, shortcode, html;

    classes = props.classes || [];
    if ( ! ( classes instanceof Array ) ) {
        classes = classes.split( ' ' );
    }

    _.extend( img, _.pick( props, 'width', 'height', 'alt', 'src', 'title' ) );

    // Only assign the align class to the image if we're not printing
    // a caption, since the alignment is sent to the shortcode.
    if ( props.align && ! props.caption ) {
        classes.push( 'align' + props.align );
    }

    if ( props.size ) {
        classes.push( 'size-' + props.size );
    }

    if ( props.attachment_id ) {
        classes.push( 'wp-image-' + props.attachment_id );
    }

    img['class'] = _.compact( classes ).join(' ');

    // Generate `img` tag options.
    options = {
        tag:    'img',
        attrs:  img,
        single: true
    };

    // Generate the `a` element options, if they exist.
    if ( props.linkUrl ) {
        options = {
            tag:   'a',
            attrs: {
                href: props.linkUrl
            },
            content: options
        };

        if ( props.linkClassName ) {
            options.attrs.class = props.linkClassName;
        }

        if ( props.linkRel ) {
            options.attrs.rel = props.linkRel;
        }

        if ( props.linkTargetBlank ) { // expects a boolean
            options.attrs.target = "_blank";
        }
    }

    html = wp.html.string( options );

    // Generate the caption shortcode.
    if ( props.caption ) {
        shortcode = {};

        if ( img.width ) {
            shortcode.width = img.width;
        }

        if ( props.captionId ) {
            shortcode.id = props.captionId;
        }

        if ( props.align ) {
            shortcode.align = 'align' + props.align;
        } else {
            shortcode.align = 'alignnone';
        }

        if (props.captionClassName) {
            shortcode.class = props.captionClassName;
        }

        html = wp.shortcode.string({
            tag:     'caption',
            attrs:   shortcode,
            content: html + props.caption
        });

        html = Formatter.applyVisualFormatting( html );
    }

    return html;
};

/**
 *  Modified from wp-includes/js/tinymce/plugins/wpeditimage/plugin.js
 *  see `extractImageData`
 *
 *  @brief      Extracts properties and meta data from an image, and optionally its link and caption.
 *
 *  @param      imageNode   An image node in the DOM to inspect.
 *
 *  @return     Returns an object containing the extracted properties and meta data.
 */
ZSSEditor.extractImageMeta = function( imageNode ) {
    var classes, extraClasses, metadata, captionBlock, caption, link, width, height,
    captionClassName = [],
    isIntRegExp = /^\d+$/;

    // Default attributes. All values are strings, except linkTargetBlank
    metadata = {
        align: 'none',      // Accepted values: center, left, right or empty string.
        alt: '',            // Image alt attribute
        attachment_id: '',  // Numeric attachment id of the image in the site's media library
        caption: '',        // The text of the caption for the image (if any)
        captionClassName: '', // The classes for the caption shortcode (if any).
        captionId: '',      // The caption shortcode's ID attribute. The numeric value should match the value of attachment_id
        classes: '',        // The class attribute for the image. Does not include editor generated classes
        height: '',         // The image height attribute
        linkClassName: '',  // The class attribute for the link
        linkRel: '',        // The rel attribute for the link (if any)
        linkTargetBlank: false, // true if the link should open in a new window.
        linkUrl: '',        // The href attribute of the link
        size: 'custom',     // Accepted values: custom, medium, large, thumbnail, or empty string
        src: '',            // The src attribute of the image
        title: '',          // The title attribute of the image (if any)
        width: '',          // The image width attribute
        naturalWidth:'',    // The natural width of the image.
        naturalHeight:''    // The natural height of the image.
    };

    // populate metadata with values of matched attributes
    metadata.src = $( imageNode ).attr( 'src' ) || '';
    metadata.alt = $( imageNode ).attr( 'alt' ) || '';
    metadata.title = $( imageNode ).attr( 'title' ) || '';
    metadata.naturalWidth = imageNode.naturalWidth;
    metadata.naturalHeight = imageNode.naturalHeight;

    width = $(imageNode).attr( 'width' );
    height = $(imageNode).attr( 'height' );

    if ( ! isIntRegExp.test( width ) || parseInt( width, 10 ) < 1 ) {
        width = imageNode.naturalWidth || imageNode.width;
    }

    if ( ! isIntRegExp.test( height ) || parseInt( height, 10 ) < 1 ) {
        height = imageNode.naturalHeight || imageNode.height;
    }

    metadata.width = width;
    metadata.height = height;

    classes = imageNode.className.split( /\s+/ );
    extraClasses = [];

    $.each( classes, function( index, value ) {
        if ( /^wp-image/.test( value ) ) {
           metadata.attachment_id = parseInt( value.replace( 'wp-image-', '' ), 10 );
        } else if ( /^align/.test( value ) ) {
           metadata.align = value.replace( 'align', '' );
        } else if ( /^size/.test( value ) ) {
           metadata.size = value.replace( 'size-', '' );
        } else {
           extraClasses.push( value );
        }
    } );

    metadata.classes = extraClasses.join( ' ' );

    // Extract caption
    var captionMeta = ZSSEditor.captionMetaForImage( imageNode )
    if (captionMeta.caption != '') {
        metadata = $.extend( metadata, captionMeta );
    }

    // Extract linkTo
    if ( imageNode.parentNode && imageNode.parentNode.nodeName === 'A' ) {
        link = imageNode.parentNode;
        metadata.linkClassName = link.className;
        metadata.linkRel = $( link ).attr( 'rel' ) || '';
        metadata.linkTargetBlank = $( link ).attr( 'target' ) === '_blank' ? true : false;
        metadata.linkUrl = $( link ).attr( 'href' ) || '';
    }

    return metadata;
};

/**
 *  @brief      Extracts the caption shortcode for an image.
 *
 *  @param      imageNode   An image node in the DOM to inspect.
 *
 *  @return     Returns a shortcode match (if any) for the passed image node.
 *  See shortcode.js::next for details
 */
ZSSEditor.getCaptionForImage = function( imageNode ) {
    var node = ZSSEditor.findImageCaptionNode( imageNode );

    // Ensure we're working with the formatted caption
    if ( node.className.indexOf( 'wp-temp' ) == -1 ) {
        return;
    }

    var html = node.outerHTML;
    html = ZSSEditor.removeVisualFormatting( html );

    return wp.shortcode.next( "caption", html, 0 );
};

/**
 *  @brief      Extracts meta data for the caption (if any) for the passed image node.
 *
 *  @param      imageNode   An image node in the DOM to inspect.
 *
 *  @return     Returns an object containing the extracted meta data.
 *  See shortcode.js::next or details
 */
ZSSEditor.captionMetaForImage = function( imageNode ) {
    var attrs,
        meta = {
            align: '',
            caption: '',
            captionClassName: '',
            captionId: ''
        };

    var caption = ZSSEditor.getCaptionForImage( imageNode );
    if ( !caption ) {
        return meta;
    }

    attrs = caption.shortcode.attrs.named;
    if ( attrs.align ) {
        meta.align = attrs.align.replace( 'align', '' );
    }
    if ( attrs.class ) {
        meta.captionClassName = attrs.class;
    }
    if ( attrs.id ) {
        meta.captionId = attrs.id;
    }
    meta.caption = caption.shortcode.content.substr( caption.shortcode.content.lastIndexOf( ">" ) + 1 );

    return meta;
}

/**
 *  @brief      Removes custom visual formatting for caption shortcodes.
 *
 *  @param      html   The markup to process
 *
 *  @return     The html with formatted captions restored to the original shortcode markup.
 */
ZSSEditor.removeCaptionFormatting = function( html ) {
    // call methods to restore any transformed content from its visual presentation to its source code.
    var regex = /<label class="wp-temp" data-wp-temp="caption"[^>]*>([\s\S]+?)<\/label>/g;

    var str = html.replace( regex, ZSSEditor.removeCaptionFormattingCallback );

    return str;
}

ZSSEditor.removeCaptionFormattingCallback = function( match, content ) {
    // TODO: check is a visual temp node
    var out = '';

    if ( content.indexOf('<img ') === -1 ) {
        // Broken caption. The user managed to drag the image out?
        // Try to return the caption text as a paragraph.
        out = content.match( /\s*<span [^>]*>([\s\S]+?)<\/span>/gi );

        if ( out && out[1] ) {
            return '<p>' + out[1] + '</p>';
        }

        return '';
    }

    out = content.replace( /\s*<span ([^>]*)>([\s\S]+?)<\/span>/gi, function( ignoreMatch, attrStr, content ) {
        if ( ! content ) {
            return '';
        }

        var id, classes, align, width, attrs = {};

        width = attrStr.match( /data-caption-width="([0-9]*)"/ );
        width = ( width && width[1] ) ? width[1] : '';
        if ( width ) {
            attrs.width = width;
        }

        id = attrStr.match( /data-caption-id="([^"]*)"/ );
        id = ( id && id[1] ) ? id[1] : '';
        if ( id ) {
            attrs.id = id;
        }

        classes = attrStr.match( /data-caption-class="([^"]*)"/ );
        classes = ( classes && classes[1] ) ? classes[1] : '';
        if ( classes ) {
            attrs.class = classes;
        }

        align = attrStr.match( /data-caption-align="([^"]*)"/ );
        align = ( align && align[1] ) ? align[1] : '';
        if ( align ) {
            attrs.align = align;
        }

        var options = {
            'tag':'caption',
            'attrs':attrs,
            'type':'closed',
            'content':content
        };

        return wp.shortcode.string( options );
    });

    return out;
}

// MARK: - Galleries

ZSSEditor.insertGallery = function( imageIds, type, columns ) {
    var shortcode;
    if (type) {
        shortcode = '[gallery type="' + type + '" ids="' + imageIds + '"]';
    } else {
        shortcode = '[gallery columns="' + columns + '" ids="' + imageIds + '"]';
    }

    this.insertHTMLWrappedInParagraphTags(shortcode);
}

ZSSEditor.insertLocalGallery = function( placeholderId ) {
    var container = '<span id="' + placeholderId + '" class="gallery_container">['
                    + nativeState.localizedStringUploadingGallery + ']</span>';
    this.insertHTMLWrappedInParagraphTags(container);
}

ZSSEditor.replacePlaceholderGallery = function( placeholderId, imageIds, type, columns ) {
    var span = 'span#' + placeholderId + '.gallery_container';

    var shortcode;
    if (type) {
        shortcode = '[gallery type="' + type + '" ids="' + imageIds + '"]';
    } else {
        shortcode = '[gallery columns="' + columns + '" ids="' + imageIds + '"]';
    }

    $(span).replaceWith(shortcode);
}

// MARK: - Commands

/**
 *  @brief      Removes editor specific visual formatting
 *
 *  @param      html   The markup to remove formatting
 *
 *  @return     Returns the string with the visual formatting removed.
 */
ZSSEditor.removeVisualFormatting = function( html ) {
    var str = html;
    str = ZSSEditor.removeImageRemoteUrl( str );
    str = ZSSEditor.removeImageSelectionFormattingFromHTML( str );
    str = ZSSEditor.removeCaptionFormatting( str );
    str = ZSSEditor.replaceVideoPressVideosForShortcode( str );
    str = ZSSEditor.replaceVideosForShortcode( str );
    str = ZSSEditor.removeVideoContainers( str );

    // More tag
    str = str.replace(/<hr class="more-tag" wp-more-data="(.*?)">/igm, "<!--more$1-->")
    str = str.replace(/<hr class="nextpage-tag">/igm, "<!--nextpage-->")
    return str;
};

ZSSEditor.insertHTML = function(html) {
	document.execCommand('insertHTML', false, html);
	this.sendEnabledStyles();
};

ZSSEditor.isCommandEnabled = function(commandName) {
	return document.queryCommandState(commandName);
};

ZSSEditor.sendEnabledStyles = function(e) {

	var items = [];

    var focusedField = this.getFocusedField();

    if (!focusedField.hasNoStyle) {
        // Find all relevant parent tags
        var parentTags = ZSSEditor.parentTags();

        if (parentTags != null) {
            for (var i = 0; i < parentTags.length; i++) {
                var currentNode = parentTags[i];

                if (currentNode.nodeName.toLowerCase() == 'a') {
                    ZSSEditor.currentEditingLink = currentNode;

                    var title = encodeURIComponent(currentNode.text);
                    var href = encodeURIComponent(currentNode.href);

                    items.push('link-title:' + title);
                    items.push('link:' + href);
                } else if (currentNode.nodeName == NodeName.BLOCKQUOTE) {
                    items.push('blockquote');
                }
            }
        }

        if (ZSSEditor.isCommandEnabled('bold')) {
            items.push('bold');
        }
        if (ZSSEditor.isCommandEnabled('createLink')) {
            items.push('createLink');
        }
        if (ZSSEditor.isCommandEnabled('italic')) {
            items.push('italic');
        }
        if (ZSSEditor.isCommandEnabled('subscript')) {
            items.push('subscript');
        }
        if (ZSSEditor.isCommandEnabled('superscript')) {
            items.push('superscript');
        }
        if (ZSSEditor.isCommandEnabled('strikeThrough')) {
            items.push('strikeThrough');
        }
        if (ZSSEditor.isCommandEnabled('underline')) {
            var isUnderlined = false;

            // DRM: 'underline' gets highlighted if it's inside of a link... so we need a special test
            // in that case.
            if (!ZSSEditor.currentEditingLink) {
                items.push('underline');
            }
        }
        if (ZSSEditor.isCommandEnabled('insertOrderedList')) {
            items.push('orderedList');
        }
        if (ZSSEditor.isCommandEnabled('insertUnorderedList')) {
            items.push('unorderedList');
        }
        if (ZSSEditor.isCommandEnabled('justifyCenter')) {
            items.push('justifyCenter');
        }
        if (ZSSEditor.isCommandEnabled('justifyFull')) {
            items.push('justifyFull');
        }
        if (ZSSEditor.isCommandEnabled('justifyLeft')) {
            items.push('justifyLeft');
        }
        if (ZSSEditor.isCommandEnabled('justifyRight')) {
            items.push('justifyRight');
        }
        if (ZSSEditor.isCommandEnabled('insertHorizontalRule')) {
            items.push('horizontalRule');
        }
        var formatBlock = document.queryCommandValue('formatBlock');
        if (formatBlock.length > 0) {
            items.push(formatBlock);
        }

        // Use jQuery to figure out those that are not supported
        if (typeof(e) != "undefined") {

            // The target element
            var t = $(e.target);
            var nodeName = e.target.nodeName.toLowerCase();

            // Background Color
            try
            {
                var bgColor = t.css('backgroundColor');
                if (bgColor && bgColor.length != 0 && bgColor != 'rgba(0, 0, 0, 0)' && bgColor != 'rgb(0, 0, 0)' && bgColor != 'transparent') {
                    items.push('backgroundColor');
                }
            }
            catch(e)
            {
                // DRM: I had to add these stupid try-catch blocks to solve an issue with t.css throwing
                // exceptions for no reason.
            }

            // Text Color
            try
            {
                var textColor = t.css('color');
                if (textColor && textColor.length != 0 && textColor != 'rgba(0, 0, 0, 0)' && textColor != 'rgb(0, 0, 0)' && textColor != 'transparent') {
                    items.push('textColor');
                }
            }
            catch(e)
            {
                // DRM: I had to add these stupid try-catch blocks to solve an issue with t.css throwing
                // exceptions for no reason.
            }

            // Image
            if (nodeName == 'img') {
                ZSSEditor.currentEditingImage = t;
                items.push('image:'+t.attr('src'));
                if (t.attr('alt') !== undefined) {
                    items.push('image-alt:'+t.attr('alt'));
                }
            }
        }
    }

	ZSSEditor.stylesCallback(items);
};

// MARK: - Commands: High Level Editing

/**
 *  @brief      Inserts a br tag at the caret position.
 */
ZSSEditor.insertBreakTagAtCaretPosition = function() {
    // iOS IMPORTANT: we were adding <br> tags with range.insertNode() before using
    // this method.  Unfortunately this was causing issues with the first <br> tag
    // being completely ignored under iOS:
    //
    // https://bugs.webkit.org/show_bug.cgi?id=23474
    //
    // The following line seems to work fine under iOS, so please be careful if this
    // needs to be changed for any reason.
    //
    document.execCommand("insertLineBreak");
};

// MARK: - Advanced Node Manipulation

/**
 *  @brief      Given the specified node, find the previous node in the DOM.
 *
 *  @param      node       The node used as a starting point for the "previous" search.
 *
 *  @returns    If a previous node is found, it will be returned otherwise null;
 */
ZSSEditor.previousNode = function(node) {
    if (!node) {
        return null;
    }
    var previous = node.previousSibling;
    if (previous) {
        node = previous;
        while (node.hasChildNodes()) {
            node = node.lastChild;
        }
        return node;
    }
    var parent = node.parentNode;
    if (parent && parent.hasChildNodes()) {
        return parent;
    }
    return null;
};

/**
 *  @brief      Ends the editing of a list (either UL or OL).
 *
 *  @details    This function finds the list node, inserts a new paragraph as a sibling to the list node
 *              then scrubs any <br> tags created as part of the insertParagraph command.
 */
ZSSEditor.completeListEditing = function() {
    // Get the current selection
    var sel = window.getSelection();
    if (sel && sel.rangeCount > 0) {
        var range = sel.getRangeAt(0);
        var node = range.startContainer;
        if (node.hasChildNodes() && range.startOffset > 0) {
            node = node.childNodes[range.startOffset - 1];
        }

        // Walk backwards through the DOM until we find an ul or ol
        while (node) {
            if (node.nodeType == 1 &&
                    (node.tagName.toUpperCase() == NodeName.UL
                        || node.tagName.toUpperCase() == NodeName.OL)) {

                var focusedNode = document.getSelection().getRangeAt(0).startContainer;

                if (focusedNode.nodeType == 3) {
                    // If the focused node is a text node, the list item was not empty when toggled off
                    // Wrap the text in a div and attach it as a sibling to the div wrapping the list
                    var parentParagraph = focusedNode.parentNode;
                    var paragraph = document.createElement('div');

                    paragraph.appendChild(focusedNode);
                    parentParagraph.insertAdjacentElement('afterEnd', paragraph);

                    ZSSEditor.giveFocusToElement(paragraph, 1);
                } else {
                    // Attach a new paragraph node as a sibling to the parent node
                    document.execCommand('insertParagraph', false);
                }

                // Remove any superfluous <br> tags that are created
                ZSSEditor.scrubBRFromNode(node.parentNode);
                break;
            }
            node = ZSSEditor.previousNode(node);
        }
    }
}

/**
 *  @brief      Given the specified node, remove all instances of <br> from it and it's children.
 *
 *  @param      node       The node to scrub
 */
ZSSEditor.scrubBRFromNode = function(node) {
    if (!node) {
        return;
    }
    $(node).contents().filter(NodeName.BR).remove();
};

/**
 *  @brief      Extracts a node from a parent node, and from all nodes in between the two.
 */
ZSSEditor.extractNodeFromAncestorNode = function(descendant, ancestor) {

    while (ancestor.contains(descendant)) {

        this.extractNodeFromParent(descendant);
        break;
    }
};

/**
 *  @brief      Extract the specified node from its direct parent node.
 *  @details    If the node has siblings, before or after it, the parent node is split accordingly
 *              into two new clones of it.
 */
ZSSEditor.extractNodeFromParent = function(node) {

    var parentNode = node.parentNode;
    var grandParentNode = parentNode.parentNode;
    var clonedParentForPreviousSiblings = null;
    var clonedParentForNextSiblings = null;

    if (node.previousSibling != null) {
        var clonedParentForPreviousSiblings = parentNode.cloneNode();

        while (parentNode.firstChild != node) {
            clonedParentForPreviousSiblings.appendChild(parentNode.firstChild);
        }
    }

    if (node.nextSibling != null) {
        var clonedParentForNextSiblings = parentNode.cloneNode();

        while (node.nextSibling != null) {
            clonedParentForNextSiblings.appendChild(node.nextSibling);
        }
    }

    if (clonedParentForPreviousSiblings) {
        grandParentNode.insertBefore(clonedParentForPreviousSiblings, parentNode);
    }

    grandParentNode.insertBefore(node, parentNode);

    if (clonedParentForNextSiblings) {
        grandParentNode.insertBefore(clonedParentForNextSiblings, parentNode);
    }

    grandParentNode.removeChild(parentNode);
};

ZSSEditor.getChildNodesIntersectingRange = function(parentNode, range) {

    var nodes = new Array();

    if (parentNode) {
        var currentNode = parentNode.firstChild;
        var pushNodes = false;
        var exit = false;

        while (currentNode) {

            if (range.intersectsNode(currentNode)) {
                nodes.push(currentNode);
            }

            currentNode = currentNode.nextSibling;
        }
    }

    return nodes;
};

/**
 *  @brief      Given the specified range, find the ancestor element that  will be used to set the
 *              blockquote ON or OFF.
 *
 *  @param      range       The range we want to set the blockquote ON or OFF for.
 *
 *  @returns    If a parent BLOCKQUOTE element is found, it will be return.  Otherwise the closest
 *              parent element will be returned.
 */
ZSSEditor.getAncestorElementForSettingBlockquote = function(range) {

    var nodes = new Array();
    var parentElement = range.commonAncestorContainer;

    while (parentElement
           && (parentElement.nodeType != document.ELEMENT_NODE
               || parentElement.nodeName == NodeName.PARAGRAPH
               || parentElement.nodeName == NodeName.STRONG
               || parentElement.nodeName == NodeName.EM
               || parentElement.nodeName == NodeName.DEL
               || parentElement.nodeName == NodeName.A
               || parentElement.nodeName == NodeName.UL
               || parentElement.nodeName == NodeName.OL
               || parentElement.nodeName == NodeName.LI
               || parentElement.nodeName == NodeName.CODE
               || parentElement.nodeName == NodeName.SPAN
               // Include nested divs, but ignore the parent contenteditable field div
               || (parentElement.nodeName == NodeName.DIV && parentElement.parentElement.nodeName != NodeName.BODY))) {
        parentElement = parentElement.parentNode;
    }

    var currentElement = parentElement;

    while (currentElement
           && currentElement.nodeName != NodeName.BLOCKQUOTE) {
        currentElement = currentElement.parentElement;
    }

    var result = currentElement ? currentElement : parentElement;

    return result;
};

/**
 *  @brief      Joins any adjacent blockquote siblings.
 *  @details    You probably want to call joinAdjacentSiblingsOrAncestorBlockquotes() instead of
 *              this.
 *
 *  @returns    true if a sibling was joined.  false otherwise.
 */
ZSSEditor.joinAdjacentSiblingsBlockquotes = function(node) {

    var shouldJoinToPreviousSibling = this.hasPreviousSiblingWithName(node, NodeName.BLOCKQUOTE);
    var shouldJoinToNextSibling = this.hasNextSiblingWithName(node, NodeName.BLOCKQUOTE);
    var joinedASibling = (shouldJoinToPreviousSibling || shouldJoinToNextSibling);

    var previousSibling = node.previousSibling;
    var nextSibling = node.nextSibling;

    if (shouldJoinToPreviousSibling) {

        previousSibling.appendChild(node);

        if (shouldJoinToNextSibling) {

            while (nextSibling.firstChild) {
                previousSibling.appendChild(nextSibling.firstChild);
            }

            nextSibling.parentNode.removeChild(nextSibling);
        }
    } else if (shouldJoinToNextSibling) {

        nextSibling.insertBefore(node, nextSibling.firstChild);
    }

    return joinedASibling;
};

/**
 *  @brief      Joins any adjacent blockquote siblings, or the blockquote siblings of any ancestor.
 *  @details    When turning blockquotes back on, this method makes sure that we attach new
 *              blockquotes to exiting ones.
 *
 *  @returns    true if a sibling or ancestor sibling was joined.  false otherwise.
 */
ZSSEditor.joinAdjacentSiblingsOrAncestorBlockquotes = function(node) {

    var currentNode = node;
    var rootNode = this.getFocusedField().getWrappedDomNode();
    var joined = false;

    while (currentNode
           && currentNode != rootNode
           && !joined) {

        joined = this.joinAdjacentSiblingsBlockquotes(currentNode);
        currentNode = currentNode.parentNode;
    };

    return joined;
};

/**
 *  @brief      Surrounds a node's contents into another node
 *  @details    When creating new nodes that should force paragraphs inside of them, this method
 *              should be called.
 *
 *  @param      node            The node that will have its contents wrapped into a new node.
 *  @param      wrapperNodeName The nodeName of the node that will created to wrap the contents.
 *
 *  @returns    The newly created wrapper node.
 */
ZSSEditor.surroundNodeContentsWithNode = function(node, wrapperNodeName) {

    var range = document.createRange();
    var wrapperNode = document.createElement(wrapperNodeName);

    range.selectNodeContents(node);
    range.surroundContents(wrapperNode);

    return wrapperNode;
};

/**
 *  @brief      Surrounds a node's contents with a paragraph node.
 *  @details    When creating new nodes that should force paragraphs inside of them, this method
 *              should be called.
 *
 *  @returns    The paragraph node.
 */
ZSSEditor.surroundNodeContentsWithAParagraphNode = function(node) {

    return this.surroundNodeContentsWithNode(node, this.defaultParagraphSeparator);
};

// MARK: - Sibling nodes

ZSSEditor.hasNextSiblingWithName = function(node, siblingNodeName) {
    return node.nextSibling && node.nextSibling.nodeName == siblingNodeName;
};

ZSSEditor.hasPreviousSiblingWithName = function(node, siblingNodeName) {
    return node.previousSibling && node.previousSibling.nodeName == siblingNodeName;
};


// MARK: - Parent nodes & tags

ZSSEditor.findParentContenteditableDiv = function() {
    var parentNode = null;
    var selection = window.getSelection();
    if (selection.rangeCount < 1) {
        return null;
    }
    var range = selection.getRangeAt(0).cloneRange();

    var referenceNode = this.closerParentNodeWithNameRelativeToNode('div', range.commonAncestorContainer);

    while (referenceNode.parentNode.nodeName != NodeName.BODY) {
        referenceNode = this.closerParentNodeWithNameRelativeToNode('div', referenceNode.parentNode);
    }

    return referenceNode;
};

ZSSEditor.closerParentNode = function() {

    var parentNode = null;
    var selection = window.getSelection();
    if (selection.rangeCount < 1) {
        return null;
    }
    var range = selection.getRangeAt(0).cloneRange();

    var currentNode = range.commonAncestorContainer;

    while (currentNode) {
        if (currentNode.nodeType == document.ELEMENT_NODE) {
            parentNode = currentNode;

            break;
        }

        currentNode = currentNode.parentElement;
    }

    return parentNode;
};

ZSSEditor.closerParentNodeStartingAtNode = function(nodeName, startingNode) {

    nodeName = nodeName.toLowerCase();

    var parentNode = null;
    var currentNode = startingNode.parentElement;

    while (currentNode) {

        if (currentNode.nodeName == document.body.nodeName) {
            break;
        }

        if (currentNode.nodeName && currentNode.nodeName.toLowerCase() == nodeName
            && currentNode.nodeType == document.ELEMENT_NODE) {
            parentNode = currentNode;

            break;
        }

        currentNode = currentNode.parentElement;
    }

    return parentNode;
};

ZSSEditor.closerParentNodeWithName = function(nodeName) {

    nodeName = nodeName.toLowerCase();

    var parentNode = null;
    var selection = window.getSelection();
    if (selection.rangeCount < 1) {
        return null;
    }
    var range = selection.getRangeAt(0).cloneRange();

    var referenceNode = range.commonAncestorContainer;

    return this.closerParentNodeWithNameRelativeToNode(nodeName, referenceNode);
};

ZSSEditor.closerParentNodeWithNameRelativeToNode = function(nodeName, referenceNode) {

    nodeName = nodeName.toUpperCase();

    var parentNode = null;
    var currentNode = referenceNode;

    while (currentNode) {

        if (currentNode.nodeName == document.body.nodeName) {
            break;
        }

        if (currentNode.nodeName == nodeName
            && currentNode.nodeType == document.ELEMENT_NODE) {
            parentNode = currentNode;

            break;
        }

        currentNode = currentNode.parentElement;
    }

    return parentNode;
};

ZSSEditor.isCloserParentNodeABlockquote = function() {
    return this.closerParentNode().nodeName == NodeName.BLOCKQUOTE;
};

ZSSEditor.parentTags = function() {

    var parentTags = [];
    var selection = window.getSelection();
    if (selection.rangeCount < 1) {
        return null;
    }
    var range = selection.getRangeAt(0);

    var currentNode = range.commonAncestorContainer;
    while (currentNode) {

        if (currentNode.nodeName == document.body.nodeName) {
            break;
        }

        if (currentNode.nodeType == document.ELEMENT_NODE) {
            parentTags.push(currentNode);
        }

        currentNode = currentNode.parentElement;
    }

    return parentTags;
};

// MARK: - Range handling

ZSSEditor.getParentRangeOfFocusedNode = function() {
    var selection = window.getSelection();
    if (selection.focusNode == null) {
        return null;
    }
    return selection.getRangeAt(selection.focusNode.parentNode);
};

ZSSEditor.setRange = function(range) {
    window.getSelection().removeAllRanges();
    window.getSelection().addRange(range);
};
// MARK: - ZSSField Constructor

function ZSSField(wrappedObject) {
    // When this bool is true, we are going to restrict input and certain callbacks
    // so IME keyboards behave properly when composing.
    this.isComposing = false;

    this.multiline = false;
    this.wrappedObject = wrappedObject;

    if (this.getWrappedDomNode().hasAttribute('nostyle')) {
        this.hasNoStyle = true;
    }

    this.useVisualFormatting = (this.wrappedObject.data("wpUseVisualFormatting") === "true")

    this.bindListeners();
};

ZSSField.prototype.bindListeners = function() {

    var thisObj = this;

    this.wrappedObject.bind('tap', function(e) { thisObj.handleTapEvent(e); });
    this.wrappedObject.bind('focus', function(e) { thisObj.handleFocusEvent(e); });
    this.wrappedObject.bind('blur', function(e) { thisObj.handleBlurEvent(e); });
    this.wrappedObject.bind('keydown', function(e) { thisObj.handleKeyDownEvent(e); });
    this.wrappedObject.bind('input', function(e) { thisObj.handleInputEvent(e); });
    this.wrappedObject.bind('compositionstart', function(e) { thisObj.handleCompositionStartEvent(e); });
    this.wrappedObject.bind('compositionend', function(e) { thisObj.handleCompositionEndEvent(e); });

    // Only supported on API19+
    this.wrappedObject.on('paste', function(e) { thisObj.handlePasteEvent(e); });
};

// MARK: - Emptying the field when it should be, well... empty (HTML madness)

/**
 *  @brief      Sometimes HTML leaves some <br> tags or &nbsp; when the user deletes all
 *              text from a contentEditable field.  This code makes sure no such 'garbage' survives.
 *  @details    If the node contains child image nodes, then the content is left untouched.
 */
ZSSField.prototype.emptyFieldIfNoContents = function() {

    var nbsp = '\xa0';
    var text = this.wrappedObject.text().replace(nbsp, '');

    if (text.length == 0 || text == '\u000A') {

        var hasChildImages = (this.wrappedObject.find('img').length > 0);
        var hasChildVideos = (this.wrappedObject.find('video').length > 0);
        var hasUnorderedList = (this.wrappedObject.find('ul').length > 0);
        var hasOrderedList = (this.wrappedObject.find('ol').length > 0);

        if (!hasChildImages && !hasChildVideos && !hasUnorderedList && !hasOrderedList) {
            this.wrappedObject.empty();
        }
    }
};

// MARK: - Handle event listeners

ZSSField.prototype.handleBlurEvent = function(e) {
    ZSSEditor.focusedField = null;

    this.emptyFieldIfNoContents();

    this.callback("callback-focus-out");
};

ZSSField.prototype.handleFocusEvent = function(e) {
    ZSSEditor.focusedField = this;

    this.callback("callback-focus-in");
};

ZSSField.prototype.handleKeyDownEvent = function(e) {

    var wasEnterPressed = (e.keyCode == '13');
    var isHardwareKeyboardPaste = (e.ctrlKey && e.keyCode == '86');

    // Handle keyDownEvent actions that need to happen after the event has completed (and the field has been modified)
    setTimeout(this.afterKeyDownEvent, 20, e.target.innerHTML, e);

    if (this.isComposing) {
        e.stopPropagation();
    } else if (wasEnterPressed && !this.isMultiline()) {
        e.preventDefault();
    } else if (this.isMultiline()) {
        // For hardware keyboards, don't do any paragraph handling for non-printable keyCodes
        // https://css-tricks.com/snippets/javascript/javascript-keycodes/
        // This avoids the filler zero-width space character from being inserted and displayed in the content field
        // when special keys are pressed in new posts
        var wasTabPressed = (e.keyCode == '9');
        var intKeyCode = parseInt(e.keyCode, 10);
        if (wasTabPressed || (intKeyCode > 13 && intKeyCode < 46) || intKeyCode == 192) {
            return;
        }

        // The containsParagraphSeparators check is intended to work around three bugs:
        // 1. On API19 only, paragraph wrapping the first character in a post will display a zero-width space character
        // (from ZSSField.wrapCaretInParagraphIfNecessary)
        // We can drop the if statement wrapping wrapCaretInParagraphIfNecessary() if we find a way to stop using
        // zero-width space characters (e.g., autocorrect issues are fixed and we switch back to p tags)
        //
        // 2. On all APIs, software pasting (long press -> paste) doesn't automatically wrap the paste in paragraph
        // tags in new posts. On API19+ this is corrected by ZSSField.handlePasteEvent(), but earlier APIs don't support
        // it. So, instead, we allow the editor not to wrap the paste in paragraph tags and it's automatically corrected
        // after adding a newline. But allowing wrapCaretInParagraphIfNecessary() to go through will wrap the paragraph
        // incorrectly, so we skip it in this case.
        //
        // 3. On all APIs, hardware pasting (CTRL + V) doesn't automatically wrap the paste in paragraph tags in
        // new posts. ZSSField.handlePasteEvent() won't fix the wrapping for hardware pastes if
        // wrapCaretInParagraphIfNecessary() goes through first, so we need to skip it in that case.
        // For API < 19, this is fixed implicitly by the 'containsParagraphSeparators' check, but for newer APIs we
        // specifically detect hardware keyboard pastes and skip paragraph wrapping in that case
        // case skip calling wrapCaretInParagraphIfNecessary().
        //
        // Previously, the check was 'if (containsParagraphSeparators)' for all API levels, but this turns out to cause
        // an autocorrect issue for new posts on API 23+:
        // https://github.com/wordpress-mobile/WordPress-Editor-Android/issues/389
        // That issue can be fixed by allowing wrapCaretInParagraphIfNecessary() to go through when adding text to an
        // empty post on those API levels, as long as we exclude the special case of hardware keyboard pasting
        //
        // We're using 'nativeState.androidApiLevel>19' rather than >22 because, even though the bug only appears on
        // 23+ at the time of writing, it's entirely possible a future System WebView or Keyboard update will introduce
        // the bug on 21+.
        var containsParagraphSeparators = this.getWrappedDomNode().innerHTML.search(
                '<' + ZSSEditor.defaultParagraphSeparator) > -1;
        if (containsParagraphSeparators || (nativeState.androidApiLevel > 19 && !isHardwareKeyboardPaste)) {
            this.wrapCaretInParagraphIfNecessary();
        }

        if (wasEnterPressed) {
            // Wrap the existing text in paragraph tags if necessary (this should only be needed if
            // wrapCaretInParagraphIfNecessary() was skipped earlier)
            var currentHtml = this.getWrappedDomNode().innerHTML;
            if (currentHtml.search('<' + ZSSEditor.defaultParagraphSeparator) == -1) {
                ZSSEditor.focusedField.setHTML(Util.wrapHTMLInTag(currentHtml, ZSSEditor.defaultParagraphSeparator));
                ZSSEditor.resetSelectionOnField(this.getWrappedDomNode().id, 1);
            }

            var sel = window.getSelection();
            if (sel.rangeCount < 1) {
                return null;
            }
            var node = $(sel.anchorNode);
            var children = $(sel.anchorNode.childNodes);
            var parentNode = rangy.getSelection().anchorNode.parentNode;

            // If enter was pressed to end a UL or OL, let's double check and handle it accordingly if so
            if (sel.isCollapsed && node.is(NodeName.LI) && (!children.length ||
                    (children.length == 1 && children.first().is(NodeName.BR)))) {
                e.preventDefault();
                if (parentNode && parentNode.nodeName === NodeName.OL) {
                    ZSSEditor.setOrderedList();
                } else if (parentNode && parentNode.nodeName === NodeName.UL) {
                    ZSSEditor.setUnorderedList();
                }
            // Exit blockquote when the user presses Enter inside a blockquote on a new line
            // (main use case is to allow double Enter to exit blockquote)
            } else if (sel.isCollapsed && sel.baseOffset == 0 && parentNode && parentNode.nodeName == 'BLOCKQUOTE') {
                e.preventDefault();
                ZSSEditor.setBlockquote();
            // When pressing enter inside an image caption, clear the caption styling from the new line
            } else if (parentNode.nodeName == NodeName.SPAN && $(parentNode).hasClass('wp-caption')) {
                setTimeout(this.handleCaptionNewLine, 100);
            }
        }
    }
};

ZSSField.prototype.handleInputEvent = function(e) {

    // Skip this if we are composing on an IME keyboard
    if (this.isComposing ) { return; }

    // IMPORTANT: we want the placeholder to come up if there's no text, so we clear the field if
    // there's no real content in it.  It's important to do this here and not on keyDown or keyUp
    // as the field could become empty because of a cut or paste operation as well as a key press.
    // This event takes care of all cases.
    //
    this.emptyFieldIfNoContents();

    var joinedArguments = ZSSEditor.getJoinedFocusedFieldIdAndCaretArguments();
    ZSSEditor.callback('callback-selection-changed', joinedArguments);
    this.callback("callback-input", joinedArguments);
};

ZSSField.prototype.handleCompositionStartEvent = function(e) {
    this.isComposing = true;
};

ZSSField.prototype.handleCompositionEndEvent = function(e) {
    this.isComposing = false;
};

ZSSField.prototype.handleTapEvent = function(e) {
    var targetNode = e.target;

    if (targetNode) {

        ZSSEditor.lastTappedNode = targetNode;

        if (targetNode.nodeName.toLowerCase() == 'a') {
            var arguments = ['url=' + encodeURIComponent(targetNode.href),
                             'title=' + encodeURIComponent(targetNode.innerHTML)];
            var joinedArguments = arguments.join(defaultCallbackSeparator);
            this.callback('callback-link-tap', joinedArguments);
        }

        if (targetNode.nodeName.toLowerCase() == 'img') {
            // If the image is uploading, or is a local image do not select it.
            if ( targetNode.dataset.wpid || targetNode.dataset.video_wpid ) {
                this.sendImageTappedCallback(targetNode);
                return;
            }

            // If we're not currently editing just return. No need to apply styles
            // or acknowledge the tap
            if ( this.wrappedObject.attr('contenteditable') != "true" ) {
                return;
            }

            // Is the tapped image the image we're editing?
            if ( targetNode == ZSSEditor.currentEditingImage ) {
                ZSSEditor.removeImageSelectionFormatting(targetNode);
                this.sendImageTappedCallback(targetNode);
                return;
            }

            // If there is a selected image, deselect it. A different image was tapped.
            if ( ZSSEditor.currentEditingImage ) {
                ZSSEditor.removeImageSelectionFormatting(ZSSEditor.currentEditingImage);
            }

            // Format and flag the image as selected.
            ZSSEditor.currentEditingImage = targetNode;
            var containerNode = ZSSEditor.applyImageSelectionFormatting(targetNode);

            // Move the cursor to the tapped image, to prevent scrolling to the bottom of the document when the
            // keyboard comes up. On API 19 and below does not work properly, with the image sometimes getting removed
            // from the post instead of the edit overlay being displayed
            if (nativeState.androidApiLevel > 19) {
                ZSSEditor.setFocusAfterElement(containerNode);
            }

            return;
        }

        if (targetNode.className.indexOf('edit-overlay') != -1 || targetNode.className.indexOf('edit-content') != -1
            || targetNode.className.indexOf('edit-icon') != -1) {
            ZSSEditor.removeImageSelectionFormatting( ZSSEditor.currentEditingImage );

            this.sendImageTappedCallback( ZSSEditor.currentEditingImage );
            return;
        }

        if (targetNode.className.indexOf('upload-overlay') != -1 ||
            targetNode.className.indexOf('upload-overlay-bg') != -1 ) {
            // Select the image node associated with the selected upload overlay
            var imageNode = targetNode.parentNode.getElementsByTagName('img')[0];

            this.sendImageTappedCallback( imageNode );
            return;
        }

        if (targetNode.className.indexOf('delete-overlay') != -1) {
            var parentEditContainer = targetNode.parentElement;
            var parentDiv = parentEditContainer.parentElement;

            // If the delete button was tapped, removing the media item and its container from the document
            if (parentEditContainer.classList.contains('edit-container')) {
                parentEditContainer.parentElement.removeChild(parentEditContainer);
            } else {
                parentEditContainer.removeChild(targetNode);
            }

            this.emptyFieldIfNoContents();

            ZSSEditor.currentEditingImage = null;
            return;
        }

        if ( ZSSEditor.currentEditingImage ) {
            ZSSEditor.removeImageSelectionFormatting( ZSSEditor.currentEditingImage );
            ZSSEditor.currentEditingImage = null;
        }

        if (targetNode.nodeName.toLowerCase() == 'video') {
        // If the video is uploading, or is a local image do not select it.
            if (targetNode.dataset.wpvideopress) {
                if (targetNode.src.length == 0 || targetNode.src == 'file:///android_asset/') {
                    // If the tapped video is a placeholder for a VideoPress video, send out an update request.
                    // This provides a way to load the video for Android API<19, where the onError property function in
                    // the placeholder video isn't being triggered, and sendVideoPressInfoRequest is never called.
                    // This is also used to manually retry loading a VideoPress video after the onError attribute has
                    // been stripped for the video tag.
                    targetNode.setAttribute("onerror", "");
                    ZSSEditor.sendVideoPressInfoRequest(targetNode.dataset.wpvideopress);
                    return;
                }
            }

            if (targetNode.dataset.wpid) {
                this.sendVideoTappedCallback( targetNode );
                return;
            }
        }
    }
};

ZSSField.prototype.handlePasteEvent = function(e) {
    if (this.isMultiline() && this.getHTML().length == 0) {
        ZSSEditor.insertHTML(Util.wrapHTMLInTag('&#x200b;', ZSSEditor.defaultParagraphSeparator));
    }
};

/**
 *  @brief      Fires after 'keydown' events, when the field contents have already been modified
 */
ZSSField.prototype.afterKeyDownEvent = function(beforeHTML, e) {
    var afterHTML = e.target.innerHTML;
    var htmlWasModified = (beforeHTML != afterHTML);

    var selection = document.getSelection();
    var range = selection.getRangeAt(0).cloneRange();
    var focusedNode = range.startContainer;

    // Correct situation where autocorrect can remove blockquotes at start of document, either when pressing enter
    // inside a blockquote, or pressing backspace immediately after one
    // https://github.com/wordpress-mobile/WordPress-Editor-Android/issues/385
    if (htmlWasModified) {
        var blockquoteMatch = beforeHTML.match('^<blockquote><div>(.*)</div></blockquote>');

        if (blockquoteMatch != null && afterHTML.match('<blockquote>') == null) {
            // Blockquote at start of post was removed
            var newParagraphMatch = afterHTML.match('^<div>(.*?)</div><div><br></div>');

            if (newParagraphMatch != null) {
                // The blockquote was removed in a newline operation
                var originalText = blockquoteMatch[1];
                var newText = newParagraphMatch[1];

                if (originalText != newText) {
                    // Blockquote was removed and its inner text changed - this points to autocorrect removing the
                    // blockquote when changing the text in the previous paragraph, so we replace the blockquote
                    ZSSEditor.turnBlockquoteOnForNode(focusedNode.parentNode.firstChild);
                }
            } else if (afterHTML.match('^<div>(.*?)</div>') != null) {
                // The blockquote was removed in a backspace operation
                ZSSEditor.turnBlockquoteOnForNode(focusedNode.parentNode);
                ZSSEditor.setFocusAfterElement(focusedNode);
            }
        }
    }

    var focusedNodeIsEmpty = (focusedNode.innerHTML != undefined
        && (focusedNode.innerHTML.length == 0 || focusedNode.innerHTML == '<br>'));

    // Blockquote handling
    if (focusedNode.nodeName == NodeName.BLOCKQUOTE && focusedNodeIsEmpty) {
        if (!htmlWasModified) {
            // We only want to handle this if the last character inside a blockquote was just deleted - if the HTML
            // is unchanged, it might be that afterKeyDownEvent was called too soon, and we should avoid doing anything
            return;
        }

        // When using backspace to delete the contents of a blockquote, the div within the blockquote is deleted
        // This makes the blockquote unable to be deleted using backspace, and also causes autocorrect issues on API19+
        range.startContainer.innerHTML = Util.wrapHTMLInTag('<br>', ZSSEditor.defaultParagraphSeparator);

        // Give focus to new div
        var newFocusElement = focusedNode.firstChild;
        ZSSEditor.giveFocusToElement(newFocusElement, 1);
    } else if (focusedNode.nodeName == NodeName.DIV && focusedNode.parentNode.nodeName == NodeName.BLOCKQUOTE) {
        if (focusedNode.parentNode.previousSibling == null && focusedNode.parentNode.childNodes.length == 1
            && focusedNodeIsEmpty) {
            // When a post begins with a blockquote, and there's content after that blockquote, backspacing inside that
            // blockquote will work until the blockquote is empty. After that, backspace will have no effect
            // This fix identifies that situation and makes the call to setBlockquote() to toggle off the blockquote
            ZSSEditor.setBlockquote();
        } else {
            // Remove extraneous break tags sometimes added to blockquotes by autocorrect actions
            // https://github.com/wordpress-mobile/WordPress-Editor-Android/issues/385
            var blockquoteChildNodes = focusedNode.parentNode.childNodes;

            for (var i = 0; i < blockquoteChildNodes.length; i++) {
                var childNode = blockquoteChildNodes[i];
                if (childNode.nodeName == NodeName.BR) {
                    childNode.parentNode.removeChild(childNode);
                }
            }
        }
    }
};

ZSSField.prototype.sendImageTappedCallback = function(imageNode) {
    var meta = JSON.stringify(ZSSEditor.extractImageMeta(imageNode));
    var imageId = "", mediaType = "image";
    if (imageNode.hasAttribute('data-wpid')){
        imageId = imageNode.getAttribute('data-wpid');
    } else if (imageNode.hasAttribute('data-video_wpid')){
        imageId = imageNode.getAttribute('data-video_wpid');
        mediaType = "video";
    }
    var arguments = ['id=' + encodeURIComponent(imageId),
                     'url=' + encodeURIComponent(imageNode.src),
                     'meta=' + encodeURIComponent(meta),
                     'type=' + mediaType];

    var joinedArguments = arguments.join(defaultCallbackSeparator);

    var thisObj = this;

    // WORKAROUND: force the event to become sort of "after-tap" through setTimeout()
    //
    setTimeout(function() { thisObj.callback('callback-image-tap', joinedArguments);}, 500);
}

ZSSField.prototype.sendVideoTappedCallback = function( videoNode ) {
    var videoId = "";
    if ( videoNode.hasAttribute( 'data-wpid' ) ){
        videoId = videoNode.getAttribute( 'data-wpid' )
    }
    var arguments = ['id=' + encodeURIComponent( videoId ),
                     'url=' + encodeURIComponent( videoNode.src )];

    var joinedArguments = arguments.join( defaultCallbackSeparator );

    ZSSEditor.callback('callback-video-tap', joinedArguments);
}

// MARK: - Callback Execution

ZSSField.prototype.callback = function(callbackScheme, callbackPath) {
    var url = callbackScheme + ":";

    url = url + "id=" + this.getNodeId();

    if (callbackPath) {
        url = url + defaultCallbackSeparator + callbackPath;
    }

    if (isUsingiOS) {
        ZSSEditor.callbackThroughIFrame(url);
    } else if (isUsingAndroid) {
        if (nativeState.androidApiLevel < 17) {
            ZSSEditor.callbackThroughIFrame(url);
        } else {
            nativeCallbackHandler.executeCallback(callbackScheme, callbackPath);
        }
    } else {
        console.log(url);
    }
};

// MARK: - Focus

ZSSField.prototype.isFocused = function() {

    return this.wrappedObject.is(':focus');
};

ZSSField.prototype.focus = function() {

    if (!this.isFocused()) {
        this.wrappedObject.focus();
    }
};

ZSSField.prototype.blur = function() {
    if (this.isFocused()) {
        this.wrappedObject.blur();
    }
};

// MARK: - Multiline support

ZSSField.prototype.isMultiline = function() {
    return this.multiline;
};

ZSSField.prototype.setMultiline = function(multiline) {
    this.multiline = multiline;
};

// MARK: - NodeId

ZSSField.prototype.getNodeId = function() {
    return this.wrappedObject.attr('id');
};

// MARK: - Editing

ZSSField.prototype.enableEditing = function () {

    this.wrappedObject.attr('contenteditable', true);

    if (!ZSSEditor.focusedField) {
        ZSSEditor.focusFirstEditableField();
    }
};

ZSSField.prototype.disableEditing = function () {
    // IMPORTANT: we're blurring the field before making it non-editable since that ensures
    // that the iOS keyboard is dismissed through an animation, as opposed to being immediately
    // removed from the screen.
    //
    this.blur();

    this.wrappedObject.attr('contenteditable', false);
};

// MARK: - Caret

/**
 *  @brief      Whenever this method is called, a check will be performed on the caret position
 *              to figure out if it needs to be wrapped in a paragraph node.
 *  @details    A parent paragraph node should be added if the current parent is either the field
 *              node itself, or a blockquote node.
 */
ZSSField.prototype.wrapCaretInParagraphIfNecessary = function() {
    var closerParentNode = ZSSEditor.closerParentNode();

    if (closerParentNode == null) {
        return;
    }

    var parentNodeShouldBeParagraph = (closerParentNode == this.getWrappedDomNode()
                                       || closerParentNode.nodeName == NodeName.BLOCKQUOTE);

    // When starting a post with a blockquote (before any text is entered), the paragraph tags inside the blockquote
    // won't properly wrap the text once it's entered
    // In that case, remove the paragraph tags and re-apply them, wrapping around the newly entered text
    var fixNewPostBlockquoteBug = (closerParentNode.nodeName == NodeName.DIV
            && closerParentNode.parentNode.nodeName == NodeName.BLOCKQUOTE
            && closerParentNode.innerHTML.length == 0);

    // On API 19 and below, identifying the situation where the blockquote bug for new posts occurs works a little
    // differently than above, with the focused node being the parent blockquote rather than the empty div inside it.
    // We still remove the empty div so it can be re-applied correctly to the newly entered text, but we select it
    // differently
    // https://github.com/wordpress-mobile/WordPress-Editor-Android/issues/398
    var fixNewPostBlockquoteBugOldApi = (closerParentNode.nodeName == NodeName.BLOCKQUOTE
            && closerParentNode.parentNode.nodeName == NodeName.DIV
            && closerParentNode.innerHTML == '<div></div>');

    if (parentNodeShouldBeParagraph || fixNewPostBlockquoteBug || fixNewPostBlockquoteBugOldApi) {
        var selection = window.getSelection();

        if (selection && selection.rangeCount > 0) {
            var range = selection.getRangeAt(0);

            if (range.startContainer == range.endContainer) {
                if (fixNewPostBlockquoteBug) {
                    closerParentNode.parentNode.removeChild(closerParentNode);
                } else if (fixNewPostBlockquoteBugOldApi) {
                    closerParentNode.removeChild(closerParentNode.firstChild);
                }

                var paragraph = document.createElement("div");
                var textNode = document.createTextNode("&#x200b;");

                paragraph.appendChild(textNode);

                range.insertNode(paragraph);
                range.selectNode(textNode);

                selection.removeAllRanges();
                selection.addRange(range);
            }
        }
    }
};

/**
 *  @brief      Called when enter is pressed inside an image caption. Clears away the span and label tags the new line
 *              inherits from the caption styling.
 */
ZSSField.prototype.handleCaptionNewLine = function() {
    var selectedNode = document.getSelection().baseNode;

    var contentsNode;
    if (selectedNode.firstChild != null) {
        contentsNode = selectedNode.firstChild.cloneNode();
    } else {
        contentsNode = selectedNode.cloneNode();
    }

    var parentSpan = selectedNode.parentNode.parentNode;
    var parentDiv = parentSpan.parentNode;

    var paragraph = document.createElement("div");
    paragraph.appendChild(contentsNode);

    parentDiv.insertBefore(paragraph, parentSpan);
    parentDiv.removeChild(parentSpan);

    ZSSEditor.giveFocusToElement(contentsNode);
};

// MARK: - i18n

ZSSField.prototype.isRightToLeftTextEnabled = function() {
    var textDir = this.wrappedObject.attr('dir');
    var isRTL = (textDir != "undefined" && textDir == 'rtl');
    return isRTL;
};

ZSSField.prototype.enableRightToLeftText = function(isRTL) {
    var textDirectionString = isRTL ? "rtl" : "ltr";
    this.wrappedObject.attr('dir', textDirectionString);
    this.wrappedObject.css('direction', textDirectionString);
};

// MARK: - HTML contents

ZSSField.prototype.isEmpty = function() {
    var html = this.getHTML();
    var isEmpty = (html.length == 0 || html == "<br>");

    return isEmpty;
};

ZSSField.prototype.getHTML = function() {
    var html = this.wrappedObject.html();
    if (ZSSEditor.defaultParagraphSeparator == 'div') {
        html = Formatter.convertDivToP(html);
    }
    html = Formatter.visualToHtml(html);
    html = ZSSEditor.removeVisualFormatting( html );
    return html;
};

ZSSField.prototype.getHTMLForCallback = function() {
    var functionArgument = "function=getHTMLForCallback";
    var idArgument = "id=" + this.getNodeId();
    var contentsArgument;

    if (this.hasNoStyle) {
        contentsArgument = "contents=" + this.strippedHTML();
    } else {
        var html;
        if (nativeState.androidApiLevel < 17) {
            // URI Encode HTML on API < 17 because of the use of WebViewClient.shouldOverrideUrlLoading. Data must
            // be decoded in shouldOverrideUrlLoading.
            html = encodeURIComponent(this.getHTML());
        } else {
            html = this.getHTML();
        }
        contentsArgument = "contents=" + html;
    }
    var joinedArguments = functionArgument + defaultCallbackSeparator + idArgument + defaultCallbackSeparator +
        contentsArgument;
    ZSSEditor.callback('callback-response-string', joinedArguments);
};

ZSSField.prototype.strippedHTML = function() {
    return this.wrappedObject.text();
};

ZSSField.prototype.setPlainText = function(text) {
    ZSSEditor.currentEditingImage = null;
    this.wrappedObject.text(text);
};

ZSSField.prototype.setHTML = function(html) {
    ZSSEditor.currentEditingImage = null;
    var mutatedHTML = Formatter.htmlToVisual(html);

    if (ZSSEditor.defaultParagraphSeparator == 'div') {
        mutatedHTML = Formatter.convertPToDiv(mutatedHTML);
    }

    this.wrappedObject.html(mutatedHTML);

    // Track video container nodes for mutation
    var videoNodes = $('span.edit-container > video');
    for (var i = 0; i < videoNodes.length; i++) {
        ZSSEditor.trackNodeForMutation($(videoNodes[i].parentNode));
    }
};

// MARK: - Placeholder

ZSSField.prototype.hasPlaceholderText = function() {
    return this.wrappedObject.attr('placeholderText') != null;
};

ZSSField.prototype.setPlaceholderText = function(placeholder) {
    this.wrappedObject.attr('placeholderText', placeholder);
};

// MARK: - Wrapped Object

ZSSField.prototype.getWrappedDomNode = function() {
    return this.wrappedObject[0];
};
