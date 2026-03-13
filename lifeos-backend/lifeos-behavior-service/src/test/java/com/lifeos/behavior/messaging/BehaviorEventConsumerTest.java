package com.lifeos.behavior.messaging;

import com.alibaba.fastjson2.JSON;
import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.behavior.service.BehaviorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BehaviorEventConsumerTest {

    @Mock
    private BehaviorService behaviorService;

    @InjectMocks
    private BehaviorEventConsumer behaviorEventConsumer;

    @Test
    void onMessageDelegatesToBehaviorService() {
        BehaviorEventCommand command = new BehaviorEventCommand();
        command.setEventId("event-201");
        command.setUserId(5L);
        command.setActionType("CREATE_NOTE");
        command.setTargetId(18L);

        behaviorEventConsumer.onMessage(JSON.toJSONString(command));

        ArgumentCaptor<BehaviorEventCommand> commandCaptor = ArgumentCaptor.forClass(BehaviorEventCommand.class);
        verify(behaviorService).recordEvent(commandCaptor.capture());
        assertThat(commandCaptor.getValue().getEventId()).isEqualTo("event-201");
        assertThat(commandCaptor.getValue().getUserId()).isEqualTo(5L);
        assertThat(commandCaptor.getValue().getActionType()).isEqualTo("CREATE_NOTE");
        assertThat(commandCaptor.getValue().getTargetId()).isEqualTo(18L);
    }
}
