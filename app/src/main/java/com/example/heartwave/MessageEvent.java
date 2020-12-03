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
        SAMPLE_RATE,
        ADC,
        BUTTON_SCAN,
        BUTTON_CONNECT,
        BUTTON_DISCONNECT,
    };
    public final String data; // Change to data
    public final File sender;
    public final File receiver;
    public final Action action;

    public MessageEvent(String data, File sender, File receiver, Action action) {
        this.data       = data;
        this.sender     = sender;
        this.receiver   = receiver;
        this.action     = action;
    }
}
