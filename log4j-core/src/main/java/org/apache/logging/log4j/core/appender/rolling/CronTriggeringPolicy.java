/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Scheduled;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.CronExpression;
import org.apache.logging.log4j.status.StatusLogger;

import java.text.ParseException;

/**
 * Rolls a file over based on a cron schedule.
 */
@Plugin(name = "CronTriggeringPolicy", category = "Core", printObject = true)
@Scheduled
public final class CronTriggeringPolicy implements TriggeringPolicy {

    private static Logger LOGGER = StatusLogger.getLogger();
    private static final String defaultSchedule = "0 0 0 * * ?";
    private RollingFileManager manager;
    private final CronExpression cronExpression;
    private final Configuration configuration;

    private CronTriggeringPolicy(CronExpression schedule, Configuration configuration) {
        this.cronExpression = schedule;
        this.configuration = configuration;
    }

    /**
     * Initializes the policy.
     * @param aManager The RollingFileManager.
     */
    @Override
    public void initialize(final RollingFileManager aManager) {
        this.manager = aManager;
        configuration.getScheduler().scheduleWithCron(cronExpression, new CronTrigger());
    }

    /**
     * Determines whether a rollover should occur.
     * @param event   A reference to the currently event.
     * @return true if a rollover should occur.
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent event) {
        return false;
    }

    public CronExpression getCronExpression() {
        return cronExpression;
    }

    /**
     * Creates a ScheduledTriggeringPolicy.
     * @param configuration the Configuration.
     * @param schedule the cron expression.
     * @return a ScheduledTriggeringPolicy.
     */
    @PluginFactory
    public static CronTriggeringPolicy createPolicy(
            @PluginConfiguration Configuration configuration,
            @PluginAttribute("schedule") final String schedule) {
        CronExpression cronExpression;
        if (schedule == null) {
            LOGGER.info("No schedule specified, defaulting to Daily");
            cronExpression = getSchedule(defaultSchedule);
        } else {
            cronExpression = getSchedule(schedule);
            if (cronExpression == null) {
                LOGGER.error("Invalid expression specified. Defaulting to Daily");
                cronExpression = getSchedule(defaultSchedule);
            }
        }
        return new CronTriggeringPolicy(cronExpression, configuration);
    }

    private static CronExpression getSchedule(String expression) {
        try {
            return new CronExpression(expression);
        } catch (ParseException pe) {
            LOGGER.error("Invalid cron expression - " + expression, pe);
            return null;
        }
    }

    @Override
    public String toString() {
        return "CronTriggeringPolicy(schedule=" + cronExpression.getCronExpression() + ")";
    }

    private class CronTrigger implements Runnable {

        public void run() {
            manager.rollover();
        }
    }

}