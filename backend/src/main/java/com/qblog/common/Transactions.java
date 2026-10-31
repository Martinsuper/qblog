package com.qblog.common;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务辅助工具
 */
public final class Transactions {

    private Transactions() {
    }

    /**
     * 在当前事务提交后执行回调；无活动事务时立即执行。
     * 用于缓存失效：必须等新数据对其他事务真正可见之后再失效，否则在事务提交前
     * 失效会给并发的缓存回源留出"读到旧数据、提交后回填"的窗口。
     */
    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
