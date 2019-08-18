package org.smartregister.chw.anc.model;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.fragment.BaseHomeVisitFragment;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.immunization.domain.ServiceWrapper;
import org.smartregister.immunization.domain.VaccineWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This action list allows users to either load a form or link it to a separate fragment.
 */
public class BaseAncHomeVisitAction {

    private String baseEntityID;
    private String title;
    private String subTitle;
    private Status actionStatus;
    private ScheduleStatus scheduleStatus;
    private ProcessingMode processingMode;
    private boolean optional;
    private BaseHomeVisitFragment destinationFragment;
    private String formName;
    private String jsonPayload;
    private String selectedOption;
    private AncHomeVisitActionHelper ancHomeVisitActionHelper;
    private List<VaccineWrapper> vaccineWrapper;
    private List<ServiceWrapper> serviceWrapper;
    private Map<String, List<VisitDetail>> details;
    private Context context;

    private BaseAncHomeVisitAction(Builder builder) throws ValidationException {
        this.baseEntityID = builder.baseEntityID;
        this.title = builder.title;
        this.subTitle = builder.subTitle;
        this.actionStatus = builder.actionStatus;
        this.scheduleStatus = builder.scheduleStatus;
        this.optional = builder.optional;
        this.destinationFragment = builder.destinationFragment;
        this.formName = builder.formName;
        this.ancHomeVisitActionHelper = builder.ancHomeVisitActionHelper;
        this.vaccineWrapper = builder.vaccineWrapper;
        this.serviceWrapper = builder.serviceWrapper;
        this.details = builder.details;
        this.context = builder.context;
        this.processingMode = builder.processingMode;
        this.jsonPayload = builder.jsonPayload;

        validateMe();
        initialize();
    }

    private void initialize() {
        try {
            if (StringUtils.isBlank(jsonPayload) && StringUtils.isNotBlank(formName)) {
                JSONObject jsonObject = JsonFormUtils.getFormAsJson(formName);

                // update the form details
                if (details != null && details.size() > 0) {
                    JsonFormUtils.populateForm(jsonObject, details);
                }

                jsonPayload = jsonObject.toString();
            }

            if (ancHomeVisitActionHelper != null) {
                ancHomeVisitActionHelper.onJsonFormLoaded(jsonPayload, context, details);
                String pre_processed = ancHomeVisitActionHelper.getPreProcessed();
                if (StringUtils.isNotBlank(pre_processed)) {
                    this.jsonPayload = pre_processed;
                }

                String sub_title = ancHomeVisitActionHelper.getPreProcessedSubTitle();
                if (StringUtils.isNotBlank(sub_title)) {
                    this.subTitle = sub_title;
                }

                ScheduleStatus status = ancHomeVisitActionHelper.getPreProcessedStatus();
                if (status != null) {
                    this.scheduleStatus = status;
                }
            }

            if (details != null && details.size() > 0) {
                if (destinationFragment != null) {
                    setJsonPayload(destinationFragment.getJsonObject().toString()); // force reload
                } else {
                    setJsonPayload(this.jsonPayload); // force reload
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Validate that action object has a proper end point destination
     */
    private void validateMe() throws ValidationException {
        if (StringUtils.isBlank(formName) && destinationFragment == null) {
            throw new ValidationException("This action object lacks a valid form or destination fragment");
        }
    }

    public String getBaseEntityID() {
        return baseEntityID;
    }

    public void setBaseEntityID(String baseEntityID) {
        this.baseEntityID = baseEntityID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public Status getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(Status actionStatus) {
        this.actionStatus = actionStatus;
    }

    public ScheduleStatus getScheduleStatus() {
        return scheduleStatus;
    }

    public void setScheduleStatus(ScheduleStatus scheduleStatus) {
        this.scheduleStatus = scheduleStatus;
    }

    public ProcessingMode getProcessingMode() {
        return processingMode;
    }

    public void setProcessingMode(ProcessingMode processingMode) {
        this.processingMode = processingMode;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getJsonPayload() {
        return jsonPayload;
    }

    public void setJsonPayload(String jsonPayload) {
        this.jsonPayload = jsonPayload;
        if (StringUtils.isNotBlank(jsonPayload)) {
            this.setScheduleStatus(ScheduleStatus.DUE);
        }

        // helper processing
        if (ancHomeVisitActionHelper != null) {
            ancHomeVisitActionHelper.onPayloadReceived(jsonPayload);

            String sub_title = ancHomeVisitActionHelper.evaluateSubTitle();
            if (sub_title != null) {
                setSubTitle(sub_title);
            }

            String post_process = ancHomeVisitActionHelper.postProcess(jsonPayload);
            if (post_process != null) {
                this.jsonPayload = ancHomeVisitActionHelper.postProcess(jsonPayload);
            }


            ancHomeVisitActionHelper.onPayloadReceived(this);
        }

        evaluateStatus();
    }

    public void setProcessedJsonPayload(String jsonPayload) {
        this.jsonPayload = jsonPayload;
    }

    public BaseHomeVisitFragment getDestinationFragment() {
        return destinationFragment;
    }

    public void setDestinationFragment(BaseHomeVisitFragment destinationFragment) {
        this.destinationFragment = destinationFragment;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    public AncHomeVisitActionHelper getAncHomeVisitActionHelper() {
        return ancHomeVisitActionHelper;
    }

    public void setAncHomeVisitActionHelper(AncHomeVisitActionHelper ancHomeVisitActionHelper) {
        this.ancHomeVisitActionHelper = ancHomeVisitActionHelper;
    }

    /**
     * This value will evaluate the json payload as complete if payload is preset
     * or pending if the payload is not present. Any custom execution will also be processed to get the final value
     */
    public void evaluateStatus() {
        setActionStatus(computedStatus());

        if (getAncHomeVisitActionHelper() != null) {
            setActionStatus(getAncHomeVisitActionHelper().evaluateStatusOnPayload());
        }
    }

    public BaseAncHomeVisitAction.Status computedStatus() {
        if (StringUtils.isNotBlank(getJsonPayload())) {
            return Status.COMPLETED;
        } else {
            return Status.PENDING;
        }
    }

    public List<VaccineWrapper> getVaccineWrapper() {
        return (getActionStatus() == Status.COMPLETED) ? vaccineWrapper : null;
    }

    public void setVaccineWrapper(VaccineWrapper vaccineWrapper) {
        this.vaccineWrapper.add(vaccineWrapper);
    }

    public List<ServiceWrapper> getServiceWrapper() {
        return (getActionStatus() == Status.COMPLETED) ? serviceWrapper : null;
    }

    public void setServiceWrapper(ServiceWrapper serviceWrapper) {
        this.serviceWrapper.add(serviceWrapper);
    }

    public enum Status {COMPLETED, PARTIALLY_COMPLETED, PENDING}

    public enum ScheduleStatus {DUE, OVERDUE}

    /**
     * Detached processing generates separate event when form is submitted
     */
    public enum ProcessingMode {COMBINED, DETACHED}

    public interface AncHomeVisitActionHelper {

        /**
         * Inject values to the json form before rendering
         * Only called once after the form has been read from the assets folder
         */
        void onJsonFormLoaded(String jsonString, Context context, Map<String, List<VisitDetail>> details);

        /**
         * executed after form is loaded.
         * Returns a string or null
         */
        String getPreProcessed();

        /**
         * Is executed immediately a json payload is received
         *
         * @param jsonPayload
         */
        void onPayloadReceived(String jsonPayload);


        /**
         * executed after form is loaded on start
         * add functionality to evaluate the state of the view immediately the form is processed
         */
        ScheduleStatus getPreProcessedStatus();

        /**
         * executed after form is loaded on start
         * add functionality to evaluate the subtitle information immediately the form is processed
         */
        String getPreProcessedSubTitle();

        /**
         * add details to process the received payload
         *
         * @param jsonPayload
         */
        String postProcess(String jsonPayload);

        /**
         * executed after the payload is received
         */
        String evaluateSubTitle();

        /**
         * Evaluated after payload is received
         *
         * @return
         */
        Status evaluateStatusOnPayload();

        /**
         * Custom processing after payload is received
         */
        void onPayloadReceived(BaseAncHomeVisitAction ancHomeVisitAction);
    }

    public static class Builder {
        private String baseEntityID;
        private String title;
        private String subTitle;
        private Status actionStatus = Status.PENDING;
        private ScheduleStatus scheduleStatus = ScheduleStatus.DUE;
        private ProcessingMode processingMode = ProcessingMode.COMBINED;
        private boolean optional = true;
        private BaseHomeVisitFragment destinationFragment;
        private String formName;
        private AncHomeVisitActionHelper ancHomeVisitActionHelper;
        private List<VaccineWrapper> vaccineWrapper = new ArrayList<>();
        private List<ServiceWrapper> serviceWrapper = new ArrayList<>();
        private Map<String, List<VisitDetail>> details = new HashMap<>();
        private Context context;
        private String jsonPayload;

        public Builder(Context context, String title) {
            this.context = context;
            this.title = title;
        }

        public Builder withBaseEntityID(String baseEntityID) {
            this.baseEntityID = baseEntityID;
            return this;
        }

        public Builder withSubtitle(String subTitle) {
            this.subTitle = subTitle;
            return this;
        }

        public Builder withOptional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public Builder withDestinationFragment(BaseHomeVisitFragment destinationFragment) {
            this.destinationFragment = destinationFragment;
            return this;
        }

        public Builder withFormName(String formName) {
            this.formName = formName;
            return this;
        }

        public Builder withDetails(Map<String, List<VisitDetail>> details) {
            this.details = details;
            return this;
        }

        public Builder withHelper(AncHomeVisitActionHelper ancHomeVisitActionHelper) {
            this.ancHomeVisitActionHelper = ancHomeVisitActionHelper;
            return this;
        }

        public Builder withScheduleStatus(ScheduleStatus scheduleStatus) {
            this.scheduleStatus = scheduleStatus;
            return this;
        }

        public Builder withProcessingMode(ProcessingMode processingMode) {
            this.processingMode = processingMode;
            return this;
        }

        public Builder withVaccineWrapper(VaccineWrapper vaccineWrapper) {
            this.vaccineWrapper.add(vaccineWrapper);
            return this;
        }

        public Builder withVaccineWrapper(List<VaccineWrapper> vaccineWrapper) {
            this.vaccineWrapper.addAll(vaccineWrapper);
            return this;
        }

        public Builder withServiceWrapper(ServiceWrapper serviceWrapper) {
            this.serviceWrapper.add(serviceWrapper);
            return this;
        }

        public Builder withServiceWrapper(List<ServiceWrapper> serviceWrapper) {
            this.serviceWrapper.addAll(serviceWrapper);
            return this;
        }

        public Builder withJsonPayload(String jsonPayload) {
            this.jsonPayload = jsonPayload;
            return this;
        }

        public BaseAncHomeVisitAction build() throws ValidationException {
            return new BaseAncHomeVisitAction(this);
        }
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}
