package org.wordpress.android.fluxc.network.xmlrpc;

public class XMLRPCFault extends XMLRPCException {
    private static final long serialVersionUID = 5676562456612956519L;
    private String mFaultString;
    private int mFaultCode;

    public XMLRPCFault(String faultString, int faultCode) {
        super(faultString);
        this.mFaultString = faultString;
        this.mFaultCode = faultCode;
    }

    public String getFaultString() {
        return mFaultString;
    }

    public int getFaultCode() {
        return mFaultCode;
    }
}
