package org.wordpress.android.stores.network.xmlrpc;

public class XMLRPCFault extends XMLRPCException {
    private static final long serialVersionUID = 5676562456612956519L;
    private String faultString;
    private int faultCode;

    public XMLRPCFault(String faultString, int faultCode) {
        super(faultString);
        this.faultString = faultString;
        this.faultCode = faultCode;
    }

    public String getFaultString() {
        return faultString;
    }

    public int getFaultCode() {
        return faultCode;
    }

    public String getMessage() {
        return super.getMessage() + " [Code: " + this.faultCode + "]";
    }
}
