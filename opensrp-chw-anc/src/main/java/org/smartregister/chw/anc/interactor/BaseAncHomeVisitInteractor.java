package org.smartregister.chw.anc.interactor;

import android.support.annotation.VisibleForTesting;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.actionhelper.DangerSignsHelper;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.repository.VisitRepository;
import org.smartregister.chw.anc.util.AppExecutors;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.repository.AllSharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class BaseAncHomeVisitInteractor implements BaseAncHomeVisitContract.Interactor {

    protected AppExecutors appExecutors;

    @VisibleForTesting
    BaseAncHomeVisitInteractor(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
    }

    public BaseAncHomeVisitInteractor() {
        this(new AppExecutors());
    }

    @Override
    public void reloadMemberDetails(String memberID, BaseAncHomeVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            MemberObject memberObject = getMemberClient(memberID);
            appExecutors.mainThread().execute(() -> callBack.onMemberDetailsReloaded(memberObject));
        };

        appExecutors.diskIO().execute(runnable);
    }

    @Override
    public MemberObject getMemberClient(String memberID) {
        MemberObject memberObject = new MemberObject();
        memberObject.setBaseEntityId(memberID);
        return memberObject;
    }

    @Override
    public void saveRegistration(String jsonString, boolean isEditMode, BaseAncHomeVisitContract.InteractorCallBack callBack) {
        Timber.v("saveRegistration");
    }

    @Override
    public void calculateActions(final BaseAncHomeVisitContract.View view, MemberObject memberObject, final BaseAncHomeVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            final LinkedHashMap<String, BaseAncHomeVisitAction> actionList = new LinkedHashMap<>();

            try {
                BaseAncHomeVisitAction ba =
                        new BaseAncHomeVisitAction.Builder(view.getContext(), "Sample Action")
                                .withSubtitle("")
                                .withOptional(false)
                                .withFormName("anc")
                                .withHelper(new DangerSignsHelper())
                                .build();
                actionList.put("Sample Action", ba);

            } catch (BaseAncHomeVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    @Override
    public void submitVisit(final boolean editMode, final String memberID, final Map<String, BaseAncHomeVisitAction> map, final BaseAncHomeVisitContract.InteractorCallBack callBack) {
        final Runnable runnable = () -> {
            boolean result = true;
            try {
                submitVisit(editMode, memberID, map, "");
            } catch (Exception e) {
                Timber.e(e);
                result = false;
            }

            final boolean finalResult = result;
            appExecutors.mainThread().execute(() -> callBack.onSubmitted(finalResult));
        };

        appExecutors.diskIO().execute(runnable);
    }

    protected void submitVisit(final boolean editMode, final String memberID, final Map<String, BaseAncHomeVisitAction> map, String parentEventType) throws Exception {
        // create a map of the different types

        Map<String, BaseAncHomeVisitAction> externalVisits = new HashMap<>();
        Map<String, String> combinedJsons = new HashMap<>();
        String payloadType = null;
        String payloadDetails = null;

        // aggregate forms to be processed
        for (Map.Entry<String, BaseAncHomeVisitAction> entry : map.entrySet()) {
            String json = entry.getValue().getJsonPayload();
            if (StringUtils.isNotBlank(json)) {
                // do not process events that are meant to be in detached mode
                // in a similar manner to the the aggregated events

                BaseAncHomeVisitAction action = entry.getValue();
                BaseAncHomeVisitAction.ProcessingMode mode = action.getProcessingMode();

                if (mode == BaseAncHomeVisitAction.ProcessingMode.SEPARATE && StringUtils.isBlank(parentEventType)) {
                    externalVisits.put(entry.getKey(), entry.getValue());
                } else {
                    combinedJsons.put(entry.getKey(), json);
                }

                payloadType = action.getPayloadType().name();
                payloadDetails = action.getPayloadDetails();
            }
        }

        String type = StringUtils.isBlank(parentEventType) ? getEncounterType() : getEncounterType();

        // persist to database
        Visit visit = saveVisit(editMode, memberID, type, combinedJsons, parentEventType);
        if (visit != null) {
            saveVisitDetails(visit, payloadType, payloadDetails);
            processExternalVisits(visit, externalVisits, memberID);
        }
    }

    /**
     * recursively persist visits to the db
     *
     * @param visit
     * @param externalVisits
     * @param memberID
     * @throws Exception
     */
    protected void processExternalVisits(Visit visit, Map<String, BaseAncHomeVisitAction> externalVisits, String memberID) throws Exception {
        if (visit != null && !externalVisits.isEmpty()) {
            for (Map.Entry<String, BaseAncHomeVisitAction> entry : externalVisits.entrySet()) {
                Map<String, BaseAncHomeVisitAction> subEvent = new HashMap<>();
                subEvent.put(entry.getKey(), entry.getValue());

                String subMemberID = entry.getValue().getBaseEntityID();
                if (StringUtils.isBlank(subMemberID))
                    subMemberID = memberID;

                submitVisit(false, subMemberID, subEvent, visit.getVisitType());
            }
        }
    }

    protected Visit saveVisit(boolean editMode, String memberID, String encounterType,
                              final Map<String, String> jsonString,
                              String parentEventType
    ) throws Exception {

        AllSharedPreferences allSharedPreferences = AncLibrary.getInstance().context().allSharedPreferences();

        String derivedEncounterType = StringUtils.isBlank(parentEventType) ? encounterType : "";
        Event baseEvent = JsonFormUtils.processVisitJsonForm(allSharedPreferences, memberID, derivedEncounterType, jsonString, getTableName());

        // only tag the first event with the date
        if (StringUtils.isBlank(parentEventType)) {
            prepareEvent(baseEvent);
        } else {
            prepareSubEvent(baseEvent);
        }

        if (baseEvent != null) {
            baseEvent.setFormSubmissionId(JsonFormUtils.generateRandomUUIDString());
            JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);

            String visitID = (editMode) ?
                    visitRepository().getLatestVisit(memberID, getEncounterType()).getVisitId() :
                    JsonFormUtils.generateRandomUUIDString();

            // reset database
            if (editMode)
                deleteOldVisit(visitID);

            Visit visit = NCUtils.eventToVisit(baseEvent, visitID);
            visit.setPreProcessedJson(new Gson().toJson(baseEvent));
            visit.setParentVisitID(getParentVisitEventID(visit, parentEventType));

            visitRepository().addVisit(visit);
            return visit;
        }
        return null;
    }

    protected String getParentVisitEventID(Visit visit, String parentEventType) {
        return visitRepository().getParentVisitEventID(visit.getBaseEntityId(), parentEventType, visit.getDate());
    }

    protected VisitRepository visitRepository() {
        return AncLibrary.getInstance().visitRepository();
    }

    protected void deleteOldVisit(String visitID) {
        visitRepository().deleteVisit(visitID);
        AncLibrary.getInstance().visitDetailsRepository().deleteVisitDetails(visitID);

        List<Visit> childVisits = visitRepository().getChildEvents(visitID);
        for (Visit v : childVisits) {
            visitRepository().deleteVisit(v.getVisitId());
            AncLibrary.getInstance().visitDetailsRepository().deleteVisitDetails(v.getVisitId());
        }
    }

    protected void saveVisitDetails(Visit visit, String payloadType, String payloadDetails) {
        if (visit.getVisitDetails() == null) return;

        for (Map.Entry<String, List<VisitDetail>> entry : visit.getVisitDetails().entrySet()) {
            if (entry.getValue() != null) {
                for (VisitDetail d : entry.getValue()) {
                    d.setPreProcessedJson(payloadDetails);
                    d.setPreProcessedType(payloadType);
                    AncLibrary.getInstance().visitDetailsRepository().addVisitDetails(d);
                }
            }
        }
    }

    /**
     * Injects implementation specific changes to the event
     *
     * @param baseEvent
     */
    protected void prepareEvent(Event baseEvent) {
        if (baseEvent != null) {
            // add anc date obs and last
            List<Object> list = new ArrayList<>();
            list.add(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
            baseEvent.addObs(new Obs("concept", "text", "anc_visit_date", "",
                    list, new ArrayList<>(), null, "anc_visit_date"));
        }
    }

    /**
     * injects additional meta data to the event
     *
     * @param baseEvent
     */
    protected void prepareSubEvent(Event baseEvent) {
        Timber.v("You can add information to sub events");
    }

    protected String getEncounterType() {
        return Constants.EVENT_TYPE.ANC_HOME_VISIT;
    }

    protected String getTableName() {
        return Constants.TABLES.ANC_MEMBERS;
    }

}
