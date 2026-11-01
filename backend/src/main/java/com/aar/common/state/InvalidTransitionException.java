package com.aar.common.state;

public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(EntityType entity, String from, String event) {
        super("Không thể thực hiện " + event + " khi " + entity + " đang ở trạng thái " + (from == null ? "(mới)" : from));
    }
}
