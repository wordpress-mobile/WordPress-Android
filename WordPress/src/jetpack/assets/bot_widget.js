window.DocsBotAI = window.DocsBotAI || {};

DocsBotAI.init = function(c) {
    return new Promise(function(resolve, reject) {
        const chatScript = document.createElement("script");
        chatScript.type = "text/javascript";
        chatScript.async = true;
        chatScript.src = "https://widget.docsbot.ai/chat.js";

        const firstScript = document.getElementsByTagName("script")[0];
        firstScript.parentNode.insertBefore(chatScript, firstScript);

        chatScript.addEventListener("load", function() {
            window.DocsBotAI.mount({
                id: c.id,
                supportCallback: c.supportCallback,
                identify: c.identify,
                options: c.options,
                signature: c.signature
            });

            const waitForShadowRoot = function(selector) {
                return new Promise(function(resolve) {
                    const elementCheck = function(mutationsList, observer) {
                        if (document.querySelector(selector)) {
                            const shadowRoot = document.querySelector(selector).shadowRoot;
                            if (shadowRoot) {
                                resolve(shadowRoot);
                                observer.disconnect();
                            }
                        }
                    };

                    const observer = new MutationObserver(elementCheck);
                    observer.observe(document.body, { childList: true, subtree: true });
                });
            };

            waitForShadowRoot("#docsbotai-root")
                .then(function(shadowRoot) {
                    // Open DocsBotAI after shadowRoot is loaded
                    window.DocsBotAI.open();

                    resolve(shadowRoot);
                })
                .catch(reject);
        });

        chatScript.addEventListener("error", function(error) {
            reject(error.message);
        });
    });
};


