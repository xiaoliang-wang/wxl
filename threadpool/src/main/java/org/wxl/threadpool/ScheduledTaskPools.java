package org.wxl.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class ScheduledTaskPools {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskPools.class);

    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4);

    public static boolean punctualTask(Runnable command,long initialDelay,long period){
        if(null == command){
            LOGGER.error("传入的command为空");
            return false;
        }
        if(period < 1){
            LOGGER.error("传入的执行间隔错误");
            return false;
        }
        if(initialDelay < 0){
            initialDelay = 0;
        }
        scheduledExecutorService.scheduleAtFixedRate(command, initialDelay, period, TimeUnit.MILLISECONDS);
        return true;
    }
    public static boolean unpunctualTask(Runnable command,long initialDelay,long period){

        return ScheduledTaskPools.punctualTask(new Runnable() {
            @Override
            public void run() {
                TaskPools.getTaskPoolsImpl().execute(command);
            }
        }, initialDelay, period);

    }




}
