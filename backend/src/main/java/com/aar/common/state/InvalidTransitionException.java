package com.aar.common.state;

public class InvalidTransitionException extends RuntimeException {

    private final String detail;

    public InvalidTransitionException(EntityType entity, String from, String event) {
        super("Không thực hiện được thao tác này ở trạng thái hiện tại, vui lòng tải lại trang");
        this.detail = entity + " " + (from == null ? "(mới)" : from) + " => " + event;
    }

    public String detail() {
        return detail;
    }
}
