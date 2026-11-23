package com.overex.backend.event.dto.responses;

public record StatusOption(boolean disabled, String why) {

    public static StatusOption allowed() {
        return new StatusOption(false, null);
    }

    public static StatusOption blocked(String why) {
        return new StatusOption(true, why);
    }

}
