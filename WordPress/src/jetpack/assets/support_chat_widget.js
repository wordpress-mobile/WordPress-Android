( function () {

    function init() {
        loadDocsBotAI()
    }

    function loadDocsBotAI() {
        const urlParams = getQueryParams(window.location);

        window.DocsBotAI.init({
            id: urlParams.id,
            identify: {
                chatId: urlParams.chatId // This will be recorded in the question metadata and accessible via the API.
            },
            supportCallback: function(event, history) {
                event.preventDefault() // Optionally prevent default behavior opening the url.
                console.log(history) // Safely access the chat history.
                sendAndroidMessage(JSON.stringify(history))
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
//                    decodeURIComponent(urlParams.questionTwo),
                    decodeURIComponent(urlParams.questionThree),
//                    decodeURIComponent(urlParams.questionFour),
                    decodeURIComponent(urlParams.questionFive),
//                    decodeURIComponent(urlParams.questionSix)
                ] // Array of example questions to show in the widget. Three are picked at random.
            },
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

    document.addEventListener( 'DOMContentLoaded', init );
} ) ();

