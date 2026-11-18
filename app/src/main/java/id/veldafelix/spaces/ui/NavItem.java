package id.veldafelix.spaces.ui;

import android.content.Context;
import android.view.*;
import android.widget.*;

/** One hit target for the entire item, including icon, label and padding. */
public final class NavItem extends LinearLayout {
    private final TextView icon,label;
    public NavItem(Context c,String symbol,String title,Runnable action) {
        super(c);setOrientation(VERTICAL);setGravity(Gravity.CENTER);setPadding(0,Glass.dp(c,6),0,Glass.dp(c,6));
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);setClickable(true);setFocusable(true);setHapticFeedbackEnabled(false);
        setContentDescription(title);
        icon=Glass.text(c,symbol,22,Glass.MUTED,false);label=Glass.text(c,title,10,Glass.MUTED,true);
        icon.setGravity(Gravity.CENTER);label.setGravity(Gravity.CENTER);
        addView(icon,new LayoutParams(-1,-2));Glass.gap(this,3);addView(label,new LayoutParams(-1,-2));
        setOnClickListener(v->action.run());active(false);
    }
    public void active(boolean active) {
        setSelected(active);icon.setTextColor(active?Glass.ACCENT:Glass.MUTED);label.setTextColor(active?Glass.INK:Glass.MUTED);
        setBackground(Glass.ripple(active?Glass.surface(0xE6FFFFFF,Glass.dp(getContext(),17)):null));
    }
    @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e) { return true; }
}
