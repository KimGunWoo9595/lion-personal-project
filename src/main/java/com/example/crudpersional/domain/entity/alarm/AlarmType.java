
package com.example.crudpersional.domain.entity.alarm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AlarmType {
    NEW_COMMENT_ON_POST("new comment!"),
    NEW_LIKEA_ON_POST("new like!"),
    ;

    private final String alarmText;
}
