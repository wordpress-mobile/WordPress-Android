;(window.DocsBotAI = window.DocsBotAI || {}),
    (DocsBotAI.init = function (c) {
      return new Promise(function (e, o) {
        var t = document.createElement('script')
        ;(t.type = 'text/javascript'), (t.async = !0), (t.src = 'https://widget.docsbot.ai/chat.js')
        var n = document.getElementsByTagName('script')[0]
        n.parentNode.insertBefore(t, n),
          t.addEventListener('load', function () {
            window.DocsBotAI.mount({
              id: c.id,
              supportCallback: c.supportCallback,
              identify: c.identify,
              options: c.options,
            })
            var t
            ;(t = function (n) {
              return new Promise(function (e) {
                if (document.querySelector(n)) return e(document.querySelector(n))
                var o = new MutationObserver(function (t) {
                  document.querySelector(n) && (e(document.querySelector(n)), o.disconnect())
                })
                o.observe(document.body, { childList: !0, subtree: !0 })
              })
            }),
              t && t('#docsbotai-root').then(e).catch(o)
          }),
          t.addEventListener('error', function (t) {
            o(t.message)
          })
      })
    })

    function sendAndroidMessage(history) {
        /* In this implementation, only the single-arg version of postMessage is supported. As noted
         * in the WebViewCompat reference doc, the second parameter, MessagePorts, is optional.
         * Also note that onmessage, addEventListener and removeEventListener are not supported.
         */
        jsObject.postMessage(history);
    }

    function getQueryParams(location) {
      return location.search
        ? location.search.substr(1).split`&`.reduce((qd, item) => {let [k,v] = item.split`=`; v = v && decodeURIComponent(v); (qd[k] = qd[k] || []).push(v); return qd}, {})
        : {}
    }

    function hideTopCloseButton() {
        document
            .querySelector("#docsbotai-root")
            .shadowRoot.querySelector("div > div > div > a")
            .style.visibility = 'hidden';
//            .style.display = 'none';
    }

    function hideTopHeader() {
        document
            .querySelector("#docsbotai-root")
            .shadowRoot.querySelector("div > div > div > div.docsbot-chat-header")
            .style.visibility = 'hidden';
//            .style.display = 'none';
    }
