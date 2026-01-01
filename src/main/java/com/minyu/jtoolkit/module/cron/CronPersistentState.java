package com.minyu.jtoolkit.module.cron;

import com.minyu.jtoolkit.core.model.PersistentState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CronPersistentState implements PersistentState {

    private String lastExpression;
}