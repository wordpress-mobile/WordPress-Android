( function () {

    function init() {
        loadDocsBotAI()
    }

    function loadDocsBotAI() {
        const urlParams = getQueryParams(window.location);

        window.DocsBotAI.init({
            id: urlParams.id,
            supportCallback: function(event, history) {
                event.preventDefault() // Optionally prevent default behavior opening the url.
                console.log(history) // Safely access the chat history.
                sendAndroidMessage(history)
            },
            options: {
                botName: "Jetpack Mobile",
                description: "Ask me anything about using Jetpack mobile app.",
                color: "#9dd977",
                supportLink: "#",
                labels: {
                    inputPlaceholder: decodeURIComponent(urlParams.inputPlaceholder),
                    firstMessage: decodeURIComponent(urlParams.firstMessage),
                    sources: "Sources",
                    helpful: "Rate as helpful",
                    unhelpful: "Rate as unhelpful",
                    getSupport: decodeURIComponent(urlParams.getSupport),
                    floatingButton: "Help",
                    suggestions: decodeURIComponent(urlParams.suggestions),
                    thinking: "Thinking..."
                }, // Override all the default labels for your own language.
                questions: [
                    decodeURIComponent(urlParams.questionOne),
                    decodeURIComponent(urlParams.questionTwo),
                    decodeURIComponent(urlParams.questionThree)
                ] // Array of example questions to show in the widget. Three are picked at random.
            },
        }).then(() => {
            // Safely do stuff here after the widget is loaded.
            let timerId = setInterval(() => {
                openDocsBot(); // wait for init
            }, 200);

            setTimeout(() => {
                clearInterval(timerId)

                hideTopCloseButton(); // hide after init
                hideTopHeader();
                resetConversation();
            }, 300);
        });
    }

    function sendAndroidMessage(history) {
        /* In this implementation, only the single-arg version of postMessage is supported. As noted
         * in the WebViewCompat reference doc, the second parameter, MessagePorts, is optional.
         * Also note that onmessage, addEventListener and removeEventListener are not supported.
         */
        console.log(history.toString());
        jsObject.postMessage(history.toString());
    }

    function getQueryParams(location) {
        return location.search ?
            location.search.substr(1).split`&`.reduce((qd, item) => {
                let [k, v] = item.split`=`;
                v = v && decodeURIComponent(v);
                (qd[k] = qd[k] || []).push(v);
                return qd
            }, {}) :
            {}
    }

    function openDocsBot() {
        const widget = document.querySelector("#docsbotai-root").shadowRoot.querySelector("a.floating-button");
        if (widget && widget !== null && typeof widget !== 'undefined') {
            widget.click();
        }

        if ( 'function' === typeof window.DocsBotAI ) {
            DocsBotAI.open();
        }
    }

    function hideTopCloseButton() {
        const closeButton = document.querySelector("#docsbotai-root").shadowRoot.querySelector("div > div > div > a");
        if (closeButton && closeButton !== null && closeButton !== 'undefined') {
            closeButton.style.display = 'none';
        }
    }

    function hideTopHeader() {
        const header = document.querySelector("#docsbotai-root").shadowRoot.querySelector("div > div > div > div.docsbot-chat-header");
        if (header && header !== null && header !== 'undefined') {
            header.style.display = 'none';
        }
    }

    function resetConversation() {
        // reset button was in top header
        localStorage.removeItem("docsbot_chat_history");
    }


    document.addEventListener( 'DOMContentLoaded', init );
} ) ();

