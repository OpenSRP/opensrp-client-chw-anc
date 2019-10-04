package org.smartregister.chw.anc_sample.activity;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.vijay.jsonwizard.activities.JsonWizardFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.anc.activity.BaseAncMemberProfileActivity;
import org.smartregister.chw.anc.activity.BaseAncRegisterActivity;
import org.smartregister.chw.anc_sample.R;
import org.smartregister.chw.anc_sample.fragment.AncRegisterFragment;
import org.smartregister.chw.anc_sample.utils.JsonFormUtils;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class AncRegisterActivity extends BaseAncRegisterActivity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sample_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.profile_page:
                openProfilePage();
                return true;
            case R.id.visit_page:
                openVisitPage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new AncRegisterFragment();
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {

        Intent intent = new Intent(this, JsonWizardFormActivity.class);
        intent.putExtra(JsonFormUtils.JSON_FORM_EXTRA.JSON, jsonForm.toString());

        Form form = new Form();
        form.setActionBarBackground(R.color.family_actionbar);
        form.setWizard(false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    private void openVisitPage() {
        // dumy common person object
        AncHomeVisitActivity.startMe(this, "12345", false);
    }

    private void openProfilePage() {
        //BaseAncMemberProfileActivity.startMe(this, EntryActivity.getSampleMember(), "Juma Family Head", "0976345634");
        BaseAncMemberProfileActivity.startMe(this, "123456");
    }
}
