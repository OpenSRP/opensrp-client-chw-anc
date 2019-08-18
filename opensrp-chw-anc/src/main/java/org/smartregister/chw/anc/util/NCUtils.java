package org.smartregister.chw.anc.util;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.google.gson.Gson;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.contract.BaseAncWomanCallDialogContract;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.opensrp_chw_anc.R;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.db.EventClient;
import org.smartregister.immunization.domain.VaccineWrapper;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.PermissionUtils;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import timber.log.Timber;

import static org.smartregister.chw.anc.AncLibrary.getInstance;
import static org.smartregister.chw.anc.util.JsonFormUtils.cleanString;
import static org.smartregister.util.JsonFormUtils.VALUE;
import static org.smartregister.util.JsonFormUtils.getFieldJSONObject;
import static org.smartregister.util.Utils.getAllSharedPreferences;

public class NCUtils {

    public static final SimpleDateFormat dd_MMM_yyyy = new SimpleDateFormat("dd MMM yyyy");
    public static final SimpleDateFormat yyyy_mm_dd = new SimpleDateFormat("yyyy-mm-dd");
    private static String TAG = NCUtils.class.getCanonicalName();
    private static String[] default_obs = {"start", "end", "deviceid", "subscriberid", "simserial", "phonenumber"};
    private static String[] vaccines = {"bcg_date", "opv0_date"};

    public static String firstCharacterUppercase(String str) {
        if (TextUtils.isEmpty(str)) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public static String convertToDateFormateString(String timeAsDDMMYYYY, SimpleDateFormat dateFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-mm-yyyy");//12-08-2018
        try {
            Date date = sdf.parse(timeAsDDMMYYYY);
            return dateFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static float convertDpToPixel(float dp, Context context) {
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static float convertPixelsToDp(float px, Context context) {
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static boolean areDrawablesIdentical(Drawable drawableA, Drawable drawableB) {
        Drawable.ConstantState stateA = drawableA.getConstantState();
        Drawable.ConstantState stateB = drawableB.getConstantState();
        // If the constant state is identical, they are using the same drawable resource.
        // However, the opposite is not necessarily true.
        return (stateA != null && stateB != null && stateA.equals(stateB))
                || getBitmap(drawableA).sameAs(getBitmap(drawableB));
    }

    public static Bitmap getBitmap(Drawable drawable) {
        Bitmap result;
        if (drawable instanceof BitmapDrawable) {
            result = ((BitmapDrawable) drawable).getBitmap();
        } else {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            // Some drawables have no intrinsic width - e.g. solid colours.
            if (width <= 0) {
                width = 1;
            }
            if (height <= 0) {
                height = 1;
            }

            result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return result;
    }

    public static Integer daysBetweenDateAndNow(String date) {
        DateTime duration;
        if (StringUtils.isNotBlank(date)) {
            try {
                duration = new DateTime(new Date(Long.valueOf(date)));
                Days days = Days.daysBetween(duration.withTimeAtStartOfDay(), DateTime.now().withTimeAtStartOfDay());
                return days.getDays();
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        return null;
    }

    public static String getLocalForm(String jsonForm) {
        String suffix = Locale.getDefault().getLanguage().equals("fr") ? "_fr" : "";
        return MessageFormat.format("{0}{1}", jsonForm, suffix);
    }

    public static org.smartregister.Context context() {
        return AncLibrary.getInstance().context();
    }

    public static boolean launchDialer(final Activity activity, final BaseAncWomanCallDialogContract.View callView, final String phoneNumber) {

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            // set a pending call execution request
            if (callView != null) {
                callView.setPendingCallRequest(new BaseAncWomanCallDialogContract.Dialer() {
                    @Override
                    public void callMe() {
                        NCUtils.launchDialer(activity, callView, phoneNumber);
                    }
                });
            }

            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_PHONE_STATE}, PermissionUtils.PHONE_STATE_PERMISSION_REQUEST_CODE);

            return false;
        } else {

            if (((TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number()
                    == null) {

                Timber.i("No dial application so we launch copy to clipboard...");

                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(activity.getText(R.string.copied_phone_number), phoneNumber);
                clipboard.setPrimaryClip(clip);

                CopyToClipboardDialog copyToClipboardDialog = new CopyToClipboardDialog(activity, R.style.copy_clipboard_dialog);
                copyToClipboardDialog.setContent(phoneNumber);
                copyToClipboardDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                copyToClipboardDialog.show();
                // no phone
                Toast.makeText(activity, activity.getText(R.string.copied_phone_number), Toast.LENGTH_SHORT).show();

            } else {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null));
                activity.startActivity(intent);
            }
            return true;
        }
    }

    public static Spanned fromHtml(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(text);
        }
    }

    public static String getTodayDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date date = new Date(System.currentTimeMillis());
        return formatter.format(date);
    }

    private static SimpleDateFormat getSourceDateFormat() {
        return new SimpleDateFormat(getInstance().getSourceDateFormat(), Locale.getDefault());
    }

    private static SimpleDateFormat getSaveDateFormat() {
        return new SimpleDateFormat(getInstance().getSaveDateFormat(), Locale.getDefault());
    }

    public static void addEvent(AllSharedPreferences allSharedPreferences, Event baseEvent) throws Exception {
        if (baseEvent != null) {
            JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
            JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(baseEvent));
            getSyncHelper().addEvent(baseEvent.getBaseEntityId(), eventJson);
        }
    }

    public static void processEvent(String baseEntityID, JSONObject eventJson) throws Exception {
        if (eventJson != null) {
            getSyncHelper().addEvent(baseEntityID, eventJson);

            long lastSyncTimeStamp = getAllSharedPreferences().fetchLastUpdatedAtDate(0);
            Date lastSyncDate = new Date(lastSyncTimeStamp);
            getClientProcessorForJava().processClient(getSyncHelper().getEvents(lastSyncDate, BaseRepository.TYPE_Unprocessed));
            getAllSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
        }
    }

    public static void startClientProcessing() throws Exception {
        long lastSyncTimeStamp = getAllSharedPreferences().fetchLastUpdatedAtDate(0);
        Date lastSyncDate = new Date(lastSyncTimeStamp);
        getClientProcessorForJava().processClient(getSyncHelper().getEvents(lastSyncDate, BaseRepository.TYPE_Unprocessed));
        getAllSharedPreferences().saveLastUpdatedAtDate(lastSyncDate.getTime());
    }

    public static ECSyncHelper getSyncHelper() {
        return getInstance().getEcSyncHelper();
    }

    public static ClientProcessorForJava getClientProcessorForJava() {
        return getInstance().getClientProcessorForJava();
    }

    public static Visit eventToVisit(Event event, String visitID) throws JSONException {
        Visit visit = new Visit();
        visit.setVisitId(visitID);
        visit.setBaseEntityId(event.getBaseEntityId());
        visit.setDate(event.getEventDate());
        visit.setVisitType(event.getEventType());
        visit.setEventId(event.getEventId());
        visit.setFormSubmissionId(event.getFormSubmissionId());
        visit.setJson(new JSONObject(JsonFormUtils.gson.toJson(event)).toString());
        visit.setProcessed(false);
        visit.setCreatedAt(new Date());
        visit.setUpdatedAt(new Date());

        Map<String, List<VisitDetail>> details = new HashMap<>();
        if (event.getObs() != null) {
            details = eventsObsToDetails(event.getObs(), visit.getVisitId(), null);
        }

        visit.setVisitDetails(details);
        return visit;
    }

    public static Map<String, List<VisitDetail>> eventsObsToDetails(List<Obs> obsList, String visitID, String baseEntityID) throws JSONException {
        List<String> exceptions = Arrays.asList(default_obs);
        Map<String, List<VisitDetail>> details = new HashMap<>();
        if (obsList == null)
            return details;

        for (Obs obs : obsList) {
            if (!exceptions.contains(obs.getFormSubmissionField())) {
                VisitDetail detail = new VisitDetail();
                detail.setVisitDetailsId(JsonFormUtils.generateRandomUUIDString());
                detail.setVisitId(visitID);
                detail.setBaseEntityId(baseEntityID);
                detail.setVisitKey(obs.getFormSubmissionField());

                if (detail.getVisitKey().contains("date")) {
                    // parse the
                    detail.setDetails(getFormattedDate(getSourceDateFormat(), getSaveDateFormat(), cleanString(obs.getValues().toString())));
                    detail.setHumanReadable(getFormattedDate(getSourceDateFormat(), getSaveDateFormat(), cleanString(obs.getHumanReadableValues().toString())));
                } else {
                    detail.setDetails(cleanString(obs.getValues().toString()));
                    detail.setHumanReadable(cleanString(obs.getHumanReadableValues().toString()));
                }

                detail.setJsonDetails(new JSONObject(JsonFormUtils.gson.toJson(obs)).toString());
                detail.setProcessed(false);
                detail.setCreatedAt(new Date());
                detail.setUpdatedAt(new Date());

                List<VisitDetail> currentList = details.get(detail.getVisitKey());
                if (currentList == null)
                    currentList = new ArrayList<>();

                currentList.add(detail);
                details.put(detail.getVisitKey(), currentList);
            }
        }

        return details;
    }

    public static String getFormattedDate(SimpleDateFormat source_sdf, SimpleDateFormat dest_sdf, String value) {
        try {
            Date date = source_sdf.parse(value);
            return dest_sdf.format(date);
        } catch (Exception e) {
            Timber.e(e);
        }
        return value;
    }

    // executed before processing
    public static Visit eventToVisit(Event event) throws JSONException {
        return eventToVisit(event, JsonFormUtils.generateRandomUUIDString());
    }

    public static void processAncHomeVisit(EventClient baseEvent) {
        processAncHomeVisit(baseEvent, null);
    }

    public static void processAncHomeVisit(EventClient baseEvent, SQLiteDatabase database) {
        try {
            Visit visit = getInstance().visitRepository().getVisitByFormSubmissionID(baseEvent.getEvent().getFormSubmissionId());
            if (visit == null) {
                visit = eventToVisit(baseEvent.getEvent());
                if (database != null) {
                    getInstance().visitRepository().addVisit(visit, database);
                } else {
                    getInstance().visitRepository().addVisit(visit);
                }
                if (visit.getVisitDetails() != null) {
                    for (Map.Entry<String, List<VisitDetail>> entry : visit.getVisitDetails().entrySet()) {
                        if (entry.getValue() != null) {
                            for (VisitDetail detail : entry.getValue()) {
                                if (database != null) {
                                    getInstance().visitDetailsRepository().addVisitDetails(detail, database);
                                } else {
                                    getInstance().visitDetailsRepository().addVisitDetails(detail);
                                }
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    // executed by event client processor
    public static Visit eventToVisit(org.smartregister.domain.db.Event event) throws JSONException {
        List<String> exceptions = Arrays.asList(default_obs);

        Visit visit = new Visit();
        visit.setVisitId(org.smartregister.chw.anc.util.JsonFormUtils.generateRandomUUIDString());
        visit.setBaseEntityId(event.getBaseEntityId());
        visit.setDate(event.getEventDate().toDate());
        visit.setVisitType(event.getEventType());
        visit.setEventId(event.getEventId());
        visit.setFormSubmissionId(event.getFormSubmissionId());
        visit.setJson(new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(event)).toString());
        visit.setProcessed(true);
        visit.setCreatedAt(new Date());
        visit.setUpdatedAt(new Date());

        Map<String, List<VisitDetail>> details = new HashMap<>();
        if (event.getObs() != null) {
            for (org.smartregister.domain.db.Obs obs : event.getObs()) {
                if (!exceptions.contains(obs.getFormSubmissionField())) {
                    VisitDetail detail = new VisitDetail();
                    detail.setVisitDetailsId(org.smartregister.chw.anc.util.JsonFormUtils.generateRandomUUIDString());
                    detail.setVisitId(visit.getVisitId());
                    detail.setVisitKey(obs.getFormSubmissionField());

                    if (detail.getVisitKey().contains("date")) {
                        // parse the
                        detail.setDetails(getFormattedDate(getSourceDateFormat(), getSaveDateFormat(), cleanString(obs.getValues().toString())));
                        detail.setHumanReadable(getFormattedDate(getSourceDateFormat(), getSaveDateFormat(), cleanString(obs.getHumanReadableValues().toString())));
                    } else {
                        detail.setDetails(cleanString(obs.getValues().toString()));
                        detail.setHumanReadable(cleanString(obs.getHumanReadableValues().toString()));
                    }

                    detail.setProcessed(true);
                    detail.setCreatedAt(new Date());
                    detail.setUpdatedAt(new Date());

                    List<VisitDetail> currentList = details.get(detail.getVisitKey());
                    if (currentList == null)
                        currentList = new ArrayList<>();

                    currentList.add(detail);
                    details.put(detail.getVisitKey(), currentList);
                }
            }
        }

        visit.setVisitDetails(details);
        return visit;
    }

    public static int getMemberProfileImageResourceIDentifier(String entityType) {
        return R.mipmap.ic_member;
    }

    public static String gestationAgeString(String lmp, Context context, boolean full) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MM-yyyy");
        int ga = Days.daysBetween(formatter.parseDateTime(lmp), new DateTime()).getDays() / 7;
        if (full)
            return String.format(context.getString(R.string.gest_age), String.valueOf(ga)) + " " + context.getString(R.string.gest_age_weeks);
        return String.valueOf(ga);
    }

    public static void saveVaccineEvents(JSONArray fields, String baseID) {

        for (int i = 0; i < vaccines.length; i++) {
            saveVaccineEvent(vaccines[i], getFieldJSONObject(fields, vaccines[i]), baseID);
        }
    }

    private static void saveVaccineEvent(String vaccineName, JSONObject vaccineObject, String baseID) {
        if (vaccineObject != null)
            try {
                String vaccineDate = vaccineObject.optString(VALUE);
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
                SimpleDateFormat formatEventDate = new SimpleDateFormat("yyy-MM-dd");
                Date date = formatter.parse(vaccineDate);

                JSONObject vaccineEventObject = createVaccineEvent(vaccineName, formatEventDate.format(date));
                Event baseEvent = new Gson().fromJson(vaccineEventObject.toString(), Event.class);
                baseEvent.setDateCreated(new Date());
                baseEvent.setEventDate(date);
                baseEvent.setBaseEntityId(baseID);
                baseEvent.setEventType("Vaccination");
                baseEvent.setEntityType("vaccination");
                baseEvent.setType("Event");
                baseEvent.setFormSubmissionId(UUID.randomUUID().toString());
                baseEvent.setEventId(UUID.randomUUID().toString());
                addEvent(getAllSharedPreferences(), baseEvent);
            } catch (Exception e) {
                Timber.e(e);
            }
    }

    private static JSONObject createVaccineEvent(String vaccineName, String vaccineDate) {

        JSONObject vaccineEvent = new JSONObject();
        try {
            vaccineEvent.put("obs", makeObs(vaccineName, vaccineDate));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return vaccineEvent;
    }

    private static JSONArray makeObs(String vaccine, String vaccineDate) {
        JSONArray dateValues = new JSONArray();
        JSONArray obsArray = new JSONArray();
        JSONArray calculateValues = new JSONArray();
        JSONObject vaccineName = new JSONObject();
        JSONObject vaccineDetails = new JSONObject();

        String parentCode = null;
        String formSubmissionField = null;
        String countValue = null;

        try {
            dateValues.put(vaccineDate);
            vaccineName.put("fieldType", "concept");
            vaccineName.put("fieldDataType", "date");
            vaccineName.put("fieldCode", "1410AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            vaccineName.put("values", dateValues);
            if (vaccine.contains("BCG")) {
                parentCode = "886AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
                formSubmissionField = "bcg";
                countValue = "0";
            } else if (vaccine.equalsIgnoreCase("OPV")) {
                parentCode = "783AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
                formSubmissionField = "opv_0";
                countValue = "0";
            }
            vaccineName.put("formSubmissionField", formSubmissionField);
            vaccineName.put("parentCode", parentCode);

            vaccineDetails.put("fieldType", "concept");
            vaccineDetails.put("fieldDataType", "calculate");
            vaccineDetails.put("fieldCode", "1418AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            vaccineDetails.put("parentCode", parentCode);
            calculateValues.put(countValue);
            vaccineDetails.put("values", calculateValues);
            vaccineDetails.put("formSubmissionField", formSubmissionField + "_dose");
            obsArray.put(vaccineName);
            obsArray.put(vaccineDetails);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return obsArray;
    }

    @Nullable
    public static JSONObject getVisitJSONFromWrapper(String entityID, Map<VaccineWrapper, String> vaccineWrapperDateMap) {

        try {
            JSONObject jsonObject = JsonFormUtils.getFormAsJson(Constants.FORMS.IMMUNIZATIOIN_VISIT);
            jsonObject.put("entity_id", entityID);
            JSONArray jsonArray = jsonObject.getJSONObject(JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);


            for (Map.Entry<VaccineWrapper, String> entry : vaccineWrapperDateMap.entrySet()) {
                JSONObject field = new JSONObject();
                field.put(JsonFormConstants.KEY, removeSpaces(entry.getKey().getName()));
                field.put(JsonFormConstants.OPENMRS_ENTITY_PARENT, "");
                field.put(JsonFormConstants.OPENMRS_ENTITY, "concept");
                field.put(JsonFormConstants.OPENMRS_ENTITY_ID, removeSpaces(entry.getKey().getName()));
                field.put(JsonFormConstants.VALUE, entry.getValue());

                jsonArray.put(field);
            }

            return jsonObject;
        } catch (Exception e) {
            Timber.e(e);
        }
        return null;
    }

    public static String removeSpaces(String s) {
        return s.replace(" ", "_").toLowerCase();
    }
}
