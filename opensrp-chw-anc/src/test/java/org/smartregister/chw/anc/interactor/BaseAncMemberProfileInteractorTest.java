package org.smartregister.chw.anc.interactor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.smartregister.chw.anc.contract.BaseAncMemberProfileContract;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.util.AppExecutors;
import org.smartregister.domain.AlertStatus;

import java.util.Date;
import java.util.concurrent.Executor;

public class BaseAncMemberProfileInteractorTest implements Executor {

    private BaseAncMemberProfileInteractor interactor;

    @Mock
    private BaseAncMemberProfileContract.InteractorCallBack callBack;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        AppExecutors appExecutors = new AppExecutors(this, this, this);
        interactor = Mockito.spy(new BaseAncMemberProfileInteractor(appExecutors));
    }

    @Override
    public void execute(Runnable command) {
        command.run();
    }

    @Test
    public void testReloadMemberDetails(){
        interactor.reloadMemberDetails("12345", callBack);
        Mockito.verify(callBack).onMemberDetailsReloaded(Mockito.any(MemberObject.class));
    }

    @Test
    public void testGetMemberClient() {
        MemberObject memberObject = interactor.getMemberClient("12345");
        Assert.assertEquals("12345", memberObject.getBaseEntityId());
    }

    @Test
    public void testRefreshProfileInfo(){
        MemberObject memberObject = new MemberObject();
        interactor.refreshProfileInfo(memberObject, callBack);

        Mockito.verify(callBack).refreshFamilyStatus(AlertStatus.normal);
        Mockito.verify(callBack).refreshLastVisit(Mockito.any(Date.class));
        Mockito.verify(callBack).refreshUpComingServicesStatus(Mockito.anyString(), Mockito.any(AlertStatus.class), Mockito.any(Date.class));
    }
}