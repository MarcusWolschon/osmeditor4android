package io.vespucci;

import java.util.List;
import java.util.UUID;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import com.google.common.util.concurrent.ListenableFuture;

import android.app.PendingIntent;
import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;
import androidx.work.WorkRequest;
import kotlinx.coroutines.flow.Flow;

@Implements(WorkManager.class)
public class ShadowWorkManager {
    /**
     * Returns a dummy WorkManager
     * 
     * @param context an Android Context
     * @return a dummy WorkManager
     */
    @Implementation
    public static WorkManager getInstance(Context context) {
        return new WorkManagerImpl();
    }

    public static class WorkManagerImpl extends WorkManager {
        @Override
        public Operation cancelAllWorkByTag(String tag) {
            return null;
        }

        @Override
        public WorkContinuation beginUniqueWork(String arg0, ExistingWorkPolicy arg1, List<OneTimeWorkRequest> arg2) {
            return null;
        }

        @Override
        public WorkContinuation beginWith(List<OneTimeWorkRequest> arg0) {
            return null;
        }

        @Override
        public Operation cancelAllWork() {
            return null;
        }

        @Override
        public Operation cancelUniqueWork(String arg0) {
            return null;
        }

        @Override
        public Operation cancelWorkById(UUID arg0) {
            return null;
        }

        @Override
        public PendingIntent createCancelPendingIntent(UUID arg0) {
            return null;
        }

        @Override
        public Operation enqueue(List<? extends WorkRequest> arg0) {
            return null;
        }

        @Override
        public Operation enqueueUniquePeriodicWork(String arg0, ExistingPeriodicWorkPolicy arg1, PeriodicWorkRequest arg2) {
            return null;
        }

        @Override
        public Operation enqueueUniqueWork(String arg0, ExistingWorkPolicy arg1, List<OneTimeWorkRequest> arg2) {
            return null;
        }
        
        public Configuration getConfiguration() {
            return null;
        }

        @Override
        public ListenableFuture<Long> getLastCancelAllTimeMillis() {
            return null;
        }

        @Override
        public LiveData<Long> getLastCancelAllTimeMillisLiveData() {
            return null;
        }

        @Override
        public ListenableFuture<WorkInfo> getWorkInfoById(UUID arg0) {
            return null;
        }

        @Override
        public LiveData<WorkInfo> getWorkInfoByIdLiveData(UUID arg0) {
            return null;
        }

        @Override
        public ListenableFuture<List<WorkInfo>> getWorkInfos(WorkQuery arg0) {
            return null;
        }

        @Override
        public ListenableFuture<List<WorkInfo>> getWorkInfosByTag(String arg0) {
            return null;
        }

        @Override
        public LiveData<List<WorkInfo>> getWorkInfosByTagLiveData(String arg0) {
            return null;
        }

        @Override
        public Flow<List<WorkInfo>> getWorkInfosFlow(WorkQuery workQuery) {
            return null;
        }
        
        @Override
        public ListenableFuture<List<WorkInfo>> getWorkInfosForUniqueWork(String arg0) {
            return null;
        }
        
        @Override
        public Flow<List<WorkInfo>> getWorkInfosForUniqueWorkFlow(String uniqueWorkName) {
            return null;
        }
        
        @Override
        public LiveData<List<WorkInfo>> getWorkInfosForUniqueWorkLiveData(String arg0) {
            return null;
        }

        @Override
        public LiveData<List<WorkInfo>> getWorkInfosLiveData(WorkQuery arg0) {
            return null;
        }

        @Override
        public Operation pruneWork() {
            return null;
        }
        
        public ListenableFuture<WorkManager.UpdateResult> updateWork(WorkRequest request) {
            return null;
        }

        @Override
        public Flow<WorkInfo> getWorkInfoByIdFlow(UUID arg0) {
            return null;
        }

        @Override
        public Flow<List<WorkInfo>> getWorkInfosByTagFlow(String arg0) {
            return null;
        }
    }
}
