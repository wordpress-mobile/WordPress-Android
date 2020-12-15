if ( window.wp.data !== undefined ) {
	const nuxStore = window.wp.data.dispatch( 'automattic/nux' );
	if ( nuxStore ) {
		nuxStore.setWpcomNuxStatus( { isNuxEnabled: false } );
	}
}

// We need to return a string or null, otherwise executing this script will error.
// eslint-disable-next-line no-unused-expressions
( '' );
