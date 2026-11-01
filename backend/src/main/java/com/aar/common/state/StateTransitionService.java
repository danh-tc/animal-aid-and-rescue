package com.aar.common.state;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Điểm duy nhất được đổi trạng thái (RB-12). Service nghiệp vụ gọi apply(), nhận trạng thái đích,
 * gán vào entity trong cùng giao dịch. Mỗi lần đổi đều ghi status_histories (NFR-03).
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
}
