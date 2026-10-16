package me.rain.kernel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public final class PermissionItemView extends FrameLayout implements Checkable {
    private boolean checked;
    private final TextView title;
    private final ImageView checkIcon;

    public PermissionItemView(Context context) { this(context, null); }

    public PermissionItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.provision_permission_list_item_view, this, true);
        title = findViewById(R.id.item_title);
        checkIcon = findViewById(R.id.item_icon);
        setChecked(false);
        setClickable(true);
        setFocusable(true);
    }

    public void setItemTitle(int stringRes) { title.setText(stringRes); setChecked(checked); }

    @Override public void setChecked(boolean value) {
        checked = value;
        checkIcon.setVisibility(value ? VISIBLE : INVISIBLE);
        setContentDescription(title.getText() + (value ? "，已验证" : "，未验证"));
    }

    @Override public boolean isChecked() { return checked; }

    @Override public void toggle() { }
}
