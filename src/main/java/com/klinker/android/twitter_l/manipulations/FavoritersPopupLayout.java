package com.klinker.android.twitter_l.manipulations;

import android.content.Context;
import com.klinker.android.twitter_l.R;

public class FavoritersPopupLayout extends RetweetersPopupLayout {
    public FavoritersPopupLayout(Context context) {
        super(context);
    }

    public FavoritersPopupLayout(Context context, boolean windowed) {
        super(context, windowed);
    }

    @Override
    public void setUserWindowTitle() {
        setTitle(getContext().getString(R.string.favorites));
    }

}
