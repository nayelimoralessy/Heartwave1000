package com.example.heartwave;

public class MessageEvent {
    public enum File {
        MAIN,
        SERVICE,
        FRAGMENT_SCAN,
        FRAGMENT_ECG,
        FRAGMENT_STATS,
    }
    public enum Action {
        SCAN,
            
    };
    public final String message; // Change to data
    public final File sender;
    public final File receiver;
    public final Action action;

    public MessageEvent(String message, File sender, File receiver, Action action) {
        this.message    = message;
        this.sender     = sender;
        this.receiver   = receiver;
        this.action     = action;
    }
}
