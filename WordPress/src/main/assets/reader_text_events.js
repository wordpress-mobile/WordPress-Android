/*
 * wvHandler is the name of the message handler in the webview, added via createJsObject inside
 * ReaderPostRenderer. It handles the `postMessage` calls from the WebView.
 */

function debounce(fn, timeout) {
    let timer;
    return () => {
        clearTimeout(timer);
        timer = setTimeout(fn, timeout);
    }
}

const textHighlighted = debounce(
    () => wvHandler.postMessage("articleTextHighlighted"),
    1000
);

document.addEventListener('selectionchange', function(event) {
    const selection = document.getSelection().toString();
    if (selection.length > 0) {
        textHighlighted();
    }
});

document.addEventListener('copy', event => wvHandler.postMessage("articleTextCopied"));
