package com.mygym.app.facialrecognition.web;

public class DownstreamUnavailableException extends RuntimeException {

    private final String downstream;

    public DownstreamUnavailableException(String downstream, Throwable cause) {
        super(downstream + " is unavailable", cause);
        this.downstream = downstream;
    }

    public String getDownstream() {
        return downstream;
    }
}
