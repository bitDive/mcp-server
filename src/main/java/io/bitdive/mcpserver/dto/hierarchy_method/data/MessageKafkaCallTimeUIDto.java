package io.bitdive.mcpserver.dto.hierarchy_method.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MessageKafkaCallTimeUIDto {
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;
    private long deltaCall;
}
