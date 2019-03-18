package org.wxl.threadpool;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class TaskPools extends ThreadPoolExecutor {
//    ScheduledThreadPoolExecutor

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskPools.class);
    private static volatile TaskPools  threadPool = null;
    /**
     * 记录运行中任务
     */
    private static final LinkedBlockingQueue<Runnable> workBlockingQueue=new  LinkedBlockingQueue<Runnable>();

    public static TaskPools getTaskPoolsImpl(){
        return TaskPools.getTaskPoolsImpl(6, 12,60L,TimeUnit.SECONDS);
    }
    public static TaskPools getTaskPoolsImpl(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit){
        if(null == TaskPools.threadPool) {
            synchronized(TaskPools.class){
                if(null == TaskPools.threadPool){
                    TaskPools.threadPool= new TaskPools(corePoolSize, maximumPoolSize,keepAliveTime,unit);
                }
            }
        }
        return TaskPools.threadPool;
    }


    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t,r);
//        workBlockingQueue.add(r);
        LOGGER.info("保存在运行的任务");
    }
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
//        workBlockingQueue.remove(r);
        LOGGER.info("移除关闭的任务");
    }
    /**
     *
     * Description: 正在运行的任务
     */
    public LinkedBlockingQueue<Runnable> getWorkBlockingQueue() {
        return workBlockingQueue;
    }


    private TaskPools(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workBlockingQueue);
    }


}
