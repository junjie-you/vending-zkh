package com.mc.vending.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.mc.vending.R;
import com.mc.vending.data.RetreatDetailData;
import com.mc.vending.data.RetreatHeadData;
import com.mc.vending.data.VendingChnProductWrapperData;

public class MC_ReturnForwardDetailAdapter extends BaseAdapter {

    private LayoutInflater                     inflater;
    private Map<String, ViewHodler>            viewMap;
    private List<VendingChnProductWrapperData> dataList;
    Activity                                   context;
    RetreatHeadData                            retreatHeadData;

    public MC_ReturnForwardDetailAdapter(Context context, List<VendingChnProductWrapperData> dataList,
            ListView lv, RetreatHeadData retreatHeadData) {
        super();
        if (dataList == null) {
            this.dataList = new ArrayList<VendingChnProductWrapperData>();
        } else {
            this.dataList = dataList;
        }
        viewMap = new HashMap<String, ViewHodler>();
        this.context = (Activity) context;
        inflater = this.context.getLayoutInflater();
        this.retreatHeadData = retreatHeadData;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // System.out.println("======" + position);
        ViewHodler viewHodler;
        if (convertView == null) {
            viewHodler = new ViewHodler();
            convertView = inflater.inflate(R.layout.urgent_replenishment_item, parent, false);
            viewHodler.channle_number = (TextView) convertView.findViewById(R.id.channle_number);
            viewHodler.sku_name = (TextView) convertView.findViewById(R.id.sku_name);
            viewHodler.replenishment_number = (EditText) convertView.findViewById(R.id.replenishment_number);
            viewHodler.btn_sub = (Button) convertView.findViewById(R.id.btn_sub);
            viewHodler.btn_sum = (Button) convertView.findViewById(R.id.btn_sum);
        } else {
            viewHodler = (ViewHodler) convertView.getTag();
        }
        VendingChnProductWrapperData data = (VendingChnProductWrapperData) getItem(position);
        viewHodler.channle_number.setText(data.getVendingChn().getVc1Code());
        viewHodler.sku_name.setText(data.getProductName());

        if (retreatHeadData.getRt1Status().equals("0")) {
            viewHodler.replenishment_number.setText(String.valueOf(data.getActQty()));
        } else {
            viewHodler.replenishment_number.setText(getActQty(data.getVendingChn().getVc1Code(),
                    retreatHeadData.getRetreatDetailDatas()));
        }
        viewHodler.btn_sub.setTag(position);
        viewHodler.btn_sum.setTag(100 + position);

        if (retreatHeadData.getRt1Status().equals("1")) {
            viewHodler.btn_sub.setVisibility(View.INVISIBLE);
            viewHodler.btn_sum.setVisibility(View.INVISIBLE);
        }

        convertView.setTag(viewHodler);
        viewMap.put(String.valueOf(position), viewHodler);
        return convertView;
    }

    private String getActQty(String chnCode, List<RetreatDetailData> datas) {
        String actQty = "0";
        for (RetreatDetailData retreatDetailData : datas) {
            if (retreatDetailData.getRt2Vc1Code().equals(chnCode)) {
                actQty = String.valueOf(retreatDetailData.getRt2ActualQty());
                break;
            }
        }
        return actQty;

    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return dataList.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void reloadViewHolder(int index) {
        ViewHodler viewHodler = viewMap.get(String.valueOf(index));
        VendingChnProductWrapperData data = (VendingChnProductWrapperData) getItem(index);
        viewHodler.replenishment_number.setText(String.valueOf(data.getActQty()));
    }

    class ViewHodler {
        TextView channle_number;
        TextView sku_name;
        EditText replenishment_number;
        Button   btn_sub;
        Button   btn_sum;
    }

}
