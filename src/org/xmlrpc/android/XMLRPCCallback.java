package org.xmlrpc.android;

/**
 * The XMLRPCCallback interface must be implemented by a listener for an
 * asynchronous call to a server method.
 * When the server responds, the corresponding method on the listener is called.
 *
 * @author Tim Roes
 */
public interface XMLRPCCallback {

	/**
	 * This callback is called whenever the server successfully responds.
	 *
	 * @param id The id as returned by the XMLRPCClient.asyncCall(..) method for this request.
	 * @param result The Object returned from the server.
	 */
	public void onSuccess(long id, Object result);

	/**
	 * This callback is called whenever an error occurs during the method call.
	 *
	 * @param id The id as returned by the XMLRPCClient.asyncCall(..) method for this request.
	 * @param error The error occured.
	 */
	public void onFailure(long id, XMLRPCException error);

}
