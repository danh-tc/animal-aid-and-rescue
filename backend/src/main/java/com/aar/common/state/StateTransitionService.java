package com.aar.common.state;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Điểm duy nhất được đổi trạng thái (RB-12). Service nghiệp vụ gọi apply(), nhận trạng thái đích,
 * gán vào entity trong cùng giao dịch. Mỗi lần đổi đều ghi status_histories (NFR-03).
 *
 * <p>Đối tượng mới tạo: lấy trạng thái ban đầu bằng initialStatus(), lưu entity để có id,
 * rồi gọi apply() với from = null để ghi lịch sử.</p>
 */
@Service
public class StateTransitionService {

    private final StatusHistoryRepository histories;

    public StateTransitionService(StatusHistoryRepository histories) {
        this.histories = histories;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String apply(EntityType entity, Long entityId, String from, String event, Long changedBy, String note) {
        String to = TransitionTable.require(entity, from, event);
        histories.save(new StatusHistory(entity, entityId, from, to, event, note, changedBy));
        return to;
    }

    /** Trạng thái ban đầu khi tạo đối tượng bằng sự kiện event. */
    public String initialStatus(EntityType entity, String event) {
        return TransitionTable.require(entity, null, event);
    }
}
