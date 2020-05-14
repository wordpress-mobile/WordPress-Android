window.blockEditorSelect = window.wp.data.select( 'core/block-editor' ) ||
    window.wp.data.select( 'core/editor' );

window.blockEditorDispatch = window.wp.data.dispatch( 'core/block-editor' ) ||
    window.wp.data.dispatch( 'core/editor' );

window.getHTMLPostContent = () => {
	const blocks = window.blockEditorSelect.getBlocks();
	const HTML = window.wp.blocks.serialize( blocks );
	if (window.webkit) {
    	    window.webkit.messageHandlers.htmlPostContent.postMessage( HTML );
    	} else {
    	    window.wpwebkit.postMessage( HTML );
    	}
};

window.insertBlock = ( blockHTML ) => {
	const post = window.wp.data.select( 'core/editor' ).getCurrentPost();
	window.wp.data.dispatch( 'core/editor' ).setupEditor( post, { content: blockHTML } );

	const clientId = window.blockEditorSelect.getBlocks()[ 0 ].clientId;
	window.blockEditorDispatch.selectBlock( clientId );
};