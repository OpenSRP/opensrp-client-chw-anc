package org.smartregister.chw.anc.util;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.Vaccine;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.FormUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

import static org.smartregister.chw.anc.util.Constants.ENCOUNTER_TYPE;

public class JsonFormUtils extends org.smartregister.util.JsonFormUtils {
    public static final String METADATA = "metadata";
    public static final String IMAGE = "image";
    public static final String ANC_HOME_VISIT = "home_visit_group";

    protected static Triple<Boolean, JSONObject, JSONArray> validateParameters(String jsonString) {

        JSONObject jsonForm = toJSONObject(jsonString);
        JSONArray fields = fields(jsonForm);

        Triple<Boolean, JSONObject, JSONArray> registrationFormParams = Triple.of(jsonForm != null && fields != null, jsonForm, fields);
        return registrationFormParams;
    }

    public static Event processAncJsonForm(AllSharedPreferences allSharedPreferences, String entityId, String encounterType, Map<String, String> jsonStrings) {

        // aggregate all the fields into 1 payload

        JSONObject jsonForm = null;
        JSONObject metadata = null;

        List<JSONObject> fields_obj = new ArrayList<>();

        for (Map.Entry<String, String> map : jsonStrings.entrySet()) {
            Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(map.getValue());

            if (!registrationFormParams.getLeft()) {
                return null;
            }

            if (jsonForm == null) {
                jsonForm = registrationFormParams.getMiddle();
            }

            if (metadata == null) {
                metadata = getJSONObject(jsonForm, METADATA);
            }

            // add all the fields to the event while injecting a new variable for grouping
            JSONArray local_fields = registrationFormParams.getRight();
            int x = 0;
            while (local_fields.length() > x) {
                try {
                    JSONObject obj = local_fields.getJSONObject(x);
                    obj.put(ANC_HOME_VISIT, map.getKey());
                    fields_obj.add(obj);
                } catch (JSONException e) {
                    Timber.e(e);
                }
                x++;
            }
        }

        JSONArray fields = new JSONArray(fields_obj);

        return org.smartregister.util.JsonFormUtils.createEvent(fields, metadata, formTag(allSharedPreferences), entityId, encounterType, Constants.TABLES.ANC_MEMBERS);
    }

    public static Event undoEvent(String ancMemberBaseID, String eventType) {

        Event eventMember = null;
        try {
            AllSharedPreferences allSharedPreferences = AncLibrary.getInstance().context().allSharedPreferences();

            eventMember = org.smartregister.util.JsonFormUtils.createEvent(new JSONArray(), new JSONObject(), formTag(allSharedPreferences), ancMemberBaseID, eventType, Constants.TABLES.ANC_MEMBERS);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return eventMember;
    }

    public static Event processJsonForm(AllSharedPreferences allSharedPreferences, String jsonString, String table) {

        Triple<Boolean, JSONObject, JSONArray> registrationFormParams = validateParameters(jsonString);

        if (!registrationFormParams.getLeft()) {
            return null;
        }

        JSONObject jsonForm = registrationFormParams.getMiddle();
        JSONArray fields = registrationFormParams.getRight();
        String entityId = getString(jsonForm, ENTITY_ID);

        return org.smartregister.util.JsonFormUtils.createEvent(fields, getJSONObject(jsonForm, METADATA), formTag(allSharedPreferences), entityId, getString(jsonForm, ENCOUNTER_TYPE), table);
    }

    protected static FormTag formTag(AllSharedPreferences allSharedPreferences) {
        FormTag formTag = new FormTag();
        formTag.providerId = allSharedPreferences.fetchRegisteredANM();
        formTag.appVersion = AncLibrary.getInstance().getApplicationVersion();
        formTag.databaseVersion = AncLibrary.getInstance().getDatabaseVersion();
        return formTag;
    }

    public static void tagEvent(AllSharedPreferences allSharedPreferences, Event event) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        event.setProviderId(providerId);
        event.setLocationId(locationId(allSharedPreferences));
        event.setChildLocationId(allSharedPreferences.fetchCurrentLocality());
        event.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        event.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));

        event.setClientApplicationVersion(AncLibrary.getInstance().getApplicationVersion());
        event.setClientDatabaseVersion(AncLibrary.getInstance().getDatabaseVersion());
    }

    private static String locationId(AllSharedPreferences allSharedPreferences) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        String userLocationId = allSharedPreferences.fetchUserLocalityId(providerId);
        if (StringUtils.isBlank(userLocationId)) {
            userLocationId = allSharedPreferences.fetchDefaultLocalityId(providerId);
        }

        return userLocationId;
    }

    public static void getRegistrationForm(JSONObject jsonObject, String entityId, String currentLocationId) throws JSONException {
        jsonObject.getJSONObject(METADATA).put(ENCOUNTER_LOCATION, currentLocationId);
        jsonObject.put(org.smartregister.util.JsonFormUtils.ENTITY_ID, entityId);
    }

    public static JSONObject getFormAsJson(String formName) throws Exception {
        return FormUtils.getInstance(AncLibrary.getInstance().context().applicationContext()).getFormJson(formName);
    }

    public static Vaccine tagSyncMetadata(AllSharedPreferences allSharedPreferences, Vaccine vaccine) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        vaccine.setAnmId(providerId);
        vaccine.setLocationId(locationId(allSharedPreferences));
        vaccine.setChildLocationId(allSharedPreferences.fetchCurrentLocality());
        vaccine.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        vaccine.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));
        return vaccine;
    }

    public static ServiceRecord tagSyncMetadata(AllSharedPreferences allSharedPreferences, ServiceRecord serviceRecord) {
        String providerId = allSharedPreferences.fetchRegisteredANM();
        serviceRecord.setAnmId(providerId);
        serviceRecord.setLocationId(locationId(allSharedPreferences));
        serviceRecord.setChildLocationId(allSharedPreferences.fetchCurrentLocality());
        serviceRecord.setTeam(allSharedPreferences.fetchDefaultTeam(providerId));
        serviceRecord.setTeamId(allSharedPreferences.fetchDefaultTeamId(providerId));
        return serviceRecord;
    }


    /**
     * Returns a value from json form field
     *
     * @param jsonObject native forms jsonObject
     * @param key        field object key
     * @return value
     */
    public static String getValue(JSONObject jsonObject, String key) {
        try {
            JSONArray jsonArray = jsonObject.getJSONObject(JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
            int x = 0;
            while (jsonArray.length() > x) {
                JSONObject jo = jsonArray.getJSONObject(x);
                if (jo.getString(JsonFormConstants.KEY).equalsIgnoreCase(key)) {
                    return jo.getString(JsonFormConstants.VALUE);
                }
                x++;
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return "";
    }

    public static String getFirstObjectKey(JSONObject jsonObject) {
        try {
            JSONArray jsonArray = jsonObject.getJSONObject(JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
            if (jsonArray.length() > 0) {
                return jsonArray.getJSONObject(0).getString(JsonFormConstants.KEY);

            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return "";
    }


    /**
     * Returns a value from a native forms checkbox field and returns an comma separated string
     *
     * @param jsonObject native forms jsonObject
     * @param key        field object key
     * @return value
     */
    public static String getCheckBoxValue(JSONObject jsonObject, String key) {
        try {
            JSONArray jsonArray = jsonObject.getJSONObject(JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);

            JSONObject jo = null;
            int x = 0;
            while (jsonArray.length() > x) {
                jo = jsonArray.getJSONObject(x);
                if (jo.getString(JsonFormConstants.KEY).equalsIgnoreCase(key)) {
                    break;
                }
                x++;
            }

            StringBuilder resBuilder = new StringBuilder();
            if (jo != null) {
                // read all the checkboxes
                JSONArray jaOptions = jo.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
                int optionSize = jaOptions.length();
                int y = 0;
                while (optionSize > y) {
                    JSONObject options = jaOptions.getJSONObject(y);
                    if (options.getBoolean(JsonFormConstants.VALUE)) {
                        resBuilder.append(options.getString(JsonFormConstants.TEXT)).append(", ");
                    }
                    y++;
                }

                String res = resBuilder.toString();
                res = res.substring(0, res.length() - 2);
                return res;
            }

        } catch (Exception e) {
            Timber.e(e);
        }
        return "";
    }


    public static void populateForm(JSONObject jsonObject, Map<String, List<VisitDetail>> details) {
        Timber.v("populateForm");
        try {
            // x steps
            String count_str = jsonObject.getString(JsonFormConstants.COUNT);

            int step_count = StringUtils.isNotBlank(count_str) ? Integer.valueOf(count_str) : 1;
            while (step_count > 0) {
                JSONArray jsonArray = jsonObject.getJSONObject(MessageFormat.format("step{0}", step_count)).getJSONArray(JsonFormConstants.FIELDS);

                int field_count = jsonArray.length() - 1;
                while (field_count >= 0) {

                    JSONObject jo = jsonArray.getJSONObject(field_count);
                    List<VisitDetail> detailList = details.get(jo.getString(JsonFormConstants.KEY));

                    if (detailList != null) {
                        if (jo.getString(JsonFormConstants.TYPE).equalsIgnoreCase(JsonFormConstants.CHECK_BOX)) {
                            jo.put(JsonFormConstants.VALUE, getValue(jo, detailList));
                        } else {
                            jo.put(JsonFormConstants.VALUE, getValue(detailList.get(0)));
                        }
                    }

                    field_count--;
                }

                step_count--;
            }

        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public static String getValue(VisitDetail visitDetail) {
        String humanReadable = cleanString(visitDetail.getHumanReadable());
        if (StringUtils.isNotBlank(humanReadable))
            return humanReadable;

        return cleanString(visitDetail.getDetails());
    }

    public static JSONArray getValue(JSONObject jo, List<VisitDetail> visitDetails) throws JSONException {
        JSONArray values = new JSONArray();
        if (jo.getString(JsonFormConstants.TYPE).equalsIgnoreCase(JsonFormConstants.CHECK_BOX)) {
            JSONArray options = jo.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
            HashMap<String, NameID> valueMap = new HashMap<>();

            int x = options.length() - 1;
            while (x >= 0) {
                JSONObject object = options.getJSONObject(x);
                valueMap.put(object.getString(JsonFormConstants.TEXT), new NameID(object.getString(JsonFormConstants.KEY), x));
                x--;
            }

            for (VisitDetail d : visitDetails) {
                String val = getValue(d);
                NameID nid = valueMap.get(val);
                if (nid != null) {
                    values.put(nid.name);
                    options.getJSONObject(nid.position).put(JsonFormConstants.VALUE, true);
                }
            }
        } else {
            for (VisitDetail d : visitDetails) {
                String val = getValue(d);
                if (StringUtils.isNotBlank(val)) {
                    values.put(val);
                }
            }
        }
        return values;
    }

    public static String cleanString(String dirtyString) {
        if (StringUtils.isBlank(dirtyString))
            return "";

        return dirtyString.substring(1, dirtyString.length() - 1);
    }

    private static class NameID {
        private String name;
        private int position;

        public NameID(String name, int position) {
            this.name = name;
            this.position = position;
        }
    }
}