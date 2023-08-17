window.DocsBotAI = window.DocsBotAI || {};

DocsBotAI.init = function (c) {
    return new Promise(function (resolve, reject) {
        const chatScript = document.createElement("script");
        chatScript.type = "text/javascript";
        chatScript.async = true;
        chatScript.src = "https://widget.docsbot.ai/chat.js";

        const firstScript = document.getElementsByTagName("script")[0];
        firstScript.parentNode.insertBefore(chatScript, firstScript);

        chatScript.addEventListener("load", function () {
            window.DocsBotAI.mount({
                id: c.id,
                supportCallback: c.supportCallback,
                identify: c.identify,
                options: c.options,
                signature: c.signature
            });

            const waitForElementAndShadowRoot = function (selector) {
                return new Promise(function (resolve) {
                    const elementCheck = function (mutationsList, observer) {
                        const shadowRoot = document.querySelector(selector)?.shadowRoot;
                        if (shadowRoot) {
                            // reset chat history
                            localStorage.removeItem("docsbot_chat_history");

                            // Open DocsBotAI after shadowRoot is loaded
                            window.DocsBotAI.open();

                            const linkElem = document.createElement("link");
                            linkElem.setAttribute("rel", "stylesheet");
                            linkElem.setAttribute("href", "/assets/support_chat_widget.css");
                            shadowRoot.appendChild(linkElem);
                        }
                    };

                    const observer = new MutationObserver(elementCheck);
                    observer.observe(document.body, { childList: true, subtree: true });
                });
            };

            waitForElementAndShadowRoot("#docsbotai-root").catch(reject);
        });

        chatScript.addEventListener("error", function (error) {
            reject(error.message);
        });
    });
};
