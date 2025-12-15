package com.minyu.jtoolkit.module.cron;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CronViewState {

    private String lastExpression;
}