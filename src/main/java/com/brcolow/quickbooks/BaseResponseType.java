package com.brcolow.quickbooks;

import java.math.BigInteger;
import java.util.Locale;

/**
 * Defines abstract methods that every "*RsType" in Quickbooks XML specification supports.
 */
public abstract class BaseResponseType {
    public abstract String getRequestID();
    public abstract void setRequestID(String value);

    public abstract BigInteger getStatusCode();
    public abstract void setStatusCode(BigInteger value);

    public StatusSeverity getStatusSeverityEnum() {
        return StatusSeverity.valueOf(getStatusSeverity().toUpperCase(Locale.US));
    }

    public abstract String getStatusSeverity();
    public abstract void setStatusSeverity(String value);

    public abstract String getStatusMessage();
    public abstract void setStatusMessage(String value);
}
