package com.aar.common.state;

/** Một dòng trong bảng chuyển trạng thái. from = null nghĩa là đối tượng mới được tạo. */
public record Transition(EntityType entity, String from, String event, String to) {
}
