package com.panda.mapper;

import com.panda.entity.ScooterCommand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ScooterCommandMapper {

    int insert(ScooterCommand scooterCommand);

    int markSent(@Param("commandId") String commandId,
                 @Param("sendTime") LocalDateTime sendTime);

    int markFailed(@Param("commandId") String commandId,
                   @Param("errorMessage") String errorMessage);

    int markAcked(@Param("commandId") String commandId,
                  @Param("ackTime") LocalDateTime ackTime);

    int markAckFailed(@Param("commandId") String commandId,
                      @Param("ackTime") LocalDateTime ackTime,
                      @Param("errorMessage") String errorMessage);

    ScooterCommand getByCommandId(@Param("commandId") String commandId);

    List<ScooterCommand> listRetryableTimeoutCommands(@Param("deadline") LocalDateTime deadline,
                                                      @Param("limit") Integer limit);

    List<ScooterCommand> listExhaustedTimeoutCommands(@Param("deadline") LocalDateTime deadline,
                                                      @Param("limit") Integer limit);

    int markRetrySent(@Param("commandId") String commandId,
                      @Param("sendTime") LocalDateTime sendTime);

    int markRetryError(@Param("commandId") String commandId,
                       @Param("errorMessage") String errorMessage);

    int markTimeoutCommands(@Param("deadline") LocalDateTime deadline,
                            @Param("errorMessage") String errorMessage);

    int markTimeoutByCommandId(@Param("commandId") String commandId,
                               @Param("errorMessage") String errorMessage);
}
