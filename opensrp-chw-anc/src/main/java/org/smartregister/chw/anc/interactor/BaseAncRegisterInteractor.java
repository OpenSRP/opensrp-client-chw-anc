package org.smartregister.chw.anc.interactor;

import android.support.annotation.VisibleForTesting;

import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.contract.BaseAncRegisterContract;
import org.smartregister.chw.anc.util.AppExecutors;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.anc.util.Util;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.repository.AllSharedPreferences;

import timber.log.Timber;

public class BaseAncRegisterInteractor implements BaseAncRegisterContract.Interactor {

    private AppExecutors appExecutors;

    @VisibleForTesting
    BaseAncRegisterInteractor(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
    }

    public BaseAncRegisterInteractor() {
        this(new AppExecutors());
    }

    @Override
    public void onDestroy(boolean isChangingConfiguration) {
        Timber.v("Empty onDestroy");
    }

    @Override
    public void saveRegistration(final String jsonString, final boolean isEditMode, final BaseAncRegisterContract.InteractorCallBack callBack, final String table) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // save it
                try {
                    saveRegistration(jsonString, table);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                appExecutors.mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onRegistrationSaved(isEditMode);
                    }
                });
            }
        };
        appExecutors.diskIO().execute(runnable);
    }

    private void saveRegistration(final String jsonString, String table) throws Exception {

        AllSharedPreferences allSharedPreferences = AncLibrary.getInstance().context().allSharedPreferences();
        Event baseEvent = JsonFormUtils.processJsonForm(allSharedPreferences, jsonString, table);

        Util.processEvent(allSharedPreferences, baseEvent);
    }
}
