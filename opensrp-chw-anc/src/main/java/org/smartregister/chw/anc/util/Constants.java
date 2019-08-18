package org.smartregister.chw.anc.util;

public interface Constants {

    int REQUEST_CODE_GET_JSON = 2244;
    String ENCOUNTER_TYPE = "encounter_type";

    interface JSON_FORM_EXTRA {
        String JSON = "json";
        String NEXT = "next";
        String ENCOUNTER_TYPE = "encounter_type";
        String ENTITY_TYPE = "entity_id";
        String OPENSPR_ID = "opensrp_id";
    }

    interface EVENT_TYPE {
        String ANC_REGISTRATION = "Anc Registration";
        String ANC_HOME_VISIT = "ANC Home Visit";
        String ANC_HOME_VISIT_NOT_DONE = "ANC Home Visit Not Done";
        String ANC_HOME_VISIT_NOT_DONE_UNDO = "ANC Home Visit Not Done Undo";
        String PNC_REGISTRATION = "PNC Registration";
        String PNC_HOME_VISIT = "PNC Home Visit";
        String PNC_HOME_VISIT_NOT_DONE = "PNC Home Visit Not Done";
        String PNC_HOME_VISIT_NOT_DONE_UNDO = "PNC Home Visit Not Done Undo";
        String UPDATE_EVENT_CONDITION = "Update";
        String PREGNANCY_OUTCOME = "Pregnancy Outcome";
        String CHILD_REGISTRATION = "Child Registration";

    }

    interface FORMS {
        String ANC_REGISTRATION = "anc_registration";
        String PNC_CHILD_REGISTRATION = "pnc_child_enrollment";
        String IMMUNIZATIOIN_VISIT = "immunization_visit";
    }

    interface TABLES {
        String ANC_MEMBERS = "ec_anc_register";
        String FAMILY_MEMBER = "ec_family_member";
        String PREGNANCY_OUTCOME = "ec_pregnancy_outcome";
        String EC_CHILD = "ec_child";
    }

    interface CONFIGURATION {

        String ANC_REGISTER = "anc_register";
    }

    interface ACTIVITY_PAYLOAD {
        String BASE_ENTITY_ID = "BASE_ENTITY_ID";
        String ACTION = "ACTION";
        String TABLE_NAME = "TABLE";
    }

    interface ACTIVITY_PAYLOAD_TYPE {
        String REGISTRATION = "REGISTRATION";
    }

    interface ANC_MEMBER_OBJECTS {
        String EDIT_MODE = "editMode";
        String MEMBER_PROFILE_OBJECT = "MemberObject";
        String FAMILY_HEAD_NAME = "familyHeadName";
        String FAMILY_HEAD_PHONE = "familyHeadPhoneNumber";
    }

    interface RELATIONSHIP {
        String MOTHER = "mother";
        String FAMILY = "family";
    }

    interface MEMBER_PROFILE_TYPES {
        String ANC = "anc";
        String PNC = "pnc";
    }

    interface DATE_FORMATS {
        String NATIVE_FORMS = "dd-MM-yyyy";
    }

    interface HOME_VISIT {
        String VACCINE_NOT_GIVEN = "Vaccine not given";
    }

    interface HOME_VISIT_TASK {
        String VACCINE = "vaccine";
        String SERVICE = "service";
        String SUB_EVENT = "subevent";
    }
}
