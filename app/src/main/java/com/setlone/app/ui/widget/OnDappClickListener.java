package com.setlone.app.ui.widget;

import java.io.Serializable;

import com.setlone.app.entity.DApp;

public interface OnDappClickListener extends Serializable {
    void onDappClick(DApp dapp);
}
