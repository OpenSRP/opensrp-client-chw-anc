package org.smartregister.chw.anc.contract;

import android.content.Context;

import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.domain.AlertStatus;
import org.smartregister.view.contract.BaseProfileContract;

import java.util.Date;

public interface BaseAncMemberProfileContract {

    interface View {
        Context getContext();

        void setMemberName(String memberName);

        void setRecordVisitTitle(String tittle);

        void setMemberGA(String memberGA);

        void setMemberAddress(String memberAddress);

        void setMemberChwMemberId(String memberChwMemberId);

        BaseAncMemberProfileContract.Presenter presenter();

        void openMedicalHistory();

        void openUpcomingService();

        void openFamilyDueServices();

        void setProfileImage(String baseEntityId, String entityType);

        void setVisitNotDoneThisMonth();

        void updateVisitNotDone(long value);

        void showProgressBar(boolean status);

        void setLastVisit(Date lastVisitDate);

        void setUpComingServicesStatus(String service, AlertStatus status, Date date);

        void setFamilyStatus(AlertStatus status);

        void onMemberDetailsReloaded(MemberObject memberObject);
    }

    interface Presenter extends BaseProfileContract.Presenter {

        BaseAncMemberProfileContract.View getView();

        void fetchProfileData();

        void refreshProfileBottom();

        void reloadMemberDetails(String memberID);
    }

    interface Interactor {

        void reloadMemberDetails(String memberID, InteractorCallBack callBack);

        MemberObject getMemberClient(String memberID);

        void refreshProfileView(MemberObject memberObject, boolean isForEdit, InteractorCallBack callback);

        void updateVisitNotDone(long value, InteractorCallBack callback);

        void refreshProfileInfo(MemberObject memberObject, InteractorCallBack callback);

    }

    interface InteractorCallBack {

        void refreshProfileTopSection(MemberObject memberObject);

        void refreshLastVisit(Date lastVisitDate);

        void refreshUpComingServicesStatus(String service, AlertStatus status, Date date);

        void refreshFamilyStatus(AlertStatus status);

        void onMemberDetailsReloaded(MemberObject memberObject);
    }

}
