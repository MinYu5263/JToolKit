package com.minyu.jtoolkit.module.cron;

import com.minyu.jtoolkit.core.model.ViewState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CronViewState implements ViewState {

    private String lastExpression;
}